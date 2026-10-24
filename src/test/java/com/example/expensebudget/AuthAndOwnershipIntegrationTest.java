package com.example.expensebudget;

import com.example.expensebudget.model.CsrfTokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:expense-budget-auth-tests?mode=memory&cache=shared",
        "spring.datasource.hikari.minimum-idle=1",
        "spring.datasource.hikari.maximum-pool-size=1"
})
class AuthAndOwnershipIntegrationTest {
    private static final String PASSWORD = "a-strong-password-12";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void registrationHashesPasswordsAndRejectsDuplicateEmailsIgnoringCase() throws Exception {
        String email = uniqueEmail();
        Csrf firstCsrf = csrf(null);

        mockMvc.perform(post("/api/auth/register")
                        .cookie(firstCsrf.cookie())
                        .header(firstCsrf.headerName(), firstCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(PASSWORD))));

        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE email = ?", String.class, email);
        assertThat(passwordHash).startsWith("$2a$12$").isNotEqualTo(PASSWORD);

        Csrf csrf = csrf(null);
        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email.toUpperCase(), "password", PASSWORD))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with that email already exists."));
    }

    @Test
    void privatePagesAndApisRequireAuthenticationAndWritesRequireCsrf() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("/login.html"));
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please log in to continue."));
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", uniqueEmail(), "password", PASSWORD))))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginLogoutAndInvalidCredentialsUsePrivateSession() throws Exception {
        String email = uniqueEmail();
        register(email);

        Csrf loginCsrf = csrf(null);
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .cookie(loginCsrf.cookie())
                        .header(loginCsrf.headerName(), loginCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(login.getResponse().getContentAsString()).doesNotContain(PASSWORD);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        Csrf logoutCsrf = csrf(session);
        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .cookie(logoutCsrf.cookie())
                        .header(logoutCsrf.headerName(), logoutCsrf.token()))
                .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());

        Csrf invalidLoginCsrf = csrf(null);
        mockMvc.perform(post("/api/auth/login")
                        .cookie(invalidLoginCsrf.cookie())
                        .header(invalidLoginCsrf.headerName(), invalidLoginCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "incorrect-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void eachAccountCanAccessOnlyItsOwnTransactionsAndBudgets() throws Exception {
        String firstEmail = uniqueEmail();
        String secondEmail = uniqueEmail();
        jdbcTemplate.update("""
                INSERT INTO transactions (entry_type, category, amount_cents, description, entry_date, user_id)
                VALUES ('EXPENSE', 'OTHER', 990000, 'Unassigned legacy entry', '2026-10-01', NULL)
                """);
        jdbcTemplate.update("""
                INSERT INTO budgets (budget_month, amount_cents, user_id)
                VALUES ('2026-10', 600000, NULL)
                """);
        register(firstEmail);
        register(secondEmail);
        MockHttpSession firstSession = login(firstEmail);
        MockHttpSession secondSession = login(secondEmail);

        Csrf firstCsrf = csrf(firstSession);
        MvcResult created = mockMvc.perform(post("/api/transactions")
                        .session(firstSession)
                        .cookie(firstCsrf.cookie())
                        .header(firstCsrf.headerName(), firstCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "type", "EXPENSE",
                                "category", "FOOD",
                                "amount", 1250.00,
                                "description", "Private groceries",
                                "date", "2026-10-24"))))
                .andExpect(status().isOk())
                .andReturn();
        long transactionId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/transactions?month=2026-10").session(firstSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Private groceries"));
        mockMvc.perform(get("/api/transactions?month=2026-10").session(secondSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/dashboard?month=2026-10").session(secondSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpenses").value(0));
        mockMvc.perform(get("/api/reports?month=2026-10").session(secondSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spendingByCategory.length()").value(0));

        Csrf secondCsrf = csrf(secondSession);
        mockMvc.perform(put("/api/transactions/{id}", transactionId)
                        .session(secondSession)
                        .cookie(secondCsrf.cookie())
                        .header(secondCsrf.headerName(), secondCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "type", "EXPENSE",
                                "category", "FOOD",
                                "amount", 1.00,
                                "description", "Not yours",
                                "date", "2026-10-24"))))
                .andExpect(status().isNotFound());
        secondCsrf = csrf(secondSession);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/transactions/{id}", transactionId)
                        .session(secondSession)
                        .cookie(secondCsrf.cookie())
                        .header(secondCsrf.headerName(), secondCsrf.token()))
                .andExpect(status().isNotFound());

        Csrf budgetCsrf = csrf(firstSession);
        mockMvc.perform(put("/api/budgets/2026-10")
                        .session(firstSession)
                        .cookie(budgetCsrf.cookie())
                        .header(budgetCsrf.headerName(), budgetCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", 5000.00))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyBudget").value(5000.00));
        mockMvc.perform(get("/api/dashboard?month=2026-10").session(secondSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyBudget").value(0));
        mockMvc.perform(get("/api/dashboard?month=2026-10").session(firstSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpenses").value(1250.00))
                .andExpect(jsonPath("$.monthlyBudget").value(5000.00));
    }

    private void register(String email) throws Exception {
        Csrf csrf = csrf(null);
        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isCreated());
    }

    private MockHttpSession login(String email) throws Exception {
        Csrf csrf = csrf(null);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private Csrf csrf(MockHttpSession session) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/auth/csrf");
        if (session != null) {
            request.session(session);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        CsrfTokenResponse token = objectMapper.readValue(result.getResponse().getContentAsString(),
                CsrfTokenResponse.class);
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        return new Csrf(token.headerName(), token.token(), cookie);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String uniqueEmail() {
        return UUID.randomUUID() + "@example.test";
    }

    private record Csrf(String headerName, String token, Cookie cookie) {
    }
}
