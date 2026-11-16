package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.domain.Area;
import io.github.ahmedsh10.shiftwise.domain.Assignment;
import io.github.ahmedsh10.shiftwise.domain.Employee;
import io.github.ahmedsh10.shiftwise.domain.ShiftTemplate;
import io.github.ahmedsh10.shiftwise.service.RuleViolation.Rule;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure scheduling rules, kept free of Spring and the database so they are easy to test
 * and to reuse later for replacement suggestions.
 */
public final class AssignmentRules {

    private AssignmentRules() {
    }

    public static List<RuleViolation> check(Employee employee, Area area, ShiftTemplate shift, LocalDate date,
                                            ScheduleContext context, RuleSettings settings) {
        List<RuleViolation> violations = new ArrayList<>();
        String who = employee.getFullName();

        if (!employee.isActive()) {
            violations.add(new RuleViolation(Rule.EMPLOYEE_INACTIVE, who + " is not active"));
        }

        if (context.absent()) {
            violations.add(new RuleViolation(Rule.EMPLOYEE_ABSENT, who + " is absent on " + date));
        }

        if (!employee.canWorkIn(area)) {
            violations.add(new RuleViolation(Rule.NOT_QUALIFIED_FOR_AREA,
                    who + " is not trained for " + area.getName()));
        }

        checkOverlap(who, shift, date, context, violations);
        checkRest(who, shift, date, context, settings, violations);
        checkWeeklyHours(employee, shift, context, violations);

        return violations;
    }

    private static void checkOverlap(String who, ShiftTemplate shift, LocalDate date, ScheduleContext context,
                                     List<RuleViolation> violations) {
        for (Assignment existing : context.nearby()) {
            if (existing.getWorkDate().equals(date) && existing.getShiftTemplate().overlaps(shift)) {
                violations.add(new RuleViolation(Rule.OVERLAPPING_SHIFT,
                        who + " already works " + existing.getShiftTemplate().getCode()
                                + " in " + existing.getArea().getName() + " on " + date));
                return;
            }
        }
    }

    /**
     * Rest is measured between working days: from the last end on one day to the first start
     * on the next. Breaks inside a day (a split shift, morning + evening) are not "rest".
     */
    private static void checkRest(String who, ShiftTemplate shift, LocalDate date, ScheduleContext context,
                                  RuleSettings settings, List<RuleViolation> violations) {
        Duration minRest = settings.minRest();
        if (minRest.isZero()) {
            return;
        }
        LocalDateTime newStart = date.atTime(shift.firstStart());
        LocalDateTime newEnd = date.atTime(shift.lastEnd());

        for (Assignment existing : context.nearby()) {
            LocalDate otherDate = existing.getWorkDate();
            ShiftTemplate other = existing.getShiftTemplate();
            Duration rest;
            if (otherDate.equals(date.minusDays(1))) {
                rest = Duration.between(otherDate.atTime(other.lastEnd()), newStart);
            } else if (otherDate.equals(date.plusDays(1))) {
                rest = Duration.between(newEnd, otherDate.atTime(other.firstStart()));
            } else {
                continue;
            }
            if (rest.compareTo(minRest) < 0) {
                violations.add(new RuleViolation(Rule.INSUFFICIENT_REST,
                        who + " would rest only " + format(rest) + " between " + other.getCode() + " on "
                                + otherDate + " and " + shift.getCode() + " on " + date
                                + " (minimum " + settings.minRestHours() + "h)"));
                return;
            }
        }
    }

    private static void checkWeeklyHours(Employee employee, ShiftTemplate shift, ScheduleContext context,
                                         List<RuleViolation> violations) {
        Duration alreadyPlanned = context.week().stream()
                .map(a -> a.getShiftTemplate().workedTime())
                .reduce(Duration.ZERO, Duration::plus);
        Duration total = alreadyPlanned.plus(shift.workedTime());
        Duration contract = Duration.ofHours(employee.getContractHoursPerWeek());
        if (total.compareTo(contract) > 0) {
            violations.add(new RuleViolation(Rule.WEEKLY_HOURS_EXCEEDED,
                    employee.getFullName() + " would work " + format(total) + " this week, contract is "
                            + employee.getContractHoursPerWeek() + "h"));
        }
    }

    private static String format(Duration d) {
        long h = d.toHours();
        long m = d.toMinutesPart();
        return m == 0 ? h + "h" : h + "h" + String.format("%02d", m);
    }
}
