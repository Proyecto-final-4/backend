package com.backend.backend.domain.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.backend.domain.category.Category;
import com.backend.backend.domain.category.CategoryRepository;
import com.backend.backend.domain.category.CategoryType;
import com.backend.backend.domain.user.User;
import com.backend.backend.domain.user.UserRepository;
import com.backend.backend.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
        properties = {
            "app.env.validation.enabled=false",
            "app.jwt.secret-key=01234567890123456789012345678901",
            "app.encryption.key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        })
@AutoConfigureMockMvc
class TransactionDateFilterIntegrationTest {

    private static final LocalDate JANUARY_DATE = LocalDate.of(2026, 1, 15);
    private static final LocalDate MARCH_DATE = LocalDate.of(2026, 3, 15);
    private static final LocalDate MAY_DATE = LocalDate.of(2026, 5, 15);

    @Autowired private MockMvc mockMvc;

    @Autowired private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private UserRepository userRepository;

    @Autowired private CategoryRepository categoryRepository;

    @Autowired private TransactionRepository transactionRepository;

    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired
    private com.backend.backend.shared.crypto.EncryptionService encryptionService;

    private User testUser;
    private Category testCategory;
    private final List<UUID> transactionIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        String dateFilterEmail = "date-filter-" + UUID.randomUUID() + "@test.com";
        testUser = new User();
        testUser.setEmail(dateFilterEmail);
        testUser.setEmailHmac(encryptionService.hmac(dateFilterEmail));
        testUser.setName("Date Filter Test");
        testUser.setPasswordHash(passwordEncoder.encode("password"));
        testUser = userRepository.save(testUser);

        testCategory = new Category();
        testCategory.setName("Test Expense");
        testCategory.setType(CategoryType.EXPENSE);
        testCategory.setUser(testUser);
        testCategory.setSystem(false);
        testCategory = categoryRepository.save(testCategory);

        transactionIds.add(saveTransaction(JANUARY_DATE, "January expense"));
        transactionIds.add(saveTransaction(MARCH_DATE, "March expense"));
        transactionIds.add(saveTransaction(MAY_DATE, "May expense"));
    }

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAllById(transactionIds);
        transactionIds.clear();
        if (testCategory != null) {
            categoryRepository.delete(testCategory);
        }
        if (testUser != null) {
            userRepository.delete(testUser);
        }
    }

    @Test
    void getTransactions_withFromOnly_returnsTransactionsOnOrAfterFromDate() throws Exception {
        MvcResult result =
                mockMvc.perform(
                                get("/transactions")
                                        .param("from", "2026-03-01")
                                        .param("size", "50")
                                        .header(
                                                HttpHeaders.AUTHORIZATION,
                                                bearerToken(testUser.getEmail())))
                        .andExpect(status().isOk())
                        .andReturn();

        List<LocalDate> dates = extractTransactionDates(result);
        assertThat(dates).containsExactlyInAnyOrder(MARCH_DATE, MAY_DATE);
        assertThat(dates).doesNotContain(JANUARY_DATE);
    }

    @Test
    void getTransactions_withToOnly_returnsTransactionsOnOrBeforeToDate() throws Exception {
        MvcResult result =
                mockMvc.perform(
                                get("/transactions")
                                        .param("to", "2026-02-28")
                                        .param("size", "50")
                                        .header(
                                                HttpHeaders.AUTHORIZATION,
                                                bearerToken(testUser.getEmail())))
                        .andExpect(status().isOk())
                        .andReturn();

        List<LocalDate> dates = extractTransactionDates(result);
        assertThat(dates).containsExactly(JANUARY_DATE);
        assertThat(dates).doesNotContain(MARCH_DATE, MAY_DATE);
    }

    private UUID saveTransaction(LocalDate date, String description) {
        Transaction transaction = new Transaction();
        transaction.setUser(testUser);
        transaction.setCategory(testCategory);
        transaction.setAmount(BigDecimal.valueOf(100));
        transaction.setType(TransactionType.EXPENSE);
        transaction.setTransactionDate(date);
        transaction.setDescription(description);
        return transactionRepository.save(transaction).getId();
    }

    private String bearerToken(String email) {
        return "Bearer " + jwtService.generateToken(email);
    }

    private List<LocalDate> extractTransactionDates(MvcResult result) throws Exception {
        JsonNode content =
                objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
        List<LocalDate> dates = new ArrayList<>();
        for (JsonNode node : content) {
            dates.add(LocalDate.parse(node.get("transactionDate").asText()));
        }
        return dates;
    }
}
