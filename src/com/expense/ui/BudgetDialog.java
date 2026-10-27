package com.expense.ui;

import com.expense.model.Transaction;
import com.expense.service.ExpenseService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Đặt hạn mức chi tiêu hàng tháng cho từng danh mục (để trống / 0 = không giới hạn). */
public class BudgetDialog extends JDialog {
    private final ExpenseService service;
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Danh mục", "Hạn mức / tháng (đ)"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return c == 1; }
    };
    private final JTable table = new JTable(model);
    private boolean saved;

    public BudgetDialog(Frame owner, ExpenseService service) {
        super(owner, "Ngân sách hàng tháng", true);
        this.service = service;
        Map<String, Double> budgets = service.getBudgets();
        for (String cat : service.categories(Transaction.Type.EXPENSE)) {
            Double v = budgets.get(cat);
            model.addRow(new Object[]{cat, v == null ? "" : String.valueOf(v.longValue())});
        }
        table.setRowHeight(24);

        JButton add = new JButton("Thêm danh mục");
        JButton save = new JButton("Lưu");
        JButton cancel = new JButton("Hủy");
        add.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Tên danh mục mới:");
            if (name != null && !name.trim().isEmpty()) model.addRow(new Object[]{name.trim(), ""});
        });
        save.addActionListener(e -> onSave());
        cancel.addActionListener(e -> dispose());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(add);
        btns.add(save);
        btns.add(cancel);

        setLayout(new BorderLayout(0, 6));
        add(new JLabel("  Nhấp đúp vào ô hạn mức để chỉnh sửa. Cảnh báo sẽ hiện khi chi vượt hạn mức."), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(btns, BorderLayout.SOUTH);
        setSize(420, 400);
        setLocationRelativeTo(owner);
    }

    private void onSave() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        Map<String, Double> map = new LinkedHashMap<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            String digits = String.valueOf(model.getValueAt(i, 1)).replaceAll("[^0-9]", "");
            if (!digits.isEmpty() && Double.parseDouble(digits) > 0) {
                map.put(String.valueOf(model.getValueAt(i, 0)), Double.parseDouble(digits));
            }
        }
        service.setBudgets(map);
        saved = true;
        dispose();
    }

    public boolean isSaved() { return saved; }
}
