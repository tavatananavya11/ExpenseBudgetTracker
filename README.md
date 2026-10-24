# Expense & Budget Tracker

A beginner-friendly expense and budget tracker built with Java 21, Spring Boot, Maven, SQLite, HTML, CSS, JavaScript, and Chart.js. The app runs as one local web server: Spring serves the website and its REST API, while SQLite saves your records on disk.

## What it can do

- Protect accounts and financial records with email/password login, BCrypt password hashing, and per-account data ownership.
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

1. Open [http://localhost:8080/signup.html](http://localhost:8080/signup.html) and create an account. Passwords must be at least 12 characters.
2. Log in with that account.
3. Choose a month, set a budget, and add income or expense records.
4. Check that only this account's totals, transactions, and reports appear.
5. Log out, create a second account, and confirm that the first account's records are not visible.
6. Stop and restart the app, log back in, and check that the account and records remain.

The app does not create a default account or insert sample data. Each person must create their own account.

## Database and data safety

The SQLite database is created automatically at `data/expense-tracker.db` the first time the app starts. It contains user accounts, password hashes, transactions, and budgets. Financial records are linked to an account by a database owner ID; the owner ID is always derived from the authenticated server-side session, never accepted from the browser.

The app uses BCrypt to hash passwords, an HTTP-only session cookie, CSRF protection for writes (including login and logout), and a 30-minute session timeout. In local development the session cookie is not marked `Secure` so HTTP localhost works; set `SESSION_COOKIE_SECURE=true` for HTTPS production. Email verification, password reset, and authentication rate limiting are not included yet.

**Existing data migration:** on first startup after this update, the schema migration adds ownership while preserving existing transaction and budget rows. Because those older rows have no known owner, they remain unassigned and are not shown to any account. No account automatically inherits shared historical data. Back up the SQLite file before deployment; data can be assigned later only through an explicit, reviewed migration.

For Render, SQLite is appropriate only for a **single running service instance** with a persistent disk mounted at `/var/data` and `DB_PATH=/var/data/expense-tracker.db`. Render disks are attached to one service instance and are not a shared database for horizontally scaled instances. Back up the disk regularly. For a multi-instance or more resilient public deployment, migrate the database to managed PostgreSQL and use a shared server-side session store before scaling out. App sessions are held in the running instance, so a redeploy or restart logs users out; they can log in again without losing financial data on the persistent disk.

The charts use Chart.js from jsDelivr, and the typeface is loaded from Google Fonts. Those two visual assets need an internet connection; saving and loading records uses the local app and database.

## Run with Docker

Docker must be installed and running. From the project root, build and start the app with a persistent local Docker volume:

```powershell
docker build -t expense-budget-tracker .
docker run --name expense-budget-tracker -p 8080:8080 -e PORT=8080 -e DB_PATH=/var/data/expense-tracker.db -v expense-budget-data:/var/data expense-budget-tracker
```

Open [http://localhost:8080](http://localhost:8080). The named Docker volume keeps the database when you remove and recreate the container. To stop the container, press **Ctrl+C** and run `docker rm expense-budget-tracker` before creating another container with that name.

## Deploy on Render

The app now requires accounts, but anyone can create an account. Each account can access only its own records. The app does not include email verification, password recovery, or rate limiting yet.

1. Push the project to GitHub, then in Render choose **New → Web Service** and connect the `ExpenseBudgetTracker` repository.
2. Choose the branch you want to deploy and select **Docker** as the runtime. Set the Dockerfile path to `./Dockerfile`; leave the build and start command fields empty so Render uses the Dockerfile.
3. Add `DB_PATH=/var/data/expense-tracker.db` and `SESSION_COOKIE_SECURE=true` as environment variables. Render supplies `PORT`; the application uses it and defaults to `8080` locally. Do not override it with a different fixed port.
4. Add a Render persistent disk mounted at `/var/data`. Choose a disk size available for your Render plan. Without the disk, SQLite data in the container filesystem can be lost during a deploy or restart.
5. Run a single service instance while using SQLite. A Render disk is not shared among multiple instances. For horizontal scaling or stronger managed backups, move to PostgreSQL and configure a shared session store.
6. Before the first deploy, back up the existing Render SQLite disk. The migration preserves old rows as unassigned; they will no longer appear in a dashboard. After deploying, register separate accounts and verify each sees only its own records.
7. Open the public URL shown in Render. Signup is at `/signup.html`; the root redirects logged-out visitors to the login page.

The Docker image contains the application, not your local SQLite file. Do not add the database file to Git or bake it into the image.

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
| `GET` | `/api/auth/csrf` | Obtain a CSRF token for a browser request |
| `POST` | `/api/auth/register` | Create an account |
| `POST` | `/api/auth/login` | Log in and start a session |
| `GET` | `/api/auth/me` | Read the current account's email |
| `POST` | `/api/auth/logout` | End the session |
| `GET` | `/api/dashboard?month=2026-10` | Monthly totals and budget progress |
| `GET` | `/api/transactions?month=2026-10` | List that month's transactions |
| `POST` | `/api/transactions` | Create a transaction |
| `PUT` | `/api/transactions/{id}` | Edit a transaction |
| `DELETE` | `/api/transactions/{id}` | Delete a transaction |
| `PUT` | `/api/budgets/{month}` | Create or update a month's budget |
| `GET` | `/api/reports?month=2026-10` | Category spending and six-month totals |

Transaction types are `INCOME` and `EXPENSE`. Categories are `FOOD`, `RENT`, `TRAVEL`, `SHOPPING`, `EDUCATION`, `HEALTHCARE`, `BILLS`, `SALARY`, and `OTHER`. Request amounts use regular rupee values (for example, `1250.00`); the database stores the matching paise value.
