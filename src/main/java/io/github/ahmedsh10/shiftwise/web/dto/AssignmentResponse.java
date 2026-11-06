package io.github.ahmedsh10.shiftwise.web.dto;

import io.github.ahmedsh10.shiftwise.domain.Assignment;

import java.time.LocalDate;

public record AssignmentResponse(Long id, LocalDate date, Long employeeId, String employeeName,
                                 String areaCode, String shiftCode) {

    public static AssignmentResponse from(Assignment a) {
        return new AssignmentResponse(a.getId(), a.getWorkDate(), a.getEmployee().getId(),
                a.getEmployee().getFullName(), a.getArea().getCode(), a.getShiftTemplate().getCode());
    }
}
