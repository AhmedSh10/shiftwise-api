package io.github.ahmedsh10.shiftwise.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateSkillsRequest(@NotNull Set<Long> areaIds) {
}
