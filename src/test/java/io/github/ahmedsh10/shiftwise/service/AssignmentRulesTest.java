package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.domain.Area;
import io.github.ahmedsh10.shiftwise.domain.Assignment;
import io.github.ahmedsh10.shiftwise.domain.Employee;
import io.github.ahmedsh10.shiftwise.domain.Role;
import io.github.ahmedsh10.shiftwise.domain.ShiftSegment;
import io.github.ahmedsh10.shiftwise.domain.ShiftTemplate;
import io.github.ahmedsh10.shiftwise.domain.Site;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AssignmentRulesTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 9);
    private static final LocalDate TUESDAY = MONDAY.plusDays(1);
    private static final RuleSettings ELEVEN_HOURS = RuleSettings.DEFAULTS;

    private final Site site = new Site("Test site");
    private final Area kitchen = area(1L, "KITCHEN");
    private final Area wards = area(2L, "WARDS");
    private final ShiftTemplate morning = shift("MORNING", "08:00", "12:00");
    private final ShiftTemplate afternoon = shift("AFTERNOON", "13:00", "17:00");
    private final ShiftTemplate longMorning = shift("LONG", "06:30", "12:00");
    private final ShiftTemplate early = shift("EARLY", "06:30", "10:30");
    private final ShiftTemplate evening = shift("EVENING", "17:30", "21:30");
    private final ShiftTemplate split = shift("SPLIT", "13:00", "15:30", "17:30", "21:30");

    private static ShiftTemplate shift(String code, String... times) {
        List<ShiftSegment> segments = new java.util.ArrayList<>();
        for (int i = 0; i < times.length; i += 2) {
            segments.add(new ShiftSegment(LocalTime.parse(times[i]), LocalTime.parse(times[i + 1])));
        }
        return new ShiftTemplate(code, code, segments);
    }

    private Area area(Long id, String code) {
        Area area = new Area(site, code, code);
        ReflectionTestUtils.setField(area, "id", id);
        return area;
    }

    private Employee cleaner(int contractHours, Area... skills) {
        Employee e = new Employee("Test Person", "t@demo.dev", "x", Role.EMPLOYEE, contractHours);
        e.setSkills(Set.of(skills));
        return e;
    }

    /** Context where the given assignments are both "nearby" and in the same week. */
    private static ScheduleContext with(Assignment... existing) {
        return new ScheduleContext(List.of(existing), List.of(existing), false);
    }

    private static List<RuleViolation.Rule> rules(List<RuleViolation> violations) {
        return violations.stream().map(RuleViolation::rule).toList();
    }

    @Test
    void qualifiedFreeEmployeePassesAllRules() {
        Employee e = cleaner(20, kitchen);

        assertThat(AssignmentRules.check(e, kitchen, morning, MONDAY, ScheduleContext.empty(), ELEVEN_HOURS)).isEmpty();
    }

    @Test
    void employeeIsNotPlacedInAnAreaTheyAreNotTrainedFor() {
        Employee e = cleaner(20, kitchen);

        assertThat(rules(AssignmentRules.check(e, wards, morning, MONDAY, ScheduleContext.empty(), ELEVEN_HOURS)))
                .containsExactly(RuleViolation.Rule.NOT_QUALIFIED_FOR_AREA);
    }

    @Test
    void overlappingShiftOnTheSameDayIsRejected() {
        Employee e = cleaner(20, kitchen, wards);
        Assignment existing = new Assignment(e, wards, longMorning, MONDAY);

        assertThat(rules(AssignmentRules.check(e, kitchen, morning, MONDAY, with(existing), ELEVEN_HOURS)))
                .containsExactly(RuleViolation.Rule.OVERLAPPING_SHIFT);
    }

    @Test
    void morningAndAfternoonOnTheSameDayAreFine() {
        Employee e = cleaner(20, kitchen, wards);
        Assignment existing = new Assignment(e, wards, morning, MONDAY);

        assertThat(AssignmentRules.check(e, kitchen, afternoon, MONDAY, with(existing), ELEVEN_HOURS)).isEmpty();
    }

    @Test
    void inactiveEmployeeIsRejected() {
        Employee e = cleaner(20, kitchen);
        e.setActive(false);

        assertThat(rules(AssignmentRules.check(e, kitchen, morning, MONDAY, ScheduleContext.empty(), ELEVEN_HOURS)))
                .containsExactly(RuleViolation.Rule.EMPLOYEE_INACTIVE);
    }

    @Test
    void absentEmployeeIsRejected() {
        Employee e = cleaner(20, kitchen);
        ScheduleContext absent = new ScheduleContext(List.of(), List.of(), true);

        assertThat(rules(AssignmentRules.check(e, kitchen, morning, MONDAY, absent, ELEVEN_HOURS)))
                .containsExactly(RuleViolation.Rule.EMPLOYEE_ABSENT);
    }

    @Test
    void eveningThenMorningNextDayIsOnlyTenAndAHalfHoursOfRest() {
        Employee e = cleaner(20, kitchen, wards);
        Assignment monEvening = new Assignment(e, wards, evening, MONDAY);   // ends 21:30

        List<RuleViolation> violations = AssignmentRules.check(e, kitchen, morning, TUESDAY, with(monEvening), ELEVEN_HOURS);

        assertThat(rules(violations)).containsExactly(RuleViolation.Rule.INSUFFICIENT_REST);
        assertThat(violations.get(0).message()).contains("10h30");
    }

    @Test
    void restIsAlsoCheckedAgainstTheNextDay() {
        Employee e = cleaner(20, kitchen, wards);
        Assignment tueEarly = new Assignment(e, wards, early, TUESDAY);      // starts 06:30

        // Split shift on Monday ends 21:30: only 9h before Tuesday 06:30
        assertThat(rules(AssignmentRules.check(e, kitchen, split, MONDAY, with(tueEarly), ELEVEN_HOURS)))
                .containsExactly(RuleViolation.Rule.INSUFFICIENT_REST);
    }

    @Test
    void eveningThenAfternoonNextDayHasEnoughRest() {
        Employee e = cleaner(20, kitchen, wards);
        Assignment monEvening = new Assignment(e, wards, evening, MONDAY);

        assertThat(AssignmentRules.check(e, kitchen, afternoon, TUESDAY, with(monEvening), ELEVEN_HOURS)).isEmpty();
    }

    @Test
    void restRuleFollowsTheConfiguredMinimum() {
        Employee e = cleaner(20, kitchen, wards);
        Assignment monEvening = new Assignment(e, wards, evening, MONDAY);

        assertThat(AssignmentRules.check(e, kitchen, morning, TUESDAY, with(monEvening), new RuleSettings(10))).isEmpty();
    }

    @Test
    void weeklyContractHoursCannotBeExceeded() {
        Employee e = cleaner(8, kitchen, wards);
        Assignment monMorning = new Assignment(e, wards, morning, MONDAY);
        Assignment tueMorning = new Assignment(e, wards, morning, TUESDAY);
        ScheduleContext week = new ScheduleContext(List.of(), List.of(monMorning, tueMorning), false);

        assertThat(rules(AssignmentRules.check(e, kitchen, morning, MONDAY.plusDays(3), week, ELEVEN_HOURS)))
                .containsExactly(RuleViolation.Rule.WEEKLY_HOURS_EXCEEDED);
    }

    @Test
    void reachingContractHoursExactlyIsAllowed() {
        Employee e = cleaner(8, kitchen, wards);
        Assignment monMorning = new Assignment(e, wards, morning, MONDAY);
        ScheduleContext week = new ScheduleContext(List.of(), List.of(monMorning), false);

        assertThat(AssignmentRules.check(e, kitchen, morning, MONDAY.plusDays(3), week, ELEVEN_HOURS)).isEmpty();
    }
}
