package com.backend.backend.domain.savingsgoal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.backend.domain.user.User;
import com.backend.backend.domain.user.UserRepository;
import com.backend.backend.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
        properties = {
            "app.env.validation.enabled=false",
            "app.jwt.secret-key=01234567890123456789012345678901"
        })
@AutoConfigureMockMvc
class SavingsGoalIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private MockMvc mockMvc;

    @Autowired private JwtService jwtService;

    @Autowired private UserRepository userRepository;

    @Autowired private SavingsGoalRepository savingsGoalRepository;

    @Autowired private PasswordEncoder passwordEncoder;

    private User userOne;
    private User userTwo;
    private final List<UUID> goalIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        userOne = saveUser("goals-user1-" + UUID.randomUUID() + "@test.com");
        userTwo = saveUser("goals-user2-" + UUID.randomUUID() + "@test.com");
    }

    @AfterEach
    void tearDown() {
        savingsGoalRepository.deleteAllById(goalIds);
        goalIds.clear();
        if (userOne != null) {
            userRepository.delete(userOne);
        }
        if (userTwo != null) {
            userRepository.delete(userTwo);
        }
    }

    @Test
    void createUpdateProgressAndEnforceUserIsolation() throws Exception {
        String createBody =
                """
                {
                  "name": "Vacaciones Europa",
                  "description": "Viaje familiar",
                  "targetAmount": 5000000,
                  "targetDate": "2026-12-31"
                }
                """;

        MvcResult createResult =
                mockMvc.perform(
                                post("/goals")
                                        .header(
                                                HttpHeaders.AUTHORIZATION,
                                                bearerToken(userOne.getEmail()))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(createBody))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.name").value("Vacaciones Europa"))
                        .andExpect(jsonPath("$.currentAmount").value(0))
                        .andExpect(jsonPath("$.isCompleted").value(false))
                        .andReturn();

        String goalId = readJson(createResult).get("id").asText();
        goalIds.add(UUID.fromString(goalId));

        mockMvc.perform(
                        get("/goals")
                                .header(HttpHeaders.AUTHORIZATION, bearerToken(userOne.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(goalId))
                .andExpect(jsonPath("$[0].currentAmount").value(0));

        String updateBody =
                """
                {
                  "currentAmount": 1500000
                }
                """;

        mockMvc.perform(
                        put("/goals/" + goalId)
                                .header(HttpHeaders.AUTHORIZATION, bearerToken(userOne.getEmail()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount").value(1500000));

        mockMvc.perform(
                        get("/goals")
                                .header(HttpHeaders.AUTHORIZATION, bearerToken(userOne.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currentAmount").value(1500000));

        mockMvc.perform(
                        put("/goals/" + goalId)
                                .header(HttpHeaders.AUTHORIZATION, bearerToken(userTwo.getEmail()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Savings goal not found"));

        mockMvc.perform(
                        delete("/goals/" + goalId)
                                .header(HttpHeaders.AUTHORIZATION, bearerToken(userTwo.getEmail())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Savings goal not found"));

        mockMvc.perform(
                        get("/goals")
                                .header(HttpHeaders.AUTHORIZATION, bearerToken(userTwo.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void createGoalWithoutTargetDate() throws Exception {
        String createBody =
                """
                {
                  "name": "Fondo emergencia",
                  "targetAmount": 1000000
                }
                """;

        MvcResult createResult =
                mockMvc.perform(
                                post("/goals")
                                        .header(
                                                HttpHeaders.AUTHORIZATION,
                                                bearerToken(userOne.getEmail()))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(createBody))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.targetDate").isEmpty())
                        .andExpect(jsonPath("$.currentAmount").value(0))
                        .andReturn();

        goalIds.add(UUID.fromString(readJson(createResult).get("id").asText()));
    }

    @Test
    void markGoalAsCompleted() throws Exception {
        MvcResult createResult =
                mockMvc.perform(
                                post("/goals")
                                        .header(
                                                HttpHeaders.AUTHORIZATION,
                                                bearerToken(userOne.getEmail()))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Meta completada",
                                                  "targetAmount": 100
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andReturn();

        String goalId = readJson(createResult).get("id").asText();
        goalIds.add(UUID.fromString(goalId));

        mockMvc.perform(
                        put("/goals/" + goalId)
                                .header(HttpHeaders.AUTHORIZATION, bearerToken(userOne.getEmail()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "isCompleted": true
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(true));
    }

    private User saveUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setName("Goals Test User");
        user.setPasswordHash(passwordEncoder.encode("password"));
        return userRepository.save(user);
    }

    private String bearerToken(String email) {
        return "Bearer " + jwtService.generateToken(email);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
