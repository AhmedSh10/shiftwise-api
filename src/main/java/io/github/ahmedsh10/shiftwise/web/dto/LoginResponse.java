package io.github.ahmedsh10.shiftwise.web.dto;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt, Long employeeId, String role) {
}
