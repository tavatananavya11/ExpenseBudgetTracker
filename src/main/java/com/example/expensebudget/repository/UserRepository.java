package com.example.expensebudget.repository;

import com.example.expensebudget.model.UserAccount;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserAccount> findByEmail(String email) {
        return jdbcTemplate.query("""
                SELECT id, email, password_hash
                FROM users
                WHERE email = ? COLLATE NOCASE
                """, (resultSet, rowNumber) ->
                new UserAccount(resultSet.getLong("id"), resultSet.getString("email"),
                        resultSet.getString("password_hash")), email).stream().findFirst();
    }

    public boolean insert(String email, String passwordHash) {
        return jdbcTemplate.update("""
                INSERT INTO users (email, password_hash)
                VALUES (?, ?)
                ON CONFLICT(email) DO NOTHING
                """, email, passwordHash) == 1;
    }
}
