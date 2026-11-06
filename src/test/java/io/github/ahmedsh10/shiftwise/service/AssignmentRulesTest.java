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

    private final Site site = new Site("Test site");
    private final Area kitchen = area(1L, "KITCHEN");
    private final Area wards = area(2L, "WARDS");
    private final ShiftTemplate morning = new ShiftTemplate("MORNING", "Morning",
            List.of(new ShiftSegment(LocalTime.of(8, 0), LocalTime.of(12, 0))));
    private final ShiftTemplate afternoon = new ShiftTemplate("AFTERNOON", "Afternoon",
            List.of(new ShiftSegment(LocalTime.of(13, 0), LocalTime.of(17, 0))));
    private final ShiftTemplate longMorning = new ShiftTemplate("LONG", "Long morning",
            List.of(new ShiftSegment(LocalTime.of(6, 30), LocalTime.of(12, 0))));

    private Area area(Long id, String code) {
        Area area = new Area(site, code, code);
        ReflectionTestUtils.setField(area, "id", id);
        return area;
    }

    private Employee cleaner(Area... skills) {
        Employee e = new Employee("Test Person", "t@demo.dev", "x", Role.EMPLOYEE, 20);
        e.setSkills(Set.of(skills));
        return e;
    }

    @Test
    void qualifiedFreeEmployeePassesAllRules() {
        Employee e = cleaner(kitchen);

        assertThat(AssignmentRules.check(e, kitchen, morning, MONDAY, List.of())).isEmpty();
    }

    @Test
    void employeeIsNotPlacedInAnAreaTheyAreNotTrainedFor() {
        Employee e = cleaner(kitchen);

        List<RuleViolation> violations = AssignmentRules.check(e, wards, morning, MONDAY, List.of());

        assertThat(violations).extracting(RuleViolation::rule)
                .containsExactly(RuleViolation.Rule.NOT_QUALIFIED_FOR_AREA);
    }

    @Test
    void overlappingShiftOnTheSameDayIsRejected() {
        Employee e = cleaner(kitchen, wards);
        Assignment existing = new Assignment(e, wards, longMorning, MONDAY);

        List<RuleViolation> violations = AssignmentRules.check(e, kitchen, morning, MONDAY, List.of(existing));

        assertThat(violations).extracting(RuleViolation::rule)
                .containsExactly(RuleViolation.Rule.OVERLAPPING_SHIFT);
    }

    @Test
    void morningAndAfternoonOnTheSameDayAreFine() {
        Employee e = cleaner(kitchen, wards);
        Assignment existing = new Assignment(e, wards, morning, MONDAY);

        assertThat(AssignmentRules.check(e, kitchen, afternoon, MONDAY, List.of(existing))).isEmpty();
    }

    @Test
    void inactiveEmployeeIsRejected() {
        Employee e = cleaner(kitchen);
        e.setActive(false);

        assertThat(AssignmentRules.check(e, kitchen, morning, MONDAY, List.of()))
                .extracting(RuleViolation::rule)
                .containsExactly(RuleViolation.Rule.EMPLOYEE_INACTIVE);
    }
}
