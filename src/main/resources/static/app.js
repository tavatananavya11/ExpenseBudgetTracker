"use strict";

const CATEGORY_LABELS = {
    FOOD: "Food",
    RENT: "Rent",
    TRAVEL: "Travel",
    SHOPPING: "Shopping",
    EDUCATION: "Education",
    HEALTHCARE: "Healthcare",
    BILLS: "Bills",
    SALARY: "Salary",
    OTHER: "Other"
};

const CATEGORY_COLORS = ["#4aa781", "#6589d8", "#e1a45e", "#9679cc", "#e07879", "#61aeb0", "#d78fae", "#a4a968", "#909baa"];
const CATEGORY_GLYPHS = {
    FOOD: "⌁", RENT: "⌂", TRAVEL: "↗", SHOPPING: "◇", EDUCATION: "▤",
    HEALTHCARE: "+", BILLS: "▧", SALARY: "↙", OTHER: "·"
};

function getLocalMonth() {
    const today = new Date();
    return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}`;
}

function getDefaultTransactionDate() {
    if (state.month !== getLocalMonth()) {
        return `${state.month}-01`;
    }
    const today = new Date();
    return `${state.month}-${String(today.getDate()).padStart(2, "0")}`;
}

const currency = new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
});
const state = {
    month: getLocalMonth(),
    transactions: [],
    categoryChart: null,
    monthlyChart: null,
    editingId: null,
    toastTimer: null
};

const elements = {
    monthPicker: document.querySelector("#monthPicker"),
    totalIncome: document.querySelector("#totalIncome"),
    totalExpenses: document.querySelector("#totalExpenses"),
    monthlyBudget: document.querySelector("#monthlyBudget"),
    remainingBalance: document.querySelector("#remainingBalance"),
    budgetMessage: document.querySelector("#budgetMessage"),
    budgetSpent: document.querySelector("#budgetSpent"),
    budgetLimit: document.querySelector("#budgetLimit"),
    budgetRemaining: document.querySelector("#budgetRemaining"),
    budgetProgress: document.querySelector("#budgetProgress"),
    budgetProgressFill: document.querySelector("#budgetProgressFill"),
    transactionCount: document.querySelector("#transactionCount"),
    transactionsBody: document.querySelector("#transactionsBody"),
    transactionsEmpty: document.querySelector("#transactionsEmpty"),
    categoryLegend: document.querySelector("#categoryLegend"),
    categoryEmptyMessage: document.querySelector("#categoryEmptyMessage"),
    transactionDialog: document.querySelector("#transactionDialog"),
    transactionForm: document.querySelector("#transactionForm"),
    transactionDialogTitle: document.querySelector("#transactionDialogTitle"),
    budgetDialog: document.querySelector("#budgetDialog"),
    budgetForm: document.querySelector("#budgetForm"),
    budgetAmount: document.querySelector("#budgetAmount"),
    budgetMonthLabel: document.querySelector("#budgetMonthLabel"),
    toast: document.querySelector("#toast")
};

function formatMoney(value) {
    return currency.format(Number(value) || 0);
}

function displayCategory(category) {
    return CATEGORY_LABELS[category] || "Other";
}

function displayDate(dateString) {
    return new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short", year: "numeric" })
        .format(new Date(`${dateString}T00:00:00`));
}

function monthLabel(month) {
    const [year, numericMonth] = month.split("-").map(Number);
    return new Intl.DateTimeFormat("en-IN", { month: "long", year: "numeric" })
        .format(new Date(year, numericMonth - 1, 1));
}

async function apiRequest(url, options = {}) {
    let response;
    try {
        const headers = { "Content-Type": "application/json", ...options.headers };
        if (options.method && !["GET", "HEAD", "OPTIONS"].includes(options.method.toUpperCase())) {
            const csrfResponse = await fetch("/api/auth/csrf", { cache: "no-store" });
            if (!csrfResponse.ok) {
                if (csrfResponse.status === 401) {
                    window.location.replace("/login.html?expired=1");
                }
                throw new Error("Could not verify your session. Refresh the page and try again.");
            }
            const csrf = await csrfResponse.json();
            headers[csrf.headerName] = csrf.token;
        }
        response = await fetch(url, {
            ...options,
            headers,
            cache: "no-store"
        });
    } catch (error) {
        if (error instanceof TypeError) {
            throw new Error("Could not reach the app server. Check that it is running and try again.");
        }
        throw error;
    }

    if (!response.ok) {
        if (response.status === 401) {
            window.location.replace("/login.html?expired=1");
            throw new Error("Your session has ended. Please log in again.");
        }
        let message = `Request failed (${response.status}).`;
        try {
            const errorBody = await response.json();
            if (typeof errorBody.message === "string") {
                message = errorBody.message;
            }
        } catch {
            // The response may have no JSON body.
        }
        throw new Error(message);
    }

    if (response.status === 204) {
        return null;
    }
    return response.json();
}

async function loadDashboard() {
    elements.monthPicker.value = state.month;
    try {
        const monthParam = encodeURIComponent(state.month);
        const [summary, report, transactions] = await Promise.all([
            apiRequest(`/api/dashboard?month=${monthParam}`),
            apiRequest(`/api/reports?month=${monthParam}`),
            apiRequest(`/api/transactions?month=${monthParam}`)
        ]);
        state.transactions = transactions;
        renderSummary(summary);
        renderCharts(report);
        renderTransactions();
    } catch (error) {
        showToast(error.message, true);
    }
}

function renderSummary(summary) {
    elements.totalIncome.textContent = formatMoney(summary.totalIncome);
    elements.totalExpenses.textContent = formatMoney(summary.totalExpenses);
    elements.monthlyBudget.textContent = formatMoney(summary.monthlyBudget);
    elements.remainingBalance.textContent = formatMoney(summary.remainingBalance);
    elements.remainingBalance.classList.toggle("negative", Number(summary.remainingBalance) < 0);
    elements.budgetSpent.textContent = `${formatMoney(summary.totalExpenses)} spent`;
    elements.budgetLimit.textContent = `${formatMoney(summary.monthlyBudget)} budget`;
    elements.budgetRemaining.textContent = formatMoney(summary.budgetRemaining);
    elements.budgetRemaining.classList.toggle("negative", Number(summary.budgetRemaining) < 0);

    const usage = Number(summary.budgetUsagePercent) || 0;
    const visualProgress = Math.min(Math.max(usage, 0), 100);
    elements.budgetProgressFill.style.width = `${visualProgress}%`;
    elements.budgetProgressFill.classList.toggle("over-budget", usage > 100);
    elements.budgetProgress.setAttribute("aria-valuenow", String(Math.round(visualProgress)));
    if (Number(summary.monthlyBudget) <= 0) {
        elements.budgetMessage.textContent = "Set a budget to start tracking your spending.";
    } else if (usage > 100) {
        elements.budgetMessage.textContent = `You’re ${Math.round(usage - 100)}% over your budget.`;
    } else {
        elements.budgetMessage.textContent = `${Math.round(usage)}% of your monthly budget used.`;
    }
}

function renderCharts(report) {
    renderCategoryChart(report.spendingByCategory || []);
    renderMonthlyChart(report.monthlyTotals || []);
}

function renderCategoryChart(categories) {
    const canvas = document.querySelector("#categoryChart");
    elements.categoryLegend.replaceChildren();
    const hasData = categories.length > 0;
    elements.categoryEmptyMessage.hidden = hasData;

    if (state.categoryChart) {
        state.categoryChart.destroy();
        state.categoryChart = null;
    }
    if (!hasData || typeof Chart === "undefined") {
        return;
    }

    state.categoryChart = new Chart(canvas, {
        type: "doughnut",
        data: {
            labels: categories.map(item => displayCategory(item.category)),
            datasets: [{
                data: categories.map(item => Number(item.amount)),
                backgroundColor: categories.map(item => CATEGORY_COLORS[Object.keys(CATEGORY_LABELS).indexOf(item.category)] || CATEGORY_COLORS[8]),
                borderWidth: 3,
                borderColor: "#ffffff",
                hoverOffset: 5
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: "69%",
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: context => ` ${context.label}: ${formatMoney(context.raw)}`
                    }
                }
            }
        }
    });

    categories.forEach(item => {
        const legendItem = document.createElement("span");
        legendItem.className = "legend-item";
        const dot = document.createElement("span");
        dot.className = "legend-dot";
        dot.style.backgroundColor = CATEGORY_COLORS[Object.keys(CATEGORY_LABELS).indexOf(item.category)] || CATEGORY_COLORS[8];
        const label = document.createElement("span");
        label.textContent = `${displayCategory(item.category)} · ${formatMoney(item.amount)}`;
        legendItem.append(dot, label);
        elements.categoryLegend.append(legendItem);
    });
}

function renderMonthlyChart(months) {
    const canvas = document.querySelector("#monthlyChart");
    if (state.monthlyChart) {
        state.monthlyChart.destroy();
    }
    if (typeof Chart === "undefined") {
        return;
    }

    state.monthlyChart = new Chart(canvas, {
        type: "bar",
        data: {
            labels: months.map(item => monthLabel(item.month).split(" ")[0]),
            datasets: [
                {
                    label: "Income",
                    data: months.map(item => Number(item.totalIncome)),
                    backgroundColor: "#6bbf98",
                    borderRadius: 4,
                    maxBarThickness: 19
                },
                {
                    label: "Expenses",
                    data: months.map(item => Number(item.totalExpenses)),
                    backgroundColor: "#e68b89",
                    borderRadius: 4,
                    maxBarThickness: 19
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { intersect: false, mode: "index" },
            plugins: {
                legend: {
                    position: "bottom",
                    labels: { usePointStyle: true, pointStyle: "circle", boxWidth: 7, boxHeight: 7, color: "#8490a0", font: { size: 10 }, padding: 17 }
                },
                tooltip: {
                    callbacks: {
                        label: context => ` ${context.dataset.label}: ${formatMoney(context.raw)}`
                    }
                }
            },
            scales: {
                x: { grid: { display: false }, border: { display: false }, ticks: { color: "#919baa", font: { size: 9 } } },
                y: {
                    beginAtZero: true,
                    border: { display: false },
                    grid: { color: "#f0f2f5" },
                    ticks: {
                        color: "#a0a8b3",
                        font: { size: 9 },
                        maxTicksLimit: 5,
                        callback: value => Number(value) >= 1000 ? `₹${Number(value) / 1000}k` : `₹${value}`
                    }
                }
            }
        }
    });
}

function createCell(className, text) {
    const cell = document.createElement("td");
    if (className) {
        cell.className = className;
    }
    cell.textContent = text;
    return cell;
}

function renderTransactions() {
    elements.transactionsBody.replaceChildren();
    elements.transactionCount.textContent = `${state.transactions.length} ${state.transactions.length === 1 ? "entry" : "entries"}`;
    elements.transactionsEmpty.classList.toggle("visible", state.transactions.length === 0);

    state.transactions.forEach(transaction => {
        const row = document.createElement("tr");
        const nameCell = document.createElement("td");
        const nameWrap = document.createElement("div");
        nameWrap.className = "transaction-name";
        const glyph = document.createElement("span");
        glyph.className = "transaction-glyph";
        glyph.textContent = CATEGORY_GLYPHS[transaction.category] || "·";
        const name = document.createElement("span");
        const title = document.createElement("strong");
        title.textContent = transaction.description;
        const type = document.createElement("small");
        type.textContent = transaction.type === "INCOME" ? "Income" : "Expense";
        name.append(title, type);
        nameWrap.append(glyph, name);
        nameCell.append(nameWrap);
        row.append(nameCell);

        const category = document.createElement("td");
        const pill = document.createElement("span");
        pill.className = "category-pill";
        pill.textContent = displayCategory(transaction.category);
        category.append(pill);
        row.append(category);
        row.append(createCell("date-cell", displayDate(transaction.date)));
        row.append(createCell(`amount-cell ${transaction.type.toLowerCase()}`,
            `${transaction.type === "INCOME" ? "+" : "−"}${formatMoney(transaction.amount)}`));

        const actionsCell = document.createElement("td");
        const actions = document.createElement("div");
        actions.className = "row-actions";
        const editButton = document.createElement("button");
        editButton.type = "button";
        editButton.className = "row-action";
        editButton.dataset.action = "edit";
        editButton.dataset.id = String(transaction.id);
        editButton.setAttribute("aria-label", `Edit ${transaction.description}`);
        editButton.textContent = "✎";
        const deleteButton = document.createElement("button");
        deleteButton.type = "button";
        deleteButton.className = "row-action delete";
        deleteButton.dataset.action = "delete";
        deleteButton.dataset.id = String(transaction.id);
        deleteButton.setAttribute("aria-label", `Delete ${transaction.description}`);
        deleteButton.textContent = "×";
        actions.append(editButton, deleteButton);
        actionsCell.append(actions);
        row.append(actionsCell);
        elements.transactionsBody.append(row);
    });
}

function openTransactionDialog(transaction = null) {
    state.editingId = transaction ? transaction.id : null;
    elements.transactionForm.reset();
    elements.transactionDialogTitle.textContent = transaction ? "Edit transaction" : "Add transaction";
    document.querySelector("#saveTransactionButton").textContent = transaction ? "Save changes" : "Save transaction";
    document.querySelector(`input[name="type"][value="${transaction?.type || "EXPENSE"}"]`).checked = true;
    document.querySelector("#transactionAmount").value = transaction ? transaction.amount : "";
    document.querySelector("#transactionCategory").value = transaction?.category || "FOOD";
    document.querySelector("#transactionDescription").value = transaction?.description || "";
    document.querySelector("#transactionDate").value = transaction?.date || getDefaultTransactionDate();
    elements.transactionDialog.showModal();
}

function showToast(message, isError = false) {
    window.clearTimeout(state.toastTimer);
    elements.toast.textContent = message;
    elements.toast.classList.toggle("error", isError);
    elements.toast.classList.add("visible");
    state.toastTimer = window.setTimeout(() => elements.toast.classList.remove("visible"), 3400);
}

async function saveTransaction(event) {
    event.preventDefault();
    const formData = new FormData(elements.transactionForm);
    const request = {
        type: formData.get("type"),
        category: formData.get("category"),
        amount: Number(formData.get("amount")),
        description: String(formData.get("description")).trim(),
        date: formData.get("date")
    };
    const isEditing = state.editingId !== null;
    const url = isEditing ? `/api/transactions/${state.editingId}` : "/api/transactions";

    try {
        await apiRequest(url, { method: isEditing ? "PUT" : "POST", body: JSON.stringify(request) });
        elements.transactionDialog.close();
        showToast(isEditing ? "Transaction updated." : "Transaction added.");
        await loadDashboard();
    } catch (error) {
        showToast(error.message, true);
    }
}

async function deleteTransaction(id) {
    const transaction = state.transactions.find(item => item.id === id);
    if (!transaction || !window.confirm(`Delete "${transaction.description}"? This cannot be undone.`)) {
        return;
    }
    try {
        await apiRequest(`/api/transactions/${id}`, { method: "DELETE" });
        showToast("Transaction deleted.");
        await loadDashboard();
    } catch (error) {
        showToast(error.message, true);
    }
}

async function saveBudget(event) {
    event.preventDefault();
    const amount = Number(elements.budgetAmount.value);
    try {
        await apiRequest(`/api/budgets/${encodeURIComponent(state.month)}`, {
            method: "PUT",
            body: JSON.stringify({ amount })
        });
        elements.budgetDialog.close();
        showToast("Monthly budget saved.");
        await loadDashboard();
    } catch (error) {
        showToast(error.message, true);
    }
}

function openBudgetDialog() {
    elements.budgetMonthLabel.textContent = monthLabel(state.month);
    elements.budgetAmount.value = document.querySelector("#monthlyBudget").textContent
        .replace(/[^\d.,-]/g, "").replace(/,/g, "");
    elements.budgetDialog.showModal();
}

function initialize() {
    elements.monthPicker.value = state.month;
    elements.monthPicker.addEventListener("change", () => {
        if (!elements.monthPicker.value) {
            return;
        }
        state.month = elements.monthPicker.value;
        loadDashboard();
    });
    document.querySelector("#addTransactionButton").addEventListener("click", () => openTransactionDialog());
    document.querySelector("#emptyAddButton").addEventListener("click", () => openTransactionDialog());
    document.querySelector("#editBudgetButton").addEventListener("click", openBudgetDialog);
    document.querySelector("#logoutButton").addEventListener("click", logout);
    elements.transactionForm.addEventListener("submit", saveTransaction);
    elements.budgetForm.addEventListener("submit", saveBudget);
    elements.transactionsBody.addEventListener("click", event => {
        const button = event.target.closest("button[data-action]");
        if (!button) {
            return;
        }
        const id = Number(button.dataset.id);
        if (button.dataset.action === "edit") {
            const transaction = state.transactions.find(item => item.id === id);
            if (transaction) {
                openTransactionDialog(transaction);
            }
        } else if (button.dataset.action === "delete") {
            deleteTransaction(id);
        }
    });
    document.querySelectorAll("[data-close-dialog]").forEach(button => {
        button.addEventListener("click", () => document.getElementById(button.dataset.closeDialog).close());
    });
    apiRequest("/api/auth/me")
        .then(profile => {
            document.querySelector("#currentUserEmail").textContent = profile.email;
            return loadDashboard();
        })
        .catch(error => showToast(error.message, true));
}

async function logout() {
    try {
        await apiRequest("/api/auth/logout", { method: "POST" });
        window.location.replace("/login.html");
    } catch (error) {
        showToast(error.message, true);
    }
}

document.addEventListener("DOMContentLoaded", initialize);
