package io.github.ahmedsh10.shiftwise.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Week 3: replacement suggestions and weekly schedule generation. */
class Week3IntegrationTest extends IntegrationTestBase {

    private static final long LUCA = 5;     // WARDS, STAIRS            20h
    private static final long PAOLO = 7;    // STAIRS, CORRIDORS        20h
    private static final long ION = 13;     // WARDS, STAIRS, CORRIDORS 20h
    private static final long WARDS = 1, STAIRS = 2, MORNING = 2;

    /** Contract hours of the demo employees, and worked hours per shift code. */
    private static final Map<Long, Integer> CONTRACT = Map.ofEntries(
            Map.entry(4L, 20), Map.entry(5L, 20), Map.entry(6L, 24), Map.entry(7L, 20), Map.entry(8L, 30),
            Map.entry(9L, 20), Map.entry(10L, 20), Map.entry(11L, 30), Map.entry(12L, 20), Map.entry(13L, 20),
            Map.entry(14L, 24), Map.entry(15L, 20), Map.entry(16L, 20));
    private static final Map<String, Double> HOURS = Map.of(
            "SUPERVISOR", 9.5, "MORNING", 4.0, "AFTERNOON", 4.0, "SPLIT", 6.5, "EVENING", 4.0);

    @Test
    void afterAnAbsenceTheFairestAvailableReplacementComesFirst() throws Exception {
        String sup = login(SUPERVISOR);
        String day = "2027-01-06"; // Wednesday

        assign(sup, LUCA, WARDS, MORNING, "2027-01-04").andExpect(status().isCreated());   // Luca already has 4h this week
        assign(sup, ION, STAIRS, MORNING, day).andExpect(status().isCreated());
        mvc.perform(post("/api/absences").header("Authorization", sup).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":%d,\"startDate\":\"%s\",\"endDate\":\"%s\"}".formatted(ION, day, day)))
                .andExpect(status().isCreated());

        // Stairs-trained: Luca (4h used), Paolo (nothing yet), Ion (absent)
        mvc.perform(get("/api/suggestions").header("Authorization", sup)
                        .param("date", day).param("areaId", "" + STAIRS).param("shiftTemplateId", "" + MORNING))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missing").value(1))
                .andExpect(jsonPath("$.candidates", hasSize(2)))
                .andExpect(jsonPath("$.candidates[0].employeeId").value((int) PAOLO))
                .andExpect(jsonPath("$.candidates[1].employeeId").value((int) LUCA))
                .andExpect(jsonPath("$.candidates[1].plannedHoursThisWeek").value(4.0));

        mvc.perform(get("/api/suggestions").header("Authorization", sup)
                        .param("date", day).param("areaId", "" + STAIRS).param("shiftTemplateId", "" + MORNING)
                        .param("includeBlocked", "true"))
                .andExpect(jsonPath("$.candidates", hasSize(3)))
                .andExpect(jsonPath("$.candidates[2].employeeId").value((int) ION))
                .andExpect(jsonPath("$.candidates[2].eligible").value(false))
                .andExpect(jsonPath("$.candidates[2].blockedBy[0].rule").value("EMPLOYEE_ABSENT"));

        assign(sup, PAOLO, STAIRS, MORNING, day).andExpect(status().isCreated());
        mvc.perform(get("/api/coverage").param("date", day).header("Authorization", sup))
                .andExpect(jsonPath("$[?(@.areaCode=='STAIRS' && @.shiftCode=='MORNING')].status").value(hasItem("GREEN")));
    }

    @Test
    void dryRunPlansAWeekWithoutSavingAnything() throws Exception {
        String sup = login(SUPERVISOR);

        generate(sup, "2027-01-18", true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(true))
                .andExpect(jsonPath("$.createdCount", greaterThan(0)))
                .andExpect(jsonPath("$.created[0].id").value(nullValue()));

        mvc.perform(get("/api/assignments").param("from", "2027-01-18").param("to", "2027-01-24")
                        .header("Authorization", sup))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void generatedWeekRespectsContractsAndASecondRunAddsNothing() throws Exception {
        String sup = login(SUPERVISOR);

        // The demo team has fewer contract hours than the site needs, so some slots stay open
        generate(sup, "2027-01-11", false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount", greaterThan(0)))
                .andExpect(jsonPath("$.unfilledCount", greaterThan(0)));

        String body = mvc.perform(get("/api/assignments").param("from", "2027-01-11").param("to", "2027-01-17")
                        .header("Authorization", sup))
                .andReturn().getResponse().getContentAsString();
        Map<Long, Double> worked = new HashMap<>();
        for (JsonNode a : json.readTree(body)) {
            worked.merge(a.get("employeeId").asLong(), HOURS.get(a.get("shiftCode").asText()), Double::sum);
        }
        assertThat(worked).isNotEmpty();
        worked.forEach((employee, hours) ->
                assertThat(hours).as("hours for employee %d", employee).isLessThanOrEqualTo(CONTRACT.get(employee)));

        // Every slot left open had no eligible person, so running again changes nothing
        generate(sup, "2027-01-11", false).andExpect(jsonPath("$.createdCount").value(0));
    }

    @Test
    void weekStartMustBeAMonday() throws Exception {
        generate(login(SUPERVISOR), "2027-01-13", true).andExpect(status().isBadRequest());
    }

    private ResultActions generate(String token, String weekStart, boolean dryRun) throws Exception {
        return mvc.perform(post("/api/schedules/generate").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weekStart\":\"%s\",\"dryRun\":%s}".formatted(weekStart, dryRun)));
    }
}
