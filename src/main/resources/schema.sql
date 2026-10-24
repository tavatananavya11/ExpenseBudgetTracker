CREATE TABLE IF NOT EXISTS transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    entry_type TEXT NOT NULL CHECK (entry_type IN ('INCOME', 'EXPENSE')),
    category TEXT NOT NULL CHECK (category IN ('FOOD', 'RENT', 'TRAVEL', 'SHOPPING', 'EDUCATION', 'HEALTHCARE', 'BILLS', 'SALARY', 'OTHER')),
    amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
    description TEXT NOT NULL,
    entry_date TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(entry_date);
CREATE INDEX IF NOT EXISTS idx_transactions_type_date ON transactions(entry_type, entry_date);

CREATE TABLE IF NOT EXISTS budgets (
    budget_month TEXT PRIMARY KEY,
    amount_cents INTEGER NOT NULL CHECK (amount_cents >= 0)
);
