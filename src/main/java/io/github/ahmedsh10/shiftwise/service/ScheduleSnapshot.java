package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.domain.Absence;
import io.github.ahmedsh10.shiftwise.domain.Assignment;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Everyone's assignments and absences for a window of dates, loaded with two queries.
 * Builds a {@link ScheduleContext} for any employee and date in memory, so ranking candidates
 * or generating a week does not hit the database once per employee.
 * New assignments can be added as they are planned, so later decisions see earlier ones.
 */
public class ScheduleSnapshot {

    private final List<Assignment> assignments;
    private final List<Absence> absences;

    public ScheduleSnapshot(List<Assignment> assignments, List<Absence> absences) {
        this.assignments = new ArrayList<>(assignments);
        this.absences = List.copyOf(absences);
    }

    /** First and last date a snapshot must cover to judge every date in [from, to]. */
    public static LocalDate[] window(LocalDate from, LocalDate to) {
        LocalDate start = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusDays(1);
        LocalDate end = to.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).plusDays(1);
        return new LocalDate[]{start, end};
    }

    public void add(Assignment assignment) {
        assignments.add(assignment);
    }

    public ScheduleContext contextFor(Long employeeId, LocalDate date) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        List<Assignment> mine = assignments.stream()
                .filter(a -> a.getEmployee().getId().equals(employeeId))
                .toList();
        List<Assignment> nearby = mine.stream()
                .filter(a -> !a.getWorkDate().isBefore(date.minusDays(1)) && !a.getWorkDate().isAfter(date.plusDays(1)))
                .toList();
        List<Assignment> week = mine.stream()
                .filter(a -> !a.getWorkDate().isBefore(monday) && !a.getWorkDate().isAfter(sunday))
                .toList();
        boolean absent = absences.stream()
                .anyMatch(ab -> ab.getEmployee().getId().equals(employeeId) && ab.covers(date));
        return new ScheduleContext(nearby, week, absent);
    }

    public long countAssigned(Long areaId, Long shiftTemplateId, LocalDate date) {
        return assignments.stream()
                .filter(a -> a.getWorkDate().equals(date)
                        && a.getArea().getId().equals(areaId)
                        && a.getShiftTemplate().getId().equals(shiftTemplateId))
                .count();
    }
}
