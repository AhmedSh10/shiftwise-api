package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.domain.Area;
import io.github.ahmedsh10.shiftwise.domain.Assignment;
import io.github.ahmedsh10.shiftwise.domain.Employee;
import io.github.ahmedsh10.shiftwise.domain.ShiftTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Who should cover this shift? Runs every scheduling rule for every trained person, then ranks
 * the ones who pass so work is spread fairly: lowest share of their contract used this week first,
 * then fewest hours, then id for a stable order. Pure Java, like {@link AssignmentRules}.
 */
public final class ReplacementRanker {

    static final Comparator<Candidate> FAIREST_FIRST = Comparator
            .comparingDouble(Candidate::utilisation)
            .thenComparing(Candidate::plannedThisWeek)
            .thenComparing(c -> c.employee().getId());

    private ReplacementRanker() {
    }

    /**
     * @return eligible candidates first (fairest first), then the trained people who are blocked, with their reasons.
     *         People not trained for the area are left out entirely.
     */
    public static List<Candidate> rank(Collection<Employee> employees, Area area, ShiftTemplate shift, LocalDate date,
                                       ScheduleSnapshot snapshot, RuleSettings settings) {
        List<Candidate> all = employees.stream()
                .filter(e -> e.canWorkIn(area))
                .map(e -> {
                    ScheduleContext context = snapshot.contextFor(e.getId(), date);
                    Duration planned = context.week().stream()
                            .map(Assignment::getShiftTemplate)
                            .map(ShiftTemplate::workedTime)
                            .reduce(Duration.ZERO, Duration::plus);
                    return new Candidate(e, planned, AssignmentRules.check(e, area, shift, date, context, settings));
                })
                .toList();

        return all.stream()
                .sorted(Comparator.comparing((Candidate c) -> !c.eligible()).thenComparing(FAIREST_FIRST))
                .toList();
    }
}
