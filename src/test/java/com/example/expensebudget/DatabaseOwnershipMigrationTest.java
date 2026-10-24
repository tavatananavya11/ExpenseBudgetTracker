package com.example.expensebudget;

import com.example.expensebudget.service.DatabaseOwnershipMigration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseOwnershipMigrationTest {

    @Test
    void keepsLegacyRecordsAndBudgetsUnassigned(@TempDir Path tempDirectory) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:sqlite:" + tempDirectory.resolve("legacy.db"), null, null);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    email TEXT NOT NULL COLLATE NOCASE UNIQUE,
                    password_hash TEXT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    entry_type TEXT NOT NULL,
                    category TEXT NOT NULL,
                    amount_cents INTEGER NOT NULL,
                    description TEXT NOT NULL,
                    entry_date TEXT NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE budgets (
                    budget_month TEXT PRIMARY KEY,
                    amount_cents INTEGER NOT NULL
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO transactions (entry_type, category, amount_cents, description, entry_date)
                VALUES ('EXPENSE', 'FOOD', 125000, 'Legacy record', '2026-10-01')
                """);
        jdbcTemplate.update("INSERT INTO budgets (budget_month, amount_cents) VALUES ('2026-10', 500000)");

        new DatabaseOwnershipMigration(jdbcTemplate).run(new DefaultApplicationArguments(new String[0]));
        new DatabaseOwnershipMigration(jdbcTemplate).run(new DefaultApplicationArguments(new String[0]));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE user_id IS NULL", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT amount_cents FROM budgets WHERE budget_month = '2026-10' AND user_id IS NULL",
                Integer.class)).isEqualTo(500000);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'budgets_legacy'",
                Integer.class)).isZero();

        long userId = jdbcTemplate.queryForObject("""
                INSERT INTO users (email, password_hash) VALUES ('account@example.test', 'not-a-real-password')
                RETURNING id
                """, Long.class);
        jdbcTemplate.update("""
                INSERT INTO budgets (budget_month, amount_cents, user_id)
                VALUES ('2026-10', 800000, ?)
                """, userId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT amount_cents FROM budgets WHERE budget_month = '2026-10' AND user_id = ?",
                Integer.class, userId)).isEqualTo(800000);
    }
}
