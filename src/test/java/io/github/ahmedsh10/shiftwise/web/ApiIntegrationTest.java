package io.github.ahmedsh10.shiftwise.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Week 1: authentication, roles, skill and overlap rules, coverage board. */
class ApiIntegrationTest extends IntegrationTestBase {


    private static final String EMPLOYEE = "sara.conti@demo.dev";

    // Demo employees and their skills
    private static final long SARA = 4;     // WARDS, CORRIDORS
    private static final long FATIMA = 6;   // WARDS, OFFICES
    private static final long ELENA = 8;    // KITCHEN, DISHWASH

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
