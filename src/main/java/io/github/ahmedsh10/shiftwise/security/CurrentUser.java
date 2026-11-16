package io.github.ahmedsh10.shiftwise.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/** The signed-in user, read from the JWT. */
public record CurrentUser(Long employeeId, String role) {

    public static CurrentUser from(Jwt jwt) {
        Number id = jwt.getClaim("employeeId");
        List<String> roles = jwt.getClaimAsStringList("roles");
        String role = roles == null || roles.isEmpty() ? "EMPLOYEE" : roles.get(0);
        return new CurrentUser(id == null ? null : id.longValue(), role);
    }

    public boolean isSupervisorOrAdmin() {
        return "SUPERVISOR".equals(role) || "ADMIN".equals(role);
    }
}
