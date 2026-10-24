# Expense & Budget Tracker

A beginner-friendly expense and budget tracker built with Java 21, Spring Boot, Maven, SQLite, HTML, CSS, JavaScript, and Chart.js. The app runs as one local web server: Spring serves the website and its REST API, while SQLite saves your records on disk.

## What it can do

- Show monthly income, expenses, budget, remaining balance, and budget progress.
- Add, edit, view, and delete income and expense records.
- Save monthly budgets and view spending by category.
- Compare income and expenses over the last six months.
- Store amounts in paise in SQLite for accurate currency calculations.
- Display currency in Indian Rupees (INR).
- Validate form fields and show success or error messages.

## 1. Install the required tools

This project requires **JDK 21** and **Apache Maven 3.9 or later**.

1. Install a JDK 21 distribution, such as [Eclipse Temurin 21](https://adoptium.net/temurin/releases/?version=21).
2. Install [Apache Maven](https://maven.apache.org/install.html). On Windows, add Maven's `bin` folder to your `Path` environment variable.
3. Close and reopen the VS Code terminal. Check that both tools are available:

   ```powershell
   java -version
   mvn -version
   ```

   The Java output should show version 21. If Windows says a command is not recognized, finish the installation or update `Path`, then open a new terminal.

4. In VS Code, install the **Extension Pack for Java** to get Java language support.

## 2. Start the website

Open a terminal in the project folder and run:

```powershell
mvn spring-boot:run
```

The first run downloads Maven dependencies, so it needs an internet connection. When Spring Boot reports that it has started, open [http://localhost:8080](http://localhost:8080) in your browser. The web page and API are served from the same address.

Stop the server with **Ctrl+C** in the terminal.

## 3. Try it out

1. Choose a month with the month picker.
2. Select **Set or update budget**, enter a non-negative amount, and save it.
3. Select **Add transaction**, choose income or expense, enter a category, amount, description, and date, then save.
4. Check that the summary, transaction list, category chart, and six-month chart update.
5. Use the edit and delete buttons beside a transaction to change or remove it.
6. Stop and restart the app. The saved data should still be there.

The app starts with an empty database; it does not insert sample data.

## Database and data safety

The SQLite database is created automatically at `data/expense-tracker.db` the first time the app starts. Keep this file to preserve your records. The app does not require a separately installed database server.

The charts use Chart.js from jsDelivr, and the typeface is loaded from Google Fonts. Those two visual assets need an internet connection; saving and loading records uses the local app and database.

## Build and test

Run the automated tests:

```powershell
mvn test
```

Build a runnable application JAR:

```powershell
mvn package
```

Run the built JAR from the project folder:

```powershell
java -jar target\expense-budget-tracker-0.0.1-SNAPSHOT.jar
```

## API overview

The browser uses these local JSON endpoints:

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/dashboard?month=2026-10` | Monthly totals and budget progress |
| `GET` | `/api/transactions?month=2026-10` | List that month's transactions |
| `POST` | `/api/transactions` | Create a transaction |
| `PUT` | `/api/transactions/{id}` | Edit a transaction |
| `DELETE` | `/api/transactions/{id}` | Delete a transaction |
| `PUT` | `/api/budgets/{month}` | Create or update a month's budget |
| `GET` | `/api/reports?month=2026-10` | Category spending and six-month totals |

Transaction types are `INCOME` and `EXPENSE`. Categories are `FOOD`, `RENT`, `TRAVEL`, `SHOPPING`, `EDUCATION`, `HEALTHCARE`, `BILLS`, `SALARY`, and `OTHER`. Request amounts use regular rupee values (for example, `1250.00`); the database stores the matching paise value.
