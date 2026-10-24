package com.example.expensebudget.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Enter your email address.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 254, message = "Email address must be 254 characters or fewer.")
        String email,
        @NotBlank(message = "Enter your password.")
        @Size(max = 72, message = "Password is too long.")
        String password
) {
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=[REDACTED]]";
    }
}
