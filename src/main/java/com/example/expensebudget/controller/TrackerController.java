package com.example.expensebudget.controller;

import com.example.expensebudget.model.BudgetRequest;
import com.example.expensebudget.model.DashboardSummary;
import com.example.expensebudget.model.MonthlyReport;
import com.example.expensebudget.model.TransactionEntry;
import com.example.expensebudget.model.TransactionRequest;
import com.example.expensebudget.service.TrackerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TrackerController {
    private final TrackerService trackerService;

    public TrackerController(TrackerService trackerService) {
        this.trackerService = trackerService;
    }

    @GetMapping("/api/transactions")
    public List<TransactionEntry> getTransactions(@RequestParam(required = false) String month) {
        return trackerService.getTransactions(month);
    }

    @PostMapping("/api/transactions")
    public TransactionEntry createTransaction(@Valid @RequestBody TransactionRequest request) {
        return trackerService.createTransaction(request);
    }

    @PutMapping("/api/transactions/{id}")
    public TransactionEntry updateTransaction(@PathVariable long id,
                                              @Valid @RequestBody TransactionRequest request) {
        return trackerService.updateTransaction(id, request);
    }

    @DeleteMapping("/api/transactions/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable long id) {
        trackerService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/dashboard")
    public DashboardSummary getDashboard(@RequestParam(required = false) String month) {
        return trackerService.getSummary(month);
    }

    @GetMapping("/api/reports")
    public MonthlyReport getReport(@RequestParam(required = false) String month) {
        return trackerService.getReport(month);
    }

    @PutMapping("/api/budgets/{month}")
    public DashboardSummary updateBudget(@PathVariable String month,
                                        @Valid @RequestBody BudgetRequest request) {
        return trackerService.updateBudget(month, request);
    }
}
