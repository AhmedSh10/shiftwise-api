package io.github.ahmedsh10.shiftwise.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared setup for API tests: one PostgreSQL container for the whole test run (started once,
 * so every test class reuses the same Spring context), with the Flyway demo data.
 *
 * Demo data: areas 1 WARDS, 2 STAIRS, 3 KITCHEN, 4 DISHWASH, 5 CORRIDORS, 6 OFFICES;
 * shifts 1 SUPERVISOR, 2 MORNING, 3 AFTERNOON, 4 SPLIT, 5 EVENING. Password for every account: demo1234.
 * Each test uses its own dates and people so tests do not depend on each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    static final String PASSWORD = "demo1234";
    static final String SUPERVISOR = "giulia.ferri@demo.dev";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    ResultActions assign(String token, long employeeId, long areaId, long shiftId, String date) throws Exception {
        String body = "{\"employeeId\":%d,\"areaId\":%d,\"shiftTemplateId\":%d,\"date\":\"%s\"}"
                .formatted(employeeId, areaId, shiftId, date);
        return mvc.perform(post("/api/assignments")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    long idOf(ResultActions result) throws Exception {
        JsonNode node = json.readTree(result.andReturn().getResponse().getContentAsString());
        return node.get("id").asLong();
    }
}
