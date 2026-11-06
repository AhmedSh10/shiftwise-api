package io.github.ahmedsh10.shiftwise.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpsertCoverageRequirementRequest(
        @NotNull Long areaId,
        @NotNull Long shiftTemplateId,
        @Min(1) @Max(100) int requiredHeadcount) {
}
