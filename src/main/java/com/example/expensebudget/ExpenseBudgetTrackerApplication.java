package com.example.expensebudget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class ExpenseBudgetTrackerApplication {

    public static void main(String[] args) {
        createDatabaseDirectory();
        SpringApplication.run(ExpenseBudgetTrackerApplication.class, args);
    }

    private static void createDatabaseDirectory() {
        try {
            Path databasePath = Path.of(System.getenv().getOrDefault("DB_PATH", "./data/expense-tracker.db"));
            Path databaseDirectory = databasePath.toAbsolutePath().getParent();
            if (databaseDirectory != null) {
                Files.createDirectories(databaseDirectory);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the data directory for the SQLite database.", exception);
        }
    }
}
