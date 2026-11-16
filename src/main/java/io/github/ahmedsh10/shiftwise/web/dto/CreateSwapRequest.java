package io.github.ahmedsh10.shiftwise.web.dto;

import jakarta.validation.constraints.NotNull;

public record CreateSwapRequest(@NotNull Long assignmentId, @NotNull Long targetEmployeeId) {
}
