package io.github.ahmedsh10.shiftwise.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Week 2: rest between days, weekly contract hours, absences, shift swaps, concurrent bookings. */
class Week2IntegrationTest extends IntegrationTestBase {

    // Demo employees and their skills
    private static final long LUCA = 5;     // WARDS, STAIRS            20h
    private static final long FATIMA = 6;   // WARDS, OFFICES           24h
    private static final long PAOLO = 7;    // STAIRS, CORRIDORS        20h
    private static final long CHIARA = 10;  // WARDS, OFFICES           20h
    private static final long ANDREA = 11;  // KITCHEN                  30h
    private static final long MARIA = 12;   // DISHWASH, CORRIDORS      20h
    private static final long ION = 13;     // WARDS, STAIRS, CORRIDORS 20h

    private static final long WARDS = 1, STAIRS = 2, DISHWASH = 4, CORRIDORS = 5, OFFICES = 6;
    private static final long MORNING = 2, AFTERNOON = 3, SPLIT = 4, EVENING = 5;

    @Test
    void eveningShiftIsNotFollowedByAMorningShiftNextDay() throws Exception {
        String sup = login(SUPERVISOR);

        assign(sup, FATIMA, OFFICES, EVENING, "2026-12-01").andExpect(status().isCreated()); // ends 21:30

        assign(sup, FATIMA, WARDS, MORNING, "2026-12-02")                                    // 08:00 -> 10h30 rest
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.violations[0].rule").value("INSUFFICIENT_REST"));

        assign(sup, FATIMA, WARDS, AFTERNOON, "2026-12-02").andExpect(status().isCreated());  // 13:00 is fine
    }

    @Test
    void weeklyContractHoursAreRespected() throws Exception {
        String sup = login(SUPERVISOR);

        // Luca has a 20h contract: five 4h mornings, Monday 7 to Friday 11 December
        for (int day = 7; day <= 11; day++) {
            assign(sup, LUCA, WARDS, MORNING, "2026-12-%02d".formatted(day)).andExpect(status().isCreated());
        }
        assign(sup, LUCA, WARDS, MORNING, "2026-12-12")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.violations[0].rule").value("WEEKLY_HOURS_EXCEEDED"));
    }

    @Test
    void absenceCancelsShiftsAndTheGapShowsOnTheCoverageBoard() throws Exception {
        String sup = login(SUPERVISOR);
        String date = "2026-12-14";

        assign(sup, PAOLO, STAIRS, MORNING, date).andExpect(status().isCreated());
        mvc.perform(get("/api/coverage").param("date", date).header("Authorization", sup))
                .andExpect(jsonPath("$[?(@.areaCode=='STAIRS' && @.shiftCode=='MORNING')].status").value(hasItem("GREEN")));

        mvc.perform(post("/api/absences").header("Authorization", sup).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":%d,\"startDate\":\"2026-12-14\",\"endDate\":\"2026-12-15\",\"reason\":\"sick\"}"
                                .formatted(PAOLO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.removedAssignments", hasSize(1)))
                .andExpect(jsonPath("$.removedAssignments[0].areaCode").value("STAIRS"));

        mvc.perform(get("/api/coverage").param("date", date).header("Authorization", sup))
                .andExpect(jsonPath("$[?(@.areaCode=='STAIRS' && @.shiftCode=='MORNING')].missing").value(hasItem(1)))
                .andExpect(jsonPath("$[?(@.areaCode=='STAIRS' && @.shiftCode=='MORNING')].status").value(hasItem("YELLOW")));

        assign(sup, PAOLO, CORRIDORS, MORNING, "2026-12-15")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.violations[0].rule").value("EMPLOYEE_ABSENT"));
    }

    @Test
    void swapGoesFromEmployeeToColleagueToSupervisor() throws Exception {
        String sup = login(SUPERVISOR);
        String chiara = login("chiara.rizzo@demo.dev");
        String ion = login("ion.popescu@demo.dev");

        long assignmentId = idOf(assign(sup, CHIARA, WARDS, MORNING, "2026-12-16").andExpect(status().isCreated()));

        long swapId = idOf(mvc.perform(post("/api/swaps").header("Authorization", chiara)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignmentId\":%d,\"targetEmployeeId\":%d}".formatted(assignmentId, ION)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_COLLEAGUE")));

        // A supervisor cannot approve before the colleague has accepted
        mvc.perform(post("/api/swaps/{id}/approve", swapId).header("Authorization", sup))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/swaps/{id}/accept", swapId).header("Authorization", ion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_SUPERVISOR"));

        mvc.perform(post("/api/swaps/{id}/approve", swapId).header("Authorization", sup))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.assignment.employeeId").value(ION));

        mvc.perform(get("/api/assignments").param("from", "2026-12-16").param("to", "2026-12-16")
                        .header("Authorization", sup))
                .andExpect(jsonPath("$[?(@.id==%d)].employeeId".formatted(assignmentId)).value(hasItem((int) ION)));
    }

    @Test
    void swapToAColleagueWhoIsNotTrainedForTheAreaIsRejected() throws Exception {
        String sup = login(SUPERVISOR);
        String chiara = login("chiara.rizzo@demo.dev");

        long assignmentId = idOf(assign(sup, CHIARA, WARDS, MORNING, "2026-12-17").andExpect(status().isCreated()));

        mvc.perform(post("/api/swaps").header("Authorization", chiara).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignmentId\":%d,\"targetEmployeeId\":%d}".formatted(assignmentId, ANDREA)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.violations[0].rule").value("NOT_QUALIFIED_FOR_AREA"));
    }

    @Test
    void employeeCannotOfferSomeoneElsesShift() throws Exception {
        String sup = login(SUPERVISOR);
        String ion = login("ion.popescu@demo.dev");

        long assignmentId = idOf(assign(sup, CHIARA, OFFICES, EVENING, "2026-12-17").andExpect(status().isCreated()));

        mvc.perform(post("/api/swaps").header("Authorization", ion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignmentId\":%d,\"targetEmployeeId\":%d}".formatted(assignmentId, ION)))
                .andExpect(status().isForbidden());
    }

    /**
     * The race from week 1: two supervisors book the same person at the same moment, on two
     * different shifts that overlap. The unique key cannot catch this; the employee row lock does.
     */
    @Test
    void concurrentOverlappingBookingsForOnePersonOnlyOneWins() throws Exception {
        String sup = login(SUPERVISOR);
        String date = "2026-12-18";
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Integer>> calls = List.of(
                    () -> { start.await(); return statusOf(assign(sup, MARIA, DISHWASH, AFTERNOON, date).andReturn()); },
                    () -> { start.await(); return statusOf(assign(sup, MARIA, CORRIDORS, SPLIT, date).andReturn()); });
            List<Future<Integer>> futures = new ArrayList<>();
            for (Callable<Integer> call : calls) {
                futures.add(pool.submit(call));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> f : futures) {
                statuses.add(f.get());
            }
            assertThat(statuses).containsExactlyInAnyOrder(201, 422);
        } finally {
            pool.shutdownNow();
        }

        mvc.perform(get("/api/assignments").param("from", date).param("to", date).header("Authorization", sup))
                .andExpect(jsonPath("$[?(@.employeeId==%d)]".formatted(MARIA), hasSize(1)));
    }

    private static int statusOf(MvcResult result) {
        return result.getResponse().getStatus();
    }
}
