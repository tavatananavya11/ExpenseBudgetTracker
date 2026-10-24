package com.example.expensebudget.model;

public record UserAccount(long id, String email, String passwordHash) {
    @Override
    public String toString() {
        return "UserAccount[id=" + id + ", email=" + email + ", passwordHash=[REDACTED]]";
    }
}
