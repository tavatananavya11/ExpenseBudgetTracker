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

    public Optional<Long> findAmountCents(String month) {
        return jdbcTemplate.query("SELECT amount_cents FROM budgets WHERE budget_month = ?",
                (resultSet, rowNumber) -> resultSet.getLong("amount_cents"), month).stream().findFirst();
    }

    public void save(String month, long amountCents) {
        jdbcTemplate.update("""
                INSERT INTO budgets (budget_month, amount_cents)
                VALUES (?, ?)
                ON CONFLICT(budget_month) DO UPDATE SET amount_cents = excluded.amount_cents
                """, month, amountCents);
    }
}
