package com.example.expensebudget.model;

import java.math.BigDecimal;

public record MonthTotal(String month, BigDecimal totalIncome, BigDecimal totalExpenses) {
}
