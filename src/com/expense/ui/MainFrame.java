package com.expense.ui;

import com.expense.model.Transaction;
import com.expense.service.ExpenseService;
import com.expense.util.CsvUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Cửa sổ chính của ứng dụng quản lý chi tiêu. */
public class MainFrame extends JFrame {
    private static final Color GREEN = new Color(0x2E7D32);
    private static final Color RED = new Color(0xC62828);

    private final ExpenseService service = new ExpenseService();
    private final NumberFormat nf = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JComboBox<String> monthBox = new JComboBox<>();
    private final JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2000, 2100, 1));
    private final JComboBox<Object> typeBox = new JComboBox<>(new Object[]{"Tất cả", Transaction.Type.INCOME, Transaction.Type.EXPENSE});
    private final JTextField searchField = new JTextField(14);

    private final TxTableModel tableModel = new TxTableModel();
    private final JTable table = new JTable(tableModel);
    private final JLabel incomeLbl = new JLabel("0", SwingConstants.CENTER);
    private final JLabel expenseLbl = new JLabel("0", SwingConstants.CENTER);
    private final JLabel balanceLbl = new JLabel("0", SwingConstants.CENTER);
    private final JLabel statusLbl = new JLabel(" ");
    private final PieChartPanel chart = new PieChartPanel();
    private final DefaultTableModel budgetModel = new DefaultTableModel(
            new Object[]{"Danh mục", "Đã chi", "Hạn mức", "Sử dụng"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private boolean updating;

    public MainFrame() {
        super("Quản lý chi tiêu hàng tháng");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 650);
        setLocationRelativeTo(null);

        for (int m = 1; m <= 12; m++) monthBox.addItem("Tháng " + m);
        monthBox.setSelectedIndex(LocalDate.now().getMonthValue() - 1);

        setJMenuBar(buildMenu());
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);

        monthBox.addActionListener(e -> { if (!updating) refresh(); });
        yearSpinner.addChangeListener(e -> { if (!updating) refresh(); });
        typeBox.addActionListener(e -> refresh());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });
        refresh();
    }

    // ---------- Dựng giao diện ----------

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("Tệp");
        JMenuItem export = new JMenuItem("Xuất CSV tháng này...");
        export.addActionListener(e -> exportCsv());
        JMenuItem exit = new JMenuItem("Thoát");
        exit.addActionListener(e -> dispose());
        file.add(export);
        file.addSeparator();
        file.add(exit);
        JMenu help = new JMenu("Trợ giúp");
        JMenuItem about = new JMenuItem("Giới thiệu");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Quản lý chi tiêu hàng tháng\nJava Swing\nDữ liệu lưu tại: ~/.expense-manager",
                "Giới thiệu", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);
        bar.add(file);
        bar.add(help);
        return bar;
    }

    private JPanel buildToolbar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton prev = new JButton("◀");
        JButton next = new JButton("▶");
        JButton today = new JButton("Hôm nay");
        prev.addActionListener(e -> shiftMonth(-1));
        next.addActionListener(e -> shiftMonth(1));
        today.addActionListener(e -> setMonth(YearMonth.now()));
        yearSpinner.setEditor(new JSpinner.NumberEditor(yearSpinner, "#"));
        p.add(prev);
        p.add(monthBox);
        p.add(yearSpinner);
        p.add(next);
        p.add(today);
        p.add(Box.createHorizontalStrut(16));
        p.add(new JLabel("Loại:"));
        p.add(typeBox);
        p.add(new JLabel("Tìm kiếm:"));
        p.add(searchField);
        return p;
    }

    private JComponent buildCenter() {
        table.setRowHeight(26);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TxRenderer r = new TxRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(r);
        table.getColumnModel().getColumn(4).setPreferredWidth(220);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editSelected();
            }
        });
        JScrollPane left = new JScrollPane(table);
        left.setBorder(BorderFactory.createTitledBorder("Giao dịch trong tháng"));

        // Thẻ tổng quan
        JPanel summary = new JPanel(new GridLayout(1, 3, 6, 0));
        summary.add(card("Thu nhập", incomeLbl, GREEN));
        summary.add(card("Chi tiêu", expenseLbl, RED));
        summary.add(card("Còn lại", balanceLbl, Color.DARK_GRAY));

        // Bảng ngân sách
        JTable budgetTable = new JTable(budgetModel);
        budgetTable.setRowHeight(24);
        budgetTable.setEnabled(false);
        budgetTable.getColumnModel().getColumn(3).setCellRenderer(new ProgressRenderer());
        JScrollPane budgetScroll = new JScrollPane(budgetTable);
        budgetScroll.setBorder(BorderFactory.createTitledBorder("Ngân sách (đ)"));
        budgetScroll.setPreferredSize(new Dimension(340, 170));

        JPanel right = new JPanel(new BorderLayout(0, 6));
        right.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        right.add(summary, BorderLayout.NORTH);
        right.add(chart, BorderLayout.CENTER);
        right.add(budgetScroll, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.62);
        split.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
        return split;
    }

    private JPanel card(String title, JLabel value, Color color) {
        value.setFont(value.getFont().deriveFont(Font.BOLD, 15f));
        value.setForeground(color);
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(value, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildBottom() {
        JButton add = new JButton("Thêm");
        JButton edit = new JButton("Sửa");
        JButton del = new JButton("Xóa");
        JButton budget = new JButton("Ngân sách...");
        JButton export = new JButton("Xuất CSV");
        add.addActionListener(e -> addTransaction());
        edit.addActionListener(e -> editSelected());
        del.addActionListener(e -> deleteSelected());
        budget.addActionListener(e -> {
            BudgetDialog d = new BudgetDialog(this, service);
            d.setVisible(true);
            if (d.isSaved()) refresh();
        });
        export.addActionListener(e -> exportCsv());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        btns.add(add);
        btns.add(edit);
        btns.add(del);
        btns.add(Box.createHorizontalStrut(20));
        btns.add(budget);
        btns.add(export);

        JPanel p = new JPanel(new BorderLayout());
        p.add(btns, BorderLayout.WEST);
        statusLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        p.add(statusLbl, BorderLayout.EAST);
        return p;
    }

    // ---------- Hành động ----------

    private YearMonth current() {
        return YearMonth.of((Integer) yearSpinner.getValue(), monthBox.getSelectedIndex() + 1);
    }

    private void setMonth(YearMonth ym) {
        updating = true;
        monthBox.setSelectedIndex(ym.getMonthValue() - 1);
        yearSpinner.setValue(ym.getYear());
        updating = false;
        refresh();
    }

    private void shiftMonth(int delta) { setMonth(current().plusMonths(delta)); }

    private void addTransaction() {
        YearMonth ym = current();
        LocalDate def = YearMonth.now().equals(ym) ? LocalDate.now() : ym.atDay(1);
        TransactionDialog d = new TransactionDialog(this, service, null, def);
        d.setVisible(true);
        Transaction t = d.getResult();
        if (t != null) {
            service.add(t);
            setMonth(YearMonth.from(t.getDate()));
            checkBudget(t);
        }
    }

    private Transaction selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một giao dịch trong bảng.", "Chưa chọn", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return tableModel.rows.get(table.convertRowIndexToModel(row));
    }

    private void editSelected() {
        Transaction old = selected();
        if (old == null) return;
        TransactionDialog d = new TransactionDialog(this, service, old, old.getDate());
        d.setVisible(true);
        Transaction t = d.getResult();
        if (t != null) {
            service.update(t);
            setMonth(YearMonth.from(t.getDate()));
            checkBudget(t);
        }
    }

    private void deleteSelected() {
        Transaction t = selected();
        if (t == null) return;
        int ok = JOptionPane.showConfirmDialog(this,
                "Xóa giao dịch \"" + t.getCategory() + " - " + nf.format(t.getAmount()) + " đ\"?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            service.delete(t.getId());
            refresh();
        }
    }

    private void checkBudget(Transaction t) {
        if (t.getType() != Transaction.Type.EXPENSE) return;
        Double limit = service.getBudgets().get(t.getCategory());
        if (limit == null) return;
        double spent = service.spent(YearMonth.from(t.getDate()), t.getCategory());
        if (spent > limit) {
            JOptionPane.showMessageDialog(this, String.format(
                    "Danh mục \"%s\" đã vượt ngân sách!\nĐã chi: %s đ / Hạn mức: %s đ",
                    t.getCategory(), nf.format(spent), nf.format(limit)), "Vượt ngân sách", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        YearMonth ym = current();
        fc.setSelectedFile(new File(String.format("chitieu_%d-%02d.csv", ym.getYear(), ym.getMonthValue())));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        List<String> lines = new ArrayList<>();
        lines.add("\uFEFF" + CsvUtil.join("Ngày", "Loại", "Danh mục", "Số tiền", "Ghi chú"));
        for (Transaction t : service.query(ym, null, "")) {
            lines.add(CsvUtil.join(df.format(t.getDate()), t.getType(), t.getCategory(), (long) t.getAmount(), t.getNote()));
        }
        try {
            Files.write(fc.getSelectedFile().toPath(), lines, StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, "Đã xuất " + (lines.size() - 1) + " giao dịch.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không thể ghi file: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------- Cập nhật dữ liệu hiển thị ----------

    private void refresh() {
        YearMonth ym = current();
        Transaction.Type type = typeBox.getSelectedItem() instanceof Transaction.Type ? (Transaction.Type) typeBox.getSelectedItem() : null;
        tableModel.setRows(service.query(ym, type, searchField.getText()));

        List<Transaction> month = service.query(ym, null, "");
        double inc = ExpenseService.sum(month, Transaction.Type.INCOME);
        double exp = ExpenseService.sum(month, Transaction.Type.EXPENSE);
        incomeLbl.setText(nf.format(inc) + " đ");
        expenseLbl.setText(nf.format(exp) + " đ");
        balanceLbl.setText(nf.format(inc - exp) + " đ");
        balanceLbl.setForeground(inc - exp >= 0 ? GREEN : RED);

        chart.setData(service.expenseByCategory(ym));

        budgetModel.setRowCount(0);
        for (Map.Entry<String, Double> e : service.getBudgets().entrySet()) {
            double spent = service.spent(ym, e.getKey());
            int pct = (int) Math.round(spent * 100 / e.getValue());
            budgetModel.addRow(new Object[]{e.getKey(), nf.format(spent), nf.format(e.getValue()), pct});
        }
        statusLbl.setText(tableModel.rows.size() + " giao dịch hiển thị");
    }

    // ---------- Model & renderer ----------

    private class TxTableModel extends AbstractTableModel {
        private final String[] cols = {"Ngày", "Loại", "Danh mục", "Số tiền (đ)", "Ghi chú"};
        List<Transaction> rows = new ArrayList<>();

        void setRows(List<Transaction> r) { rows = r; fireTableDataChanged(); }
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Class<?> getColumnClass(int c) {
            return c == 0 ? LocalDate.class : c == 3 ? Double.class : String.class;
        }
        @Override public Object getValueAt(int r, int c) {
            Transaction t = rows.get(r);
            switch (c) {
                case 0: return t.getDate();
                case 1: return t.getType().toString();
                case 2: return t.getCategory();
                case 3: return t.getAmount();
                default: return t.getNote();
            }
        }
    }

    private class TxRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object v, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(tbl, v, sel, focus, row, col);
            Transaction t = tableModel.rows.get(tbl.convertRowIndexToModel(row));
            setHorizontalAlignment(col == 3 ? RIGHT : LEFT);
            if (col == 0) setText(df.format((LocalDate) v));
            if (col == 3) {
                boolean income = t.getType() == Transaction.Type.INCOME;
                setText((income ? "+" : "-") + nf.format(t.getAmount()));
                if (!sel) setForeground(income ? GREEN : RED);
            } else if (!sel) setForeground(tbl.getForeground());
            return this;
        }
    }

    private static class ProgressRenderer implements TableCellRenderer {
        private final JProgressBar bar = new JProgressBar(0, 100);
        ProgressRenderer() { bar.setStringPainted(true); }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            int pct = (Integer) v;
            bar.setValue(Math.min(100, pct));
            bar.setString(pct + "%");
            bar.setForeground(pct > 100 ? RED : pct >= 80 ? new Color(0xEF6C00) : GREEN);
            return bar;
        }
    }
}
