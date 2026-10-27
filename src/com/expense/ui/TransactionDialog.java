package com.expense.ui;

import com.expense.model.Transaction;
import com.expense.service.ExpenseService;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** Hộp thoại thêm / sửa giao dịch. */
public class TransactionDialog extends JDialog {
    private final ExpenseService service;
    private final JComboBox<Transaction.Type> typeBox = new JComboBox<>(Transaction.Type.values());
    private final JComboBox<String> categoryBox = new JComboBox<>();
    private final JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
    private final JTextField amountField = new JTextField(15);
    private final JTextField noteField = new JTextField(15);
    private Transaction result;
    private final long editId;

    public TransactionDialog(Frame owner, ExpenseService service, Transaction existing, LocalDate defaultDate) {
        super(owner, existing == null ? "Thêm giao dịch" : "Sửa giao dịch", true);
        this.service = service;
        this.editId = existing == null ? 0 : existing.getId();

        categoryBox.setEditable(true);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy"));
        typeBox.addActionListener(e -> loadCategories());

        if (existing != null) {
            typeBox.setSelectedItem(existing.getType());
            loadCategories();
            categoryBox.setSelectedItem(existing.getCategory());
            dateSpinner.setValue(toDate(existing.getDate()));
            amountField.setText(String.valueOf((long) existing.getAmount()));
            noteField.setText(existing.getNote());
        } else {
            typeBox.setSelectedItem(Transaction.Type.EXPENSE);
            loadCategories();
            dateSpinner.setValue(toDate(defaultDate));
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        String[] labels = {"Loại:", "Danh mục:", "Ngày:", "Số tiền (đ):", "Ghi chú:"};
        JComponent[] fields = {typeBox, categoryBox, dateSpinner, amountField, noteField};
        for (int i = 0; i < labels.length; i++) {
            c.gridx = 0; c.gridy = i; c.weightx = 0;
            form.add(new JLabel(labels[i]), c);
            c.gridx = 1; c.weightx = 1;
            form.add(fields[i], c);
        }

        JButton ok = new JButton("Lưu");
        JButton cancel = new JButton("Hủy");
        ok.addActionListener(e -> onOk());
        cancel.addActionListener(e -> dispose());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(ok);
        btns.add(cancel);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(btns, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);
        pack();
        setLocationRelativeTo(owner);
    }

    private void loadCategories() {
        categoryBox.removeAllItems();
        for (String s : service.categories((Transaction.Type) typeBox.getSelectedItem())) categoryBox.addItem(s);
    }

    private void onOk() {
        String category = String.valueOf(categoryBox.getSelectedItem()).trim();
        String digits = amountField.getText().replaceAll("[^0-9]", "");
        if (category.isEmpty() || category.equals("null")) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập danh mục.", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (digits.isEmpty() || Double.parseDouble(digits) <= 0) {
            JOptionPane.showMessageDialog(this, "Số tiền phải lớn hơn 0.", "Số tiền không hợp lệ", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LocalDate date = ((Date) dateSpinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        result = new Transaction(editId, date, (Transaction.Type) typeBox.getSelectedItem(), category,
                Double.parseDouble(digits), noteField.getText().trim());
        dispose();
    }

    private static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /** @return giao dịch đã nhập, hoặc null nếu người dùng hủy */
    public Transaction getResult() { return result; }
}
