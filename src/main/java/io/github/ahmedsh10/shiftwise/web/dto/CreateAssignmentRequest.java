package io.github.ahmedsh10.shiftwise.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateAssignmentRequest(
        @NotNull Long employeeId,
        @NotNull Long areaId,
        @NotNull Long shiftTemplateId,
        @NotNull LocalDate date) {
}
