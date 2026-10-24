package com.example.expensebudget.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class BudgetRepository {
    private final JdbcTemplate jdbcTemplate;

    public BudgetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Long> findAmountCents(String month, long userId) {
        return jdbcTemplate.query("SELECT amount_cents FROM budgets WHERE budget_month = ? AND user_id = ?",
                (resultSet, rowNumber) -> resultSet.getLong("amount_cents"), month, userId).stream().findFirst();
    }

    public void save(String month, long amountCents, long userId) {
        jdbcTemplate.update("""
                INSERT INTO budgets (budget_month, amount_cents, user_id)
                VALUES (?, ?, ?)
                ON CONFLICT(user_id, budget_month) DO UPDATE SET amount_cents = excluded.amount_cents
                """, month, amountCents, userId);
    }
}
