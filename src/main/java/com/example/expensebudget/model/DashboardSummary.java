package com.example.expensebudget.model;

import java.math.BigDecimal;

public record DashboardSummary(
        String month,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal monthlyBudget,
        BigDecimal remainingBalance,
        BigDecimal budgetRemaining,
        double budgetUsagePercent
) {
}
