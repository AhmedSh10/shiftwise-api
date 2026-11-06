package io.github.ahmedsh10.shiftwise.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs against a real PostgreSQL in Docker, with the Flyway demo data
 * (areas 1 WARDS, 3 KITCHEN, 4 DISHWASH, 6 OFFICES; shifts 2 MORNING, 3 AFTERNOON, 4 SPLIT, 5 EVENING).
 * Each test uses its own date so tests do not depend on each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String SUPERVISOR = "giulia.ferri@demo.dev";
    private static final String EMPLOYEE = "sara.conti@demo.dev";
    private static final String PASSWORD = "demo1234";

    // Demo employees and their skills
    private static final long SARA = 4;     // WARDS, CORRIDORS
    private static final long FATIMA = 6;   // WARDS, OFFICES
    private static final long ELENA = 8;    // KITCHEN, DISHWASH

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        return "Bearer " + node.get("token").asText();
    }

    private ResultActions assign(String token, long employeeId, long areaId, long shiftId, String date) throws Exception {
        String body = "{\"employeeId\":%d,\"areaId\":%d,\"shiftTemplateId\":%d,\"date\":\"%s\"}"
                .formatted(employeeId, areaId, shiftId, date);
        return mvc.perform(post("/api/assignments")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + SUPERVISOR + "\",\"password\":\"nope-nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        mvc.perform(get("/api/areas")).andExpect(status().isUnauthorized());
    }

    @Test
    void employeeCanSeeOwnProfileButNotTheStaffList() throws Exception {
        String token = login(EMPLOYEE);

        mvc.perform(get("/api/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMPLOYEE))
                .andExpect(jsonPath("$.skills", hasItem("WARDS")));

        mvc.perform(get("/api/employees").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void splitShiftIsListedWithItsTwoSegments() throws Exception {
        String token = login(EMPLOYEE);

        mvc.perform(get("/api/shift-templates").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code=='SPLIT')].segments[*].start").value(hasItems("13:00:00", "17:30:00")))
                .andExpect(jsonPath("$[?(@.code=='SPLIT')].hours").value(hasItem(6.5)));
    }

    @Test
    void supervisorAssignsAndCoverageShowsWhatIsMissing() throws Exception {
        String token = login(SUPERVISOR);
        String date = "2026-11-09";

        assign(token, SARA, 1, 2, date).andExpect(status().isCreated())
                .andExpect(jsonPath("$.areaCode").value("WARDS"))
                .andExpect(jsonPath("$.shiftCode").value("MORNING"));

        // Wards need 4 in the morning: 1 assigned, 3 missing -> RED
        mvc.perform(get("/api/coverage").param("date", date).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.areaCode=='WARDS' && @.shiftCode=='MORNING')].assigned").value(hasItem(1)))
                .andExpect(jsonPath("$[?(@.areaCode=='WARDS' && @.shiftCode=='MORNING')].missing").value(hasItem(3)))
                .andExpect(jsonPath("$[?(@.areaCode=='WARDS' && @.shiftCode=='MORNING')].status").value(hasItem("RED")));
    }

    @Test
    void employeeIsNotPlacedInAnAreaTheyAreNotTrainedFor() throws Exception {
        String token = login(SUPERVISOR);

        // Sara works wards and corridors, not the kitchen
        assign(token, SARA, 3, 2, "2026-11-10")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.violations[0].rule").value("NOT_QUALIFIED_FOR_AREA"));
    }

    @Test
    void overlappingShiftsOnTheSameDayAreRejected() throws Exception {
        String token = login(SUPERVISOR);
        String date = "2026-11-11";

        assign(token, ELENA, 3, 4, date).andExpect(status().isCreated());          // kitchen, split 13:00-15:30 + 17:30-21:30
        assign(token, ELENA, 4, 3, date)                                            // dishwash, afternoon 13:00-17:00
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.violations[0].rule").value("OVERLAPPING_SHIFT"));
    }

    @Test
    void afternoonThenEveningOnTheSameDayIsAllowed() throws Exception {
        String token = login(SUPERVISOR);
        String date = "2026-11-12";

        assign(token, FATIMA, 1, 3, date).andExpect(status().isCreated());   // wards 13:00-17:00
        assign(token, FATIMA, 6, 5, date).andExpect(status().isCreated());   // offices 17:30-21:30
    }

    @Test
    void employeeCannotCreateAssignments() throws Exception {
        String token = login(EMPLOYEE);

        assign(token, SARA, 1, 2, "2026-11-13").andExpect(status().isForbidden());
    }
}
