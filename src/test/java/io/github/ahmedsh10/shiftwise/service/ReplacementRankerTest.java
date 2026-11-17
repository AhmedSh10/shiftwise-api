package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.domain.Absence;
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

class ReplacementRankerTest {

    private static final LocalDate MONDAY = LocalDate.of(2027, 1, 4);
    private static final LocalDate WEDNESDAY = MONDAY.plusDays(2);

    private final Site site = new Site("Test site");
    private final Area stairs = area(1L, "STAIRS");
    private final Area kitchen = area(2L, "KITCHEN");
    private final ShiftTemplate morning = new ShiftTemplate("MORNING", "Morning",
            List.of(new ShiftSegment(LocalTime.of(8, 0), LocalTime.of(12, 0))));

    private Area area(Long id, String code) {
        Area a = new Area(site, code, code);
        ReflectionTestUtils.setField(a, "id", id);
        return a;
    }

    private Employee person(long id, String name, int contractHours, Area... skills) {
        Employee e = new Employee(name, name + "@demo.dev", "x", Role.EMPLOYEE, contractHours);
        ReflectionTestUtils.setField(e, "id", id);
        e.setSkills(Set.of(skills));
        return e;
    }

    @Test
    void ranksByShareOfContractUsedThenListsBlockedPeopleLast() {
        Employee busy = person(1, "busy", 20, stairs);        // 8h of 20 = 40% used
        Employee lighter = person(2, "lighter", 30, stairs);  // 8h of 30 = 27% used
        Employee absent = person(3, "absent", 20, stairs);
        Employee untrained = person(4, "untrained", 20, kitchen);

        ScheduleSnapshot snapshot = new ScheduleSnapshot(
                List.of(new Assignment(busy, stairs, morning, MONDAY), new Assignment(busy, stairs, morning, MONDAY.plusDays(1)),
                        new Assignment(lighter, stairs, morning, MONDAY), new Assignment(lighter, stairs, morning, MONDAY.plusDays(1))),
                List.of(new Absence(absent, WEDNESDAY, WEDNESDAY, "sick")));

        List<Candidate> ranked = ReplacementRanker.rank(List.of(busy, lighter, absent, untrained),
                stairs, morning, WEDNESDAY, snapshot, RuleSettings.DEFAULTS);

        assertThat(ranked).extracting(c -> c.employee().getFullName()).containsExactly("lighter", "busy", "absent");
        assertThat(ranked.get(2).eligible()).isFalse();
        assertThat(ranked.get(2).violations()).extracting(RuleViolation::rule)
                .containsExactly(RuleViolation.Rule.EMPLOYEE_ABSENT);
    }

    @Test
    void sameShareOfContractFallsBackToFewestHours() {
        Employee fresh = person(7, "fresh", 20, stairs);
        Employee alsoFresh = person(5, "alsoFresh", 20, stairs);

        List<Candidate> ranked = ReplacementRanker.rank(List.of(fresh, alsoFresh), stairs, morning, WEDNESDAY,
                new ScheduleSnapshot(List.of(), List.of()), RuleSettings.DEFAULTS);

        // Nothing planned for either: stable order by id
        assertThat(ranked).extracting(c -> c.employee().getId()).containsExactly(5L, 7L);
    }

    @Test
    void plannedShiftsInTheSnapshotCountForLaterDecisions() {
        Employee only = person(1, "only", 8, stairs);   // room for two 4h mornings
        ScheduleSnapshot snapshot = new ScheduleSnapshot(List.of(), List.of());

        snapshot.add(new Assignment(only, stairs, morning, MONDAY));
        snapshot.add(new Assignment(only, stairs, morning, MONDAY.plusDays(1)));

        Candidate c = ReplacementRanker.rank(List.of(only), stairs, morning, WEDNESDAY, snapshot, RuleSettings.DEFAULTS).get(0);
        assertThat(c.violations()).extracting(RuleViolation::rule).containsExactly(RuleViolation.Rule.WEEKLY_HOURS_EXCEEDED);
    }
}
