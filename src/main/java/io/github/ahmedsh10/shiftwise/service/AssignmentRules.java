package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.domain.Area;
import io.github.ahmedsh10.shiftwise.domain.Assignment;
import io.github.ahmedsh10.shiftwise.domain.Employee;
import io.github.ahmedsh10.shiftwise.domain.ShiftTemplate;
import io.github.ahmedsh10.shiftwise.service.RuleViolation.Rule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure scheduling rules, kept free of Spring and the database so they are easy to test
 * and to reuse later for replacement suggestions.
 */
public final class AssignmentRules {

    private AssignmentRules() {
    }

    /**
     * @param sameDay the employee's existing assignments on {@code date}
     */
    public static List<RuleViolation> check(Employee employee, Area area, ShiftTemplate shift,
                                            LocalDate date, List<Assignment> sameDay) {
        List<RuleViolation> violations = new ArrayList<>();

        if (!employee.isActive()) {
            violations.add(new RuleViolation(Rule.EMPLOYEE_INACTIVE,
                    employee.getFullName() + " is not active"));
        }

        if (!employee.canWorkIn(area)) {
            violations.add(new RuleViolation(Rule.NOT_QUALIFIED_FOR_AREA,
                    employee.getFullName() + " is not trained for " + area.getName()));
        }

        for (Assignment existing : sameDay) {
            if (existing.getShiftTemplate().overlaps(shift)) {
                violations.add(new RuleViolation(Rule.OVERLAPPING_SHIFT,
                        employee.getFullName() + " already works " + existing.getShiftTemplate().getCode()
                                + " in " + existing.getArea().getName() + " on " + date));
                break;
            }
        }

        return violations;
    }
}
