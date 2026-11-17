package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.domain.Employee;

import java.time.Duration;
import java.util.List;

/**
 * One possible person for a gap.
 *
 * @param plannedThisWeek hours already scheduled in that ISO week, before this shift
 * @param violations      empty when the person can take the shift
 */
public record Candidate(Employee employee, Duration plannedThisWeek, List<RuleViolation> violations) {

    public boolean eligible() {
        return violations.isEmpty();
    }

    /** Share of the weekly contract already used: 0.0 = nothing planned, 1.0 = full. */
    public double utilisation() {
        return plannedThisWeek.toMinutes() / (employee.getContractHoursPerWeek() * 60.0);
    }
}
