"use strict";

async function getCsrfToken() {
    const response = await fetch("/api/auth/csrf", { cache: "no-store" });
    if (!response.ok) {
        throw new Error("Could not prepare a secure request. Please refresh and try again.");
    }
    return response.json();
}

async function submitAuthForm(form, endpoint, isSignup) {
    const message = document.querySelector("#formMessage");
    const submitButton = document.querySelector("#submitButton");
    message.textContent = "";
    message.classList.remove("visible", "success");
    submitButton.disabled = true;

    try {
        const csrf = await getCsrfToken();
        const formData = new FormData(form);
        const response = await fetch(endpoint, {
            method: "POST",
            cache: "no-store",
            headers: {
                "Content-Type": "application/json",
                [csrf.headerName]: csrf.token
            },
            body: JSON.stringify({
                email: String(formData.get("email")).trim(),
                password: String(formData.get("password"))
            })
        });
        const result = await response.json();
        if (!response.ok) {
            throw new Error(result.message || "We could not complete your request. Please try again.");
        }

        if (isSignup) {
            message.textContent = "Your account is ready. Log in with your new account to continue.";
            message.classList.add("visible", "success");
            form.reset();
            window.setTimeout(() => window.location.assign("/login.html"), 1200);
        } else {
            window.location.replace("/");
        }
    } catch (error) {
        message.textContent = error instanceof TypeError
            ? "Could not reach the app. Check your connection and try again."
            : error.message;
        message.classList.add("visible");
    } finally {
        submitButton.disabled = false;
    }
}

document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.querySelector("#loginForm");
    const signupForm = document.querySelector("#signupForm");
    const message = document.querySelector("#formMessage");

    if (new URLSearchParams(window.location.search).has("expired") && message) {
        message.textContent = "Your session ended. Please log in again.";
        message.classList.add("visible");
    }

    if (loginForm) {
        loginForm.addEventListener("submit", event => {
            event.preventDefault();
            if (!loginForm.reportValidity()) {
                return;
            }
            submitAuthForm(loginForm, "/api/auth/login", false);
        });
    }

    if (signupForm) {
        signupForm.addEventListener("submit", event => {
            event.preventDefault();
            if (!signupForm.reportValidity()) {
                return;
            }
            submitAuthForm(signupForm, "/api/auth/register", true);
        });
    }
});
