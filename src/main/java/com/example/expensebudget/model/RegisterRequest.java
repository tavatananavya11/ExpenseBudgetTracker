package com.example.expensebudget.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Enter your email address.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 254, message = "Email address must be 254 characters or fewer.")
        String email,
        @NotBlank(message = "Enter a password.")
        @Size(min = 12, max = 72, message = "Password must be between 12 and 72 characters.")
        String password
) {
    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", password=[REDACTED]]";
    }
}
