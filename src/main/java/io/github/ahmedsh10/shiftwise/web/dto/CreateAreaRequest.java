package io.github.ahmedsh10.shiftwise.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAreaRequest(
        @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Z0-9_]+", message = "use UPPER_CASE letters, digits and _") String code,
        @NotBlank @Size(max = 120) String name) {
}
