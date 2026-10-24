package com.example.expensebudget.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BudgetRequest(
        @NotNull(message = "Enter a monthly budget.")
        @DecimalMin(value = "0.00", message = "Budget cannot be negative.")
        @Digits(integer = 10, fraction = 2, message = "Budget can have up to 10 digits and 2 decimal places.")
        BigDecimal amount
) {
}
