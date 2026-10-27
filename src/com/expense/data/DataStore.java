package com.expense.data;

import com.expense.model.Transaction;
import com.expense.util.CsvUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Lưu dữ liệu vào thư mục ~/.expense-manager dưới dạng CSV. */
public class DataStore {
    private final Path dir = Paths.get(System.getProperty("user.home"), ".expense-manager");
    private final Path txFile = dir.resolve("transactions.csv");
    private final Path budgetFile = dir.resolve("budgets.csv");

    public List<Transaction> loadTransactions() {
        List<Transaction> result = new ArrayList<>();
        if (!Files.exists(txFile)) return result;
        try {
            for (String line : Files.readAllLines(txFile, StandardCharsets.UTF_8)) {
                if (line.trim().isEmpty()) continue;
                List<String> f = CsvUtil.parse(line);
                if (f.size() < 6) continue;
                try {
                    result.add(new Transaction(Long.parseLong(f.get(0)), LocalDate.parse(f.get(1)),
                            Transaction.Type.valueOf(f.get(2)), f.get(3),
                            Double.parseDouble(f.get(4)), f.get(5)));
                } catch (RuntimeException ignored) { /* bỏ qua dòng lỗi */ }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return result;
    }

    public Map<String, Double> loadBudgets() {
        Map<String, Double> result = new TreeMap<>();
        if (!Files.exists(budgetFile)) return result;
        try {
            for (String line : Files.readAllLines(budgetFile, StandardCharsets.UTF_8)) {
                List<String> f = CsvUtil.parse(line);
                if (f.size() < 2) continue;
                try { result.put(f.get(0), Double.parseDouble(f.get(1))); }
                catch (NumberFormatException ignored) { }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return result;
    }

    public void save(List<Transaction> items, Map<String, Double> budgets) {
        try {
            Files.createDirectories(dir);
            List<String> tx = new ArrayList<>();
            for (Transaction t : items) {
                tx.add(CsvUtil.join(t.getId(), t.getDate(), t.getType().name(),
                        t.getCategory(), t.getAmount(), t.getNote()));
            }
            Files.write(txFile, tx, StandardCharsets.UTF_8);
            List<String> bd = new ArrayList<>();
            for (Map.Entry<String, Double> e : budgets.entrySet()) {
                bd.add(CsvUtil.join(e.getKey(), e.getValue()));
            }
            Files.write(budgetFile, bd, StandardCharsets.UTF_8);
        } catch (IOException e) { e.printStackTrace(); }
    }
}
