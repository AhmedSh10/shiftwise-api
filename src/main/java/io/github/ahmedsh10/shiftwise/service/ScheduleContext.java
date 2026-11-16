package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.domain.Assignment;

import java.util.List;

/**
 * What we already know about one employee around the date being scheduled.
 *
 * @param nearby assignments from the day before to the day after (overlap and rest checks)
 * @param week   assignments in the same ISO week, Monday to Sunday (contract hours check)
 * @param absent whether the employee has an absence covering the date
 */
public record ScheduleContext(List<Assignment> nearby, List<Assignment> week, boolean absent) {

    public static ScheduleContext empty() {
        return new ScheduleContext(List.of(), List.of(), false);
    }
}
