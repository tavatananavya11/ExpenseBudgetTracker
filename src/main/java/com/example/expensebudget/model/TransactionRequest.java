package com.example.expensebudget.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotNull(message = "Choose income or expense.")
        EntryType type,
        @NotNull(message = "Choose a category.")
        Category category,
        @NotNull(message = "Enter an amount.")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero.")
        @Digits(integer = 10, fraction = 2, message = "Amount can have up to 10 digits and 2 decimal places.")
        BigDecimal amount,
        @NotBlank(message = "Enter a description.")
        @Size(max = 200, message = "Description must be 200 characters or fewer.")
        String description,
        @NotNull(message = "Choose a date.")
        LocalDate date
) {
}
