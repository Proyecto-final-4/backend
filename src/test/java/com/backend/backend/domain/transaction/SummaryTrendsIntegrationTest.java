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
            "app.jwt.secret-key=01234567890123456789012345678901",
            "app.encryption.key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        })
@AutoConfigureMockMvc
@Transactional
class SummaryTrendsIntegrationTest {

    private static final LocalDate APRIL_START = LocalDate.of(2026, 4, 1);
    private static final LocalDate APRIL_END = LocalDate.of(2026, 4, 30);
    private static final LocalDate MAY_START = LocalDate.of(2026, 5, 1);
    private static final LocalDate MAY_END = LocalDate.of(2026, 5, 31);

    @Autowired private MockMvc mockMvc;

    @Autowired private JwtService jwtService;

    @Autowired private UserRepository userRepository;

    @Autowired private CategoryRepository categoryRepository;

    @Autowired private TransactionRepository transactionRepository;

    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private com.backend.backend.shared.crypto.EncryptionService encryptionService;

    private User userOne;
    private User userTwo;
    private String tokenOne;
    private String tokenTwo;
    private Category incomeCategory;
    private Category foodCategory;
    private Category transportCategory;

    @BeforeEach
    void setUp() {
        userOne = createUser("trends-user1@test.com");
        userTwo = createUser("trends-user2@test.com");
        tokenOne = jwtService.generateToken(userOne.getEmail());
        tokenTwo = jwtService.generateToken(userTwo.getEmail());

        incomeCategory = createCategory("Salario Trends", CategoryType.INCOME, userOne);
        foodCategory = createCategory("Alimentación Trends", CategoryType.EXPENSE, userOne);
        transportCategory = createCategory("Transporte Trends", CategoryType.EXPENSE, userOne);

        createTransaction(
                userOne,
                incomeCategory,
                new BigDecimal("1000000"),
                TransactionType.INCOME,
                APRIL_START);
        createTransaction(
                userOne,
                foodCategory,
                new BigDecimal("300000"),
                TransactionType.EXPENSE,
                APRIL_START);

        createTransaction(
                userOne,
                incomeCategory,
                new BigDecimal("1200000"),
                TransactionType.INCOME,
                MAY_START);
        createTransaction(
                userOne,
                foodCategory,
                new BigDecimal("400000"),
                TransactionType.EXPENSE,
                MAY_START);
        createTransaction(
                userOne,
                transportCategory,
                new BigDecimal("100000"),
                TransactionType.EXPENSE,
                MAY_START);
    }

    @Test
    void comparesTwoPeriodsWithCorrectTotalsAndDiffs() throws Exception {
        mockMvc.perform(
                        get("/summary/trends")
                                .header("Authorization", "Bearer " + tokenOne)
                                .param("currentFrom", MAY_START.toString())
                                .param("currentTo", MAY_END.toString())
                                .param("previousFrom", APRIL_START.toString())
                                .param("previousTo", APRIL_END.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current.totalIncome").value(1200000))
                .andExpect(jsonPath("$.current.totalExpense").value(500000))
                .andExpect(jsonPath("$.current.balance").value(700000))
                .andExpect(jsonPath("$.previous.totalIncome").value(1000000))
                .andExpect(jsonPath("$.previous.totalExpense").value(300000))
                .andExpect(jsonPath("$.previous.balance").value(700000))
                .andExpect(jsonPath("$.diff.incomeChange.absolute").value(200000))
                .andExpect(jsonPath("$.diff.incomeChange.percentage").value(20.0))
                .andExpect(jsonPath("$.diff.expenseChange.absolute").value(200000))
                .andExpect(jsonPath("$.diff.expenseChange.percentage").value(66.6667))
                .andExpect(jsonPath("$.diff.balanceChange.absolute").value(0))
                .andExpect(jsonPath("$.diff.balanceChange.percentage").value(0.0));
    }

    @Test
    void categoryOnlyInCurrentPeriodHasZeroPreviousTotal() throws Exception {
        mockMvc.perform(
                        get("/summary/trends")
                                .header("Authorization", "Bearer " + tokenOne)
                                .param("currentFrom", MAY_START.toString())
                                .param("currentTo", MAY_END.toString())
                                .param("previousFrom", APRIL_START.toString())
                                .param("previousTo", APRIL_END.toString()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                        "$.diff.byCategory[?(@.categoryName=='Transporte Trends')].previousTotal")
                                .value(0))
                .andExpect(
                        jsonPath(
                                        "$.diff.byCategory[?(@.categoryName=='Transporte Trends')].currentTotal")
                                .value(100000))
                .andExpect(
                        jsonPath("$.diff.byCategory[?(@.categoryName=='Transporte Trends')].change")
                                .value(100000));
    }

    @Test
    void categoryOnlyInPreviousPeriodHasZeroCurrentTotal() throws Exception {
        Category leisureCategory = createCategory("Ocio Trends", CategoryType.EXPENSE, userOne);
        createTransaction(
                userOne,
                leisureCategory,
                new BigDecimal("50000"),
                TransactionType.EXPENSE,
                APRIL_START);

        mockMvc.perform(
                        get("/summary/trends")
                                .header("Authorization", "Bearer " + tokenOne)
                                .param("currentFrom", MAY_START.toString())
                                .param("currentTo", MAY_END.toString())
                                .param("previousFrom", APRIL_START.toString())
                                .param("previousTo", APRIL_END.toString()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.diff.byCategory[?(@.categoryName=='Ocio Trends')].currentTotal")
                                .value(0))
                .andExpect(
                        jsonPath(
                                        "$.diff.byCategory[?(@.categoryName=='Ocio Trends')].previousTotal")
                                .value(50000))
                .andExpect(
                        jsonPath("$.diff.byCategory[?(@.categoryName=='Ocio Trends')].change")
                                .value(-50000));
    }

    @Test
    void emptyPeriodReturnsZerosWithoutError() throws Exception {
        LocalDate emptyStart = LocalDate.of(2026, 1, 1);
        LocalDate emptyEnd = LocalDate.of(2026, 1, 31);

        mockMvc.perform(
                        get("/summary/trends")
                                .header("Authorization", "Bearer " + tokenOne)
                                .param("currentFrom", MAY_START.toString())
                                .param("currentTo", MAY_END.toString())
                                .param("previousFrom", emptyStart.toString())
                                .param("previousTo", emptyEnd.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previous.totalIncome").value(0))
                .andExpect(jsonPath("$.previous.totalExpense").value(0))
                .andExpect(jsonPath("$.previous.balance").value(0))
                .andExpect(jsonPath("$.diff.incomeChange.absolute").value(1200000));
    }

    @Test
    void userCannotSeeAnotherUsersTrends() throws Exception {
        createTransaction(
                userTwo,
                createCategory("Gasto Ajeno", CategoryType.EXPENSE, userTwo),
                new BigDecimal("999999"),
                TransactionType.EXPENSE,
                MAY_START);

        mockMvc.perform(
                        get("/summary/trends")
                                .header("Authorization", "Bearer " + tokenTwo)
                                .param("currentFrom", MAY_START.toString())
                                .param("currentTo", MAY_END.toString())
                                .param("previousFrom", APRIL_START.toString())
                                .param("previousTo", APRIL_END.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current.totalExpense").value(999999))
                .andExpect(jsonPath("$.previous.totalExpense").value(0));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(
                        get("/summary/trends")
                                .param("currentFrom", MAY_START.toString())
                                .param("currentTo", MAY_END.toString())
                                .param("previousFrom", APRIL_START.toString())
                                .param("previousTo", APRIL_END.toString()))
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
        transaction.setDescription("Transacción de prueba trends");
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
        user.setEmailHmac(encryptionService.hmac(email));
        user.setName("Test User");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        return userRepository.save(user);
    }
}
