package com.example.expensebudget.repository;

import com.example.expensebudget.model.Category;
import com.example.expensebudget.model.EntryType;
import com.example.expensebudget.model.TransactionEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class TransactionRepository {
    private static final RowMapper<TransactionEntry> ENTRY_MAPPER = (resultSet, rowNumber) ->
            new TransactionEntry(
                    resultSet.getLong("id"),
                    EntryType.valueOf(resultSet.getString("entry_type")),
                    Category.valueOf(resultSet.getString("category")),
                    Money.fromCents(resultSet.getLong("amount_cents")),
                    resultSet.getString("description"),
                    LocalDate.parse(resultSet.getString("entry_date"))
            );

    private final JdbcTemplate jdbcTemplate;

    public TransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TransactionEntry> findByMonth(String month, long userId) {
        return jdbcTemplate.query("""
                SELECT id, entry_type, category, amount_cents, description, entry_date
                FROM transactions
                WHERE substr(entry_date, 1, 7) = ? AND user_id = ?
                ORDER BY entry_date DESC, id DESC
                """, ENTRY_MAPPER, month, userId);
    }

    public Optional<TransactionEntry> findById(long id, long userId) {
        return jdbcTemplate.query("""
                SELECT id, entry_type, category, amount_cents, description, entry_date
                FROM transactions
                WHERE id = ? AND user_id = ?
                """, ENTRY_MAPPER, id, userId).stream().findFirst();
    }

    public long insert(EntryType type, Category category, long amountCents, String description, LocalDate date,
                       long userId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO transactions (entry_type, category, amount_cents, description, entry_date, user_id)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, type.name(), category.name(), amountCents, description, date.toString(), userId);
    }

    public boolean update(long id, EntryType type, Category category, long amountCents, String description,
                          LocalDate date, long userId) {
        return jdbcTemplate.update("""
                UPDATE transactions
                SET entry_type = ?, category = ?, amount_cents = ?, description = ?, entry_date = ?
                WHERE id = ? AND user_id = ?
                """, type.name(), category.name(), amountCents, description, date.toString(), id, userId) == 1;
    }

    public boolean delete(long id, long userId) {
        return jdbcTemplate.update("DELETE FROM transactions WHERE id = ? AND user_id = ?", id, userId) == 1;
    }

    public Totals totalsForMonth(String month, long userId) {
        return jdbcTemplate.queryForObject("""
                SELECT
                    COALESCE(SUM(CASE WHEN entry_type = 'INCOME' THEN amount_cents ELSE 0 END), 0) AS income_cents,
                    COALESCE(SUM(CASE WHEN entry_type = 'EXPENSE' THEN amount_cents ELSE 0 END), 0) AS expense_cents
                FROM transactions
                WHERE substr(entry_date, 1, 7) = ? AND user_id = ?
                """, (resultSet, rowNumber) ->
                new Totals(resultSet.getLong("income_cents"), resultSet.getLong("expense_cents")), month, userId);
    }

    public List<CategoryAmount> expensesByCategory(String month, long userId) {
        return jdbcTemplate.query("""
                SELECT category, SUM(amount_cents) AS amount_cents
                FROM transactions
                WHERE entry_type = 'EXPENSE' AND substr(entry_date, 1, 7) = ? AND user_id = ?
                GROUP BY category
                ORDER BY amount_cents DESC
                """, (resultSet, rowNumber) ->
                new CategoryAmount(Category.valueOf(resultSet.getString("category")),
                        resultSet.getLong("amount_cents")), month, userId);
    }

    public List<MonthAmount> totalsBetween(String startMonth, String endMonth, long userId) {
        return jdbcTemplate.query("""
                SELECT substr(entry_date, 1, 7) AS month,
                    COALESCE(SUM(CASE WHEN entry_type = 'INCOME' THEN amount_cents ELSE 0 END), 0) AS income_cents,
                    COALESCE(SUM(CASE WHEN entry_type = 'EXPENSE' THEN amount_cents ELSE 0 END), 0) AS expense_cents
                FROM transactions
                WHERE substr(entry_date, 1, 7) BETWEEN ? AND ? AND user_id = ?
                GROUP BY substr(entry_date, 1, 7)
                ORDER BY month
                """, (resultSet, rowNumber) ->
                new MonthAmount(resultSet.getString("month"),
                        resultSet.getLong("income_cents"), resultSet.getLong("expense_cents")),
                startMonth, endMonth, userId);
    }

    public record Totals(long incomeCents, long expenseCents) {
    }

    public record CategoryAmount(Category category, long amountCents) {
    }

    public record MonthAmount(String month, long incomeCents, long expenseCents) {
    }
}
