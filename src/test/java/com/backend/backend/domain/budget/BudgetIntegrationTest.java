package com.backend.backend.domain.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.backend.domain.category.Category;
import com.backend.backend.domain.category.CategoryRepository;
import com.backend.backend.domain.category.CategoryType;
import com.backend.backend.domain.transaction.Transaction;
import com.backend.backend.domain.transaction.TransactionRepository;
import com.backend.backend.domain.transaction.TransactionType;
import com.backend.backend.domain.user.User;
import com.backend.backend.domain.user.UserRepository;
import com.backend.backend.security.JwtService;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
        properties = {
            "app.env.validation.enabled=false",
            "app.jwt.secret-key=01234567890123456789012345678901"
        })
@AutoConfigureMockMvc
@Transactional
class BudgetIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private JwtService jwtService;

    @Autowired private UserRepository userRepository;

    @Autowired private CategoryRepository categoryRepository;

    @Autowired private TransactionRepository transactionRepository;

    @Autowired private BudgetRepository budgetRepository;

    @Autowired private PasswordEncoder passwordEncoder;

    private User userOne;
    private User userTwo;
    private String tokenOne;
    private String tokenTwo;
    private Category expenseCategory;

    @BeforeEach
    void setUp() {
        userOne = createUser("budget-user1@test.com");
        userTwo = createUser("budget-user2@test.com");
        tokenOne = jwtService.generateToken(userOne.getEmail());
        tokenTwo = jwtService.generateToken(userTwo.getEmail());

        expenseCategory = new Category();
        expenseCategory.setName("Alimentación Test");
        expenseCategory.setType(CategoryType.EXPENSE);
        expenseCategory.setSystem(false);
        expenseCategory.setUser(userOne);
        categoryRepository.save(expenseCategory);
    }

    @Test
    void createListUpdateDeleteBudgetLifecycle() throws Exception {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        String createBody =
                """
                {
                  "categoryId": "%s",
                  "amountLimit": 500000,
                  "period": "MONTHLY",
                  "startDate": "%s"
                }
                """
                        .formatted(expenseCategory.getId(), monthStart);

        MvcResult createResult =
                mockMvc.perform(
                                post("/budgets")
                                        .header("Authorization", "Bearer " + tokenOne)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(createBody))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.amountLimit").value(500000))
                        .andExpect(jsonPath("$.period").value("MONTHLY"))
                        .andExpect(jsonPath("$.isActive").value(true))
                        .andReturn();

        UUID budgetId =
                UUID.fromString(
                        JsonPath.read(createResult.getResponse().getContentAsString(), "$.id"));

        mockMvc.perform(get("/budgets").header("Authorization", "Bearer " + tokenOne))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(budgetId.toString()));

        mockMvc.perform(
                        put("/budgets/" + budgetId)
                                .header("Authorization", "Bearer " + tokenOne)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"isActive\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        mockMvc.perform(
                        delete("/budgets/" + budgetId)
                                .header("Authorization", "Bearer " + tokenOne))
                .andExpect(status().isNoContent());

        assertThat(budgetRepository.findById(budgetId)).isEmpty();
    }

    @Test
    void statusReflectsExpenseTransactionsInCurrentPeriod() throws Exception {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();

        createExpenseTransaction(new BigDecimal("300000"), today);

        Budget budget = new Budget();
        budget.setUser(userOne);
        budget.setCategory(expenseCategory);
        budget.setAmountLimit(new BigDecimal("500000"));
        budget.setPeriod(BudgetPeriod.MONTHLY);
        budget.setStartDate(monthStart);
        budget.setActive(true);
        budgetRepository.save(budget);

        mockMvc.perform(
                        get("/budgets/" + budget.getId() + "/status")
                                .header("Authorization", "Bearer " + tokenOne))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spent").value(300000))
                .andExpect(jsonPath("$.remaining").value(200000))
                .andExpect(jsonPath("$.percentage").value(60.0));

        createExpenseTransaction(new BigDecimal("250000"), today);

        mockMvc.perform(
                        get("/budgets/" + budget.getId() + "/status")
                                .header("Authorization", "Bearer " + tokenOne))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spent").value(550000))
                .andExpect(jsonPath("$.remaining").value(-50000))
                .andExpect(jsonPath("$.percentage").value(110.0));
    }

    @Test
    void userCannotAccessAnotherUsersBudget() throws Exception {
        Budget budget = new Budget();
        budget.setUser(userOne);
        budget.setCategory(expenseCategory);
        budget.setAmountLimit(new BigDecimal("100000"));
        budget.setPeriod(BudgetPeriod.MONTHLY);
        budget.setStartDate(LocalDate.now().withDayOfMonth(1));
        budget.setActive(true);
        budgetRepository.save(budget);

        mockMvc.perform(
                        get("/budgets/" + budget.getId() + "/status")
                                .header("Authorization", "Bearer " + tokenTwo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Budget not found"));
    }

    @Test
    void cannotCreateBudgetOnIncomeCategory() throws Exception {
        Category incomeCategory = new Category();
        incomeCategory.setName("Salario Test");
        incomeCategory.setType(CategoryType.INCOME);
        incomeCategory.setSystem(false);
        incomeCategory.setUser(userOne);
        categoryRepository.save(incomeCategory);

        String body =
                """
                {
                  "categoryId": "%s",
                  "amountLimit": 100000,
                  "period": "MONTHLY",
                  "startDate": "%s"
                }
                """
                        .formatted(incomeCategory.getId(), LocalDate.now().withDayOfMonth(1));

        mockMvc.perform(
                        post("/budgets")
                                .header("Authorization", "Bearer " + tokenOne)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Budgets can only be created for expense categories"
                                                + " (EXPENSE or BOTH)"));
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/budgets")).andExpect(status().isForbidden());
    }

    private void createExpenseTransaction(BigDecimal amount, LocalDate date) {
        Transaction transaction = new Transaction();
        transaction.setUser(userOne);
        transaction.setCategory(expenseCategory);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setTransactionDate(date);
        transaction.setDescription("Gasto de prueba");
        transactionRepository.save(transaction);
    }

    private User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setName("Test User");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        return userRepository.save(user);
    }
}
