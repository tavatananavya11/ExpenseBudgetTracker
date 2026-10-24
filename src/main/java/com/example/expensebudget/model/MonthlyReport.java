package com.example.expensebudget.model;

import java.util.List;

public record MonthlyReport(
        DashboardSummary summary,
        List<CategoryTotal> spendingByCategory,
        List<MonthTotal> monthlyTotals
) {
}
