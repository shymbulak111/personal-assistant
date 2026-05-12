package kz.projem.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.projem.dto.request.LoginRequest;
import kz.projem.dto.request.RegisterRequest;
import kz.projem.dto.request.TaskRequest;
import kz.projem.dto.response.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
class TaskIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("assistant_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // disable kafka for integration tests
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9999");
        registry.add("spring.autoconfigure.exclude", () ->
                "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration");
        registry.add("spring.cache.type", () -> "none");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void authenticate() throws Exception {
        // register a fresh user for each test
        String email = "integration_" + System.currentTimeMillis() + "@test.com";

        RegisterRequest reg = new RegisterRequest();
        reg.setEmail(email);
        reg.setPassword("TestPass123");
        reg.setFirstName("Integration");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        accessToken = auth.getAccessToken();
    }

    @Test
    void createAndGetTask_fullFlow() throws Exception {
        TaskRequest req = new TaskRequest();
        req.setTitle("Integration test task");
        req.setDescription("This is a test");

        MvcResult create = mockMvc.perform(post("/api/v1/tasks")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Integration test task"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andReturn();

        // verify we can fetch it back
        String responseBody = create.getResponse().getContentAsString();
        Long taskId = objectMapper.readTree(responseBody).get("id").asLong();

        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId));
    }

    @Test
    void completeTask_changesStatus() throws Exception {
        TaskRequest req = new TaskRequest();
        req.setTitle("Task to complete");

        MvcResult create = mockMvc.perform(post("/api/v1/tasks")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long taskId = objectMapper.readTree(create.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/complete")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    void getTask_withoutAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isForbidden());
    }
}
