package io.github.ahmedsh10.shiftwise.web.dto;

import io.github.ahmedsh10.shiftwise.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateEmployeeRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role,
        @Min(1) @Max(48) int contractHoursPerWeek,
        Set<Long> skillAreaIds) {
}
