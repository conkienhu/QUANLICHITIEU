package com.expense.model;

import java.time.LocalDate;

/** Một giao dịch thu hoặc chi. */
public class Transaction {

    public enum Type {
        INCOME("Thu nhập"), EXPENSE("Chi tiêu");

        private final String label;
        Type(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private long id;
    private final LocalDate date;
    private final Type type;
    private final String category;
    private final double amount;
    private final String note;

    public Transaction(long id, LocalDate date, Type type, String category, double amount, String note) {
        this.id = id;
        this.date = date;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.note = note == null ? "" : note;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public LocalDate getDate() { return date; }
    public Type getType() { return type; }
    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public String getNote() { return note; }
}
