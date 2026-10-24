package com.example.expensebudget.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionEntry(
        long id,
        EntryType type,
        Category category,
        BigDecimal amount,
        String description,
        LocalDate date
) {
}
