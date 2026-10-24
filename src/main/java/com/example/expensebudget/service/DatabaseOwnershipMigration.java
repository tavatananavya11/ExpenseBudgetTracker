package com.example.expensebudget.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

@Component
public class DatabaseOwnershipMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseOwnershipMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                addTransactionOwnerColumnIfMissing(connection);
                migrateBudgetsIfNeeded(connection);
                createOwnerIndexes(connection);
                connection.commit();
                return null;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        });
    }

    private void addTransactionOwnerColumnIfMissing(Connection connection) throws SQLException {
        if (!columns(connection, "transactions").contains("user_id")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        ALTER TABLE transactions
                        ADD COLUMN user_id INTEGER REFERENCES users(id) ON DELETE CASCADE
                        """);
            }
        }
    }

    private void migrateBudgetsIfNeeded(Connection connection) throws SQLException {
        if (columns(connection, "budgets").contains("user_id")) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE budgets RENAME TO budgets_legacy");
            statement.execute("""
                    CREATE TABLE budgets (
                        budget_month TEXT NOT NULL,
                        amount_cents INTEGER NOT NULL CHECK (amount_cents >= 0),
                        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
                        PRIMARY KEY (user_id, budget_month)
                    )
                    """);
            statement.execute("""
                    INSERT INTO budgets (budget_month, amount_cents, user_id)
                    SELECT budget_month, amount_cents, NULL FROM budgets_legacy
                    """);
            statement.execute("DROP TABLE budgets_legacy");
        }
    }

    private void createOwnerIndexes(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_transactions_user_date
                    ON transactions(user_id, entry_date)
                    """);
            statement.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_budgets_user_month
                    ON budgets(user_id, budget_month)
                    WHERE user_id IS NOT NULL
                    """);
        }
    }

    private Set<String> columns(Connection connection, String table) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (resultSet.next()) {
                columns.add(resultSet.getString("name"));
            }
        }
        return columns;
    }
}
