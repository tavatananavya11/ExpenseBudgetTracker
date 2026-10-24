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

    public List<TransactionEntry> findByMonth(String month) {
        return jdbcTemplate.query("""
                SELECT id, entry_type, category, amount_cents, description, entry_date
                FROM transactions
                WHERE substr(entry_date, 1, 7) = ?
                ORDER BY entry_date DESC, id DESC
                """, ENTRY_MAPPER, month);
    }

    public Optional<TransactionEntry> findById(long id) {
        return jdbcTemplate.query("""
                SELECT id, entry_type, category, amount_cents, description, entry_date
                FROM transactions
                WHERE id = ?
                """, ENTRY_MAPPER, id).stream().findFirst();
    }

    public long insert(EntryType type, Category category, long amountCents, String description, LocalDate date) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO transactions (entry_type, category, amount_cents, description, entry_date)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, type.name(), category.name(), amountCents, description, date.toString());
    }

    public boolean update(long id, EntryType type, Category category, long amountCents, String description, LocalDate date) {
        return jdbcTemplate.update("""
                UPDATE transactions
                SET entry_type = ?, category = ?, amount_cents = ?, description = ?, entry_date = ?
                WHERE id = ?
                """, type.name(), category.name(), amountCents, description, date.toString(), id) == 1;
    }

    public boolean delete(long id) {
        return jdbcTemplate.update("DELETE FROM transactions WHERE id = ?", id) == 1;
    }

    public Totals totalsForMonth(String month) {
        return jdbcTemplate.queryForObject("""
                SELECT
                    COALESCE(SUM(CASE WHEN entry_type = 'INCOME' THEN amount_cents ELSE 0 END), 0) AS income_cents,
                    COALESCE(SUM(CASE WHEN entry_type = 'EXPENSE' THEN amount_cents ELSE 0 END), 0) AS expense_cents
                FROM transactions
                WHERE substr(entry_date, 1, 7) = ?
                """, (resultSet, rowNumber) ->
                new Totals(resultSet.getLong("income_cents"), resultSet.getLong("expense_cents")), month);
    }

    public List<CategoryAmount> expensesByCategory(String month) {
        return jdbcTemplate.query("""
                SELECT category, SUM(amount_cents) AS amount_cents
                FROM transactions
                WHERE entry_type = 'EXPENSE' AND substr(entry_date, 1, 7) = ?
                GROUP BY category
                ORDER BY amount_cents DESC
                """, (resultSet, rowNumber) ->
                new CategoryAmount(Category.valueOf(resultSet.getString("category")),
                        resultSet.getLong("amount_cents")), month);
    }

    public List<MonthAmount> totalsBetween(String startMonth, String endMonth) {
        return jdbcTemplate.query("""
                SELECT substr(entry_date, 1, 7) AS month,
                    COALESCE(SUM(CASE WHEN entry_type = 'INCOME' THEN amount_cents ELSE 0 END), 0) AS income_cents,
                    COALESCE(SUM(CASE WHEN entry_type = 'EXPENSE' THEN amount_cents ELSE 0 END), 0) AS expense_cents
                FROM transactions
                WHERE substr(entry_date, 1, 7) BETWEEN ? AND ?
                GROUP BY substr(entry_date, 1, 7)
                ORDER BY month
                """, (resultSet, rowNumber) ->
                new MonthAmount(resultSet.getString("month"),
                        resultSet.getLong("income_cents"), resultSet.getLong("expense_cents")),
                startMonth, endMonth);
    }

    public record Totals(long incomeCents, long expenseCents) {
    }

    public record CategoryAmount(Category category, long amountCents) {
    }

    public record MonthAmount(String month, long incomeCents, long expenseCents) {
    }
}
