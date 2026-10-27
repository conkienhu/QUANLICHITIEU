package com.expense.service;

import com.expense.data.DataStore;
import com.expense.model.Transaction;
import com.expense.model.Transaction.Type;

import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/** Logic nghiệp vụ: CRUD giao dịch, thống kê, ngân sách. */
public class ExpenseService {
    public static final List<String> DEFAULT_EXPENSE = Arrays.asList(
            "Ăn uống", "Di chuyển", "Nhà ở", "Hóa đơn", "Mua sắm", "Giải trí", "Sức khỏe", "Giáo dục", "Khác");
    public static final List<String> DEFAULT_INCOME = Arrays.asList("Lương", "Thưởng", "Đầu tư", "Khác");

    private final DataStore store = new DataStore();
    private final List<Transaction> items;
    private final Map<String, Double> budgets;

    public ExpenseService() {
        items = store.loadTransactions();
        budgets = store.loadBudgets();
    }

    public void add(Transaction t) {
        long next = items.stream().mapToLong(Transaction::getId).max().orElse(0) + 1;
        t.setId(next);
        items.add(t);
        save();
    }

    public void update(Transaction t) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == t.getId()) { items.set(i, t); break; }
        }
        save();
    }

    public void delete(long id) {
        items.removeIf(t -> t.getId() == id);
        save();
    }

    /** Lọc theo tháng, loại (null = tất cả) và từ khóa (danh mục/ghi chú). */
    public List<Transaction> query(YearMonth ym, Type type, String keyword) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        return items.stream()
                .filter(t -> YearMonth.from(t.getDate()).equals(ym))
                .filter(t -> type == null || t.getType() == type)
                .filter(t -> kw.isEmpty() || t.getCategory().toLowerCase().contains(kw)
                        || t.getNote().toLowerCase().contains(kw))
                .sorted(Comparator.comparing(Transaction::getDate).reversed()
                        .thenComparing(Comparator.comparingLong(Transaction::getId).reversed()))
                .collect(Collectors.toList());
    }

    public static double sum(List<Transaction> list, Type type) {
        return list.stream().filter(t -> t.getType() == type).mapToDouble(Transaction::getAmount).sum();
    }

    public Map<String, Double> expenseByCategory(YearMonth ym) {
        Map<String, Double> m = new LinkedHashMap<>();
        query(ym, Type.EXPENSE, "").forEach(t -> m.merge(t.getCategory(), t.getAmount(), Double::sum));
        return m.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    public double spent(YearMonth ym, String category) {
        return query(ym, Type.EXPENSE, "").stream()
                .filter(t -> t.getCategory().equals(category))
                .mapToDouble(Transaction::getAmount).sum();
    }

    /** Danh mục mặc định + các danh mục người dùng đã dùng. */
    public Set<String> categories(Type type) {
        Set<String> set = new LinkedHashSet<>(type == Type.EXPENSE ? DEFAULT_EXPENSE : DEFAULT_INCOME);
        items.stream().filter(t -> t.getType() == type).forEach(t -> set.add(t.getCategory()));
        if (type == Type.EXPENSE) set.addAll(budgets.keySet());
        return set;
    }

    public Map<String, Double> getBudgets() { return Collections.unmodifiableMap(budgets); }

    public void setBudgets(Map<String, Double> newBudgets) {
        budgets.clear();
        budgets.putAll(new TreeMap<>(newBudgets));
        save();
    }

    private void save() { store.save(items, budgets); }
}
