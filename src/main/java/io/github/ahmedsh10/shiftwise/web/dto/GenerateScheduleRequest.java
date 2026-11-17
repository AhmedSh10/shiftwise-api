package io.github.ahmedsh10.shiftwise.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * @param weekStart the Monday of the week to fill
 * @param dryRun    true: plan and return the result without saving anything
 */
public record GenerateScheduleRequest(@NotNull LocalDate weekStart, boolean dryRun) {
}
