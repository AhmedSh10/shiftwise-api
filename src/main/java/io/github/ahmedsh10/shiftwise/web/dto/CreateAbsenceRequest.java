package io.github.ahmedsh10.shiftwise.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateAbsenceRequest(
        @NotNull Long employeeId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Size(max = 200) String reason) {
}
