package com.example.expensebudget.service;

import com.example.expensebudget.model.BudgetRequest;
import com.example.expensebudget.model.CategoryTotal;
import com.example.expensebudget.model.DashboardSummary;
import com.example.expensebudget.model.MonthTotal;
import com.example.expensebudget.model.MonthlyReport;
import com.example.expensebudget.model.TransactionEntry;
import com.example.expensebudget.model.TransactionRequest;
import com.example.expensebudget.repository.BudgetRepository;
import com.example.expensebudget.repository.Money;
import com.example.expensebudget.repository.TransactionRepository;
import com.example.expensebudget.security.CurrentUserService;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.stream.IntStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TrackerService {
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final CurrentUserService currentUserService;

    public TrackerService(TransactionRepository transactionRepository, BudgetRepository budgetRepository,
                          CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.currentUserService = currentUserService;
    }

    public List<TransactionEntry> getTransactions(String month) {
        return transactionRepository.findByMonth(normalizeMonth(month), currentUserService.requiredUserId());
    }

    public TransactionEntry createTransaction(TransactionRequest request) {
        long id = transactionRepository.insert(request.type(), request.category(), Money.toCents(request.amount()),
                request.description().trim(), request.date(), currentUserService.requiredUserId());
        return transactionRepository.findById(id, currentUserService.requiredUserId())
                .orElseThrow(() -> new IllegalStateException("The saved transaction could not be loaded."));
    }

    public TransactionEntry updateTransaction(long id, TransactionRequest request) {
        boolean updated = transactionRepository.update(id, request.type(), request.category(),
                Money.toCents(request.amount()), request.description().trim(), request.date(),
                currentUserService.requiredUserId());
        if (!updated) {
            throw new ResourceNotFoundException("Transaction " + id + " was not found.");
        }
        return transactionRepository.findById(id, currentUserService.requiredUserId())
                .orElseThrow(() -> new IllegalStateException("The updated transaction could not be loaded."));
    }

    public void deleteTransaction(long id) {
        if (!transactionRepository.delete(id, currentUserService.requiredUserId())) {
            throw new ResourceNotFoundException("Transaction " + id + " was not found.");
        }
    }

    public DashboardSummary getSummary(String month) {
        YearMonth selectedMonth = parseMonth(month);
        String monthKey = selectedMonth.toString();
        long userId = currentUserService.requiredUserId();
        TransactionRepository.Totals totals = transactionRepository.totalsForMonth(monthKey, userId);
        long budgetCents = budgetRepository.findAmountCents(monthKey, userId).orElse(0L);
        long balanceCents = totals.incomeCents() - totals.expenseCents();
        long budgetRemainingCents = budgetCents - totals.expenseCents();
        double usagePercent = budgetCents == 0 ? 0.0 : totals.expenseCents() * 100.0 / budgetCents;

        return new DashboardSummary(monthKey, Money.fromCents(totals.incomeCents()),
                Money.fromCents(totals.expenseCents()), Money.fromCents(budgetCents),
                Money.fromCents(balanceCents), Money.fromCents(budgetRemainingCents), usagePercent);
    }

    public MonthlyReport getReport(String month) {
        YearMonth selectedMonth = parseMonth(month);
        DashboardSummary summary = getSummary(selectedMonth.toString());
        long userId = currentUserService.requiredUserId();
        List<CategoryTotal> categories = transactionRepository.expensesByCategory(selectedMonth.toString(), userId)
                .stream()
                .map(amount -> new CategoryTotal(amount.category(), Money.fromCents(amount.amountCents())))
                .toList();

        YearMonth startMonth = selectedMonth.minusMonths(5);
        Map<String, TransactionRepository.MonthAmount> totalsByMonth =
                transactionRepository.totalsBetween(startMonth.toString(), selectedMonth.toString(), userId)
                        .stream()
                        .collect(Collectors.toMap(TransactionRepository.MonthAmount::month, Function.identity()));
        List<MonthTotal> monthlyTotals = IntStream.range(0, 6)
                .mapToObj(offset -> startMonth.plusMonths(offset).toString())
                .map(monthKey -> {
                    TransactionRepository.MonthAmount amounts = totalsByMonth.get(monthKey);
                    if (amounts == null) {
                        return new MonthTotal(monthKey, Money.fromCents(0), Money.fromCents(0));
                    }
                    return new MonthTotal(monthKey, Money.fromCents(amounts.incomeCents()),
                            Money.fromCents(amounts.expenseCents()));
                })
                .toList();

        return new MonthlyReport(summary, categories, monthlyTotals);
    }

    public DashboardSummary updateBudget(String month, BudgetRequest request) {
        String monthKey = normalizeMonth(month);
        budgetRepository.save(monthKey, Money.toCents(request.amount()), currentUserService.requiredUserId());
        return getSummary(monthKey);
    }

    private String normalizeMonth(String month) {
        return parseMonth(month).toString();
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Month must use YYYY-MM format.");
        }
    }
}
