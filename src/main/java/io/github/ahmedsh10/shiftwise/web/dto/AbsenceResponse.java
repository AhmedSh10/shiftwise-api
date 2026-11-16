package io.github.ahmedsh10.shiftwise.web.dto;

import io.github.ahmedsh10.shiftwise.domain.Absence;

import java.time.LocalDate;
import java.util.List;

/**
 * @param removedAssignments shifts that were cancelled because of this absence: these are the new gaps
 */
public record AbsenceResponse(Long id, Long employeeId, String employeeName, LocalDate startDate, LocalDate endDate,
                              String reason, List<AssignmentResponse> removedAssignments) {

    public static AbsenceResponse from(Absence a, List<AssignmentResponse> removed) {
        return new AbsenceResponse(a.getId(), a.getEmployee().getId(), a.getEmployee().getFullName(),
                a.getStartDate(), a.getEndDate(), a.getReason(), removed);
    }
}
