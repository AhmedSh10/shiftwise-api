package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.domain.Assignment;
import io.github.ahmedsh10.shiftwise.repository.AbsenceRepository;
import io.github.ahmedsh10.shiftwise.repository.AssignmentRepository;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/** Loads the {@link ScheduleContext} for an employee and a date. Call it after taking the employee lock. */
@Component
public class ScheduleContextLoader {

    private final AssignmentRepository assignments;
    private final AbsenceRepository absences;

    public ScheduleContextLoader(AssignmentRepository assignments, AbsenceRepository absences) {
        this.assignments = assignments;
        this.absences = absences;
    }

    public ScheduleContext load(Long employeeId, LocalDate date) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        LocalDate from = date.minusDays(1).isBefore(monday) ? date.minusDays(1) : monday;
        LocalDate to = date.plusDays(1).isAfter(sunday) ? date.plusDays(1) : sunday;

        List<Assignment> around = assignments.findByEmployeeIdAndWorkDateBetween(employeeId, from, to);
        List<Assignment> nearby = around.stream()
                .filter(a -> !a.getWorkDate().isBefore(date.minusDays(1)) && !a.getWorkDate().isAfter(date.plusDays(1)))
                .toList();
        List<Assignment> week = around.stream()
                .filter(a -> !a.getWorkDate().isBefore(monday) && !a.getWorkDate().isAfter(sunday))
                .toList();
        return new ScheduleContext(nearby, week, absences.isAbsentOn(employeeId, date));
    }
}
