package com.backend.backend.domain.transaction;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.backend.domain.category.Category;
import com.backend.backend.domain.category.CategoryRepository;
import com.backend.backend.domain.category.CategoryType;
import com.backend.backend.domain.user.User;
import com.backend.backend.domain.user.UserRepository;
import com.backend.backend.security.JwtService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
        properties = {
            "app.env.validation.enabled=false",
            "app.jwt.secret-key=01234567890123456789012345678901"
        })
@AutoConfigureMockMvc
@Transactional
class SummaryIntegrationTest {

    private static final LocalDate PERIOD_START = LocalDate.of(2026, 5, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 5, 31);

    @Autowired private MockMvc mockMvc;

    @Autowired private JwtService jwtService;

    @Autowired private UserRepository userRepository;

    @Autowired private CategoryRepository categoryRepository;

    @Autowired private TransactionRepository transactionRepository;

    @Autowired private PasswordEncoder passwordEncoder;

    private User user;
    private String token;
    private Category salaryCategory;
    private Category foodCategory;
    private Category transportCategory;

    @BeforeEach
    void setUp() {
        user = createUser("summary-user@test.com");
        token = jwtService.generateToken(user.getEmail());

        salaryCategory = createCategory("Salario Summary", CategoryType.INCOME, user);
        foodCategory = createCategory("Alimentación Summary", CategoryType.EXPENSE, user);
        transportCategory = createCategory("Transporte Summary", CategoryType.EXPENSE, user);

        createTransaction(
                user,
                salaryCategory,
                new BigDecimal("1000000"),
                TransactionType.INCOME,
                PERIOD_START);
        createTransaction(
                user,
                salaryCategory,
                new BigDecimal("500000"),
                TransactionType.INCOME,
                PERIOD_START);
        createTransaction(
                user,
                foodCategory,
                new BigDecimal("400000"),
                TransactionType.EXPENSE,
                PERIOD_START);
        createTransaction(
                user,
                transportCategory,
                new BigDecimal("100000"),
                TransactionType.EXPENSE,
                PERIOD_START);
    }

    @Test
    void returnsSplitCategoriesWithPercentagesAndSavingsRate() throws Exception {
        mockMvc.perform(
                        get("/summary")
                                .header("Authorization", "Bearer " + token)
                                .param("from", PERIOD_START.toString())
                                .param("to", PERIOD_END.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(1500000))
                .andExpect(jsonPath("$.totalExpense").value(500000))
                .andExpect(jsonPath("$.balance").value(1000000))
                .andExpect(jsonPath("$.savingsRate").value(66.6667))
                .andExpect(jsonPath("$.incomeByCategory.length()").value(1))
                .andExpect(
                        jsonPath("$.incomeByCategory[?(@.categoryName=='Salario Summary')].total")
                                .value(1500000))
                .andExpect(
                        jsonPath(
                                        "$.incomeByCategory[?(@.categoryName=='Salario Summary')].percentage")
                                .value(100.0))
                .andExpect(jsonPath("$.expenseByCategory.length()").value(2))
                .andExpect(
                        jsonPath(
                                        "$.expenseByCategory[?(@.categoryName=='Alimentación Summary')].total")
                                .value(400000))
                .andExpect(
                        jsonPath(
                                        "$.expenseByCategory[?(@.categoryName=='Alimentación Summary')].percentage")
                                .value(80.0))
                .andExpect(
                        jsonPath(
                                        "$.expenseByCategory[?(@.categoryName=='Transporte Summary')].total")
                                .value(100000))
                .andExpect(
                        jsonPath(
                                        "$.expenseByCategory[?(@.categoryName=='Transporte Summary')].percentage")
                                .value(20.0))
                .andExpect(jsonPath("$.byCategory").doesNotExist());
    }

    @Test
    void expenseByCategoryExcludesIncomeCategories() throws Exception {
        mockMvc.perform(
                        get("/summary")
                                .header("Authorization", "Bearer " + token)
                                .param("from", PERIOD_START.toString())
                                .param("to", PERIOD_END.toString()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.expenseByCategory[?(@.categoryName=='Salario Summary')]")
                                .isEmpty());
    }

    @Test
    void zeroIncomeReturnsNullSavingsRateWithoutError() throws Exception {
        User expenseOnlyUser = createUser("summary-no-income@test.com");
        String expenseOnlyToken = jwtService.generateToken(expenseOnlyUser.getEmail());
        Category expenseCategory =
                createCategory("Solo Gasto", CategoryType.EXPENSE, expenseOnlyUser);

        createTransaction(
                expenseOnlyUser,
                expenseCategory,
                new BigDecimal("250000"),
                TransactionType.EXPENSE,
                PERIOD_START);

        mockMvc.perform(
                        get("/summary")
                                .header("Authorization", "Bearer " + expenseOnlyToken)
                                .param("from", PERIOD_START.toString())
                                .param("to", PERIOD_END.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpense").value(250000))
                .andExpect(jsonPath("$.balance").value(-250000))
                .andExpect(jsonPath("$.savingsRate").isEmpty())
                .andExpect(jsonPath("$.incomeByCategory").isEmpty())
                .andExpect(jsonPath("$.expenseByCategory[0].percentage").value(100.0));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(
                        get("/summary")
                                .param("from", PERIOD_START.toString())
                                .param("to", PERIOD_END.toString()))
                .andExpect(status().isForbidden());
    }

    private void createTransaction(
            User user, Category category, BigDecimal amount, TransactionType type, LocalDate date) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setTransactionDate(date);
        transaction.setDescription("Transacción de prueba summary");
        transactionRepository.save(transaction);
    }

    private Category createCategory(String name, CategoryType type, User user) {
        Category category = new Category();
        category.setName(name);
        category.setType(type);
        category.setSystem(false);
        category.setUser(user);
        return categoryRepository.save(category);
    }

    private User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setName("Test User");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        return userRepository.save(user);
    }
}
