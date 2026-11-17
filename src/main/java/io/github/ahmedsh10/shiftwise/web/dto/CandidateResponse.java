package io.github.ahmedsh10.shiftwise.web.dto;

import io.github.ahmedsh10.shiftwise.service.Candidate;

import java.util.List;
import java.util.Map;

public record CandidateResponse(Long employeeId, String fullName, double plannedHoursThisWeek, int contractHours,
                                boolean eligible, List<Map<String, String>> blockedBy) {

    public static CandidateResponse from(Candidate c) {
        List<Map<String, String>> reasons = c.violations().stream()
                .map(v -> Map.of("rule", v.rule().name(), "message", v.message()))
                .toList();
        return new CandidateResponse(c.employee().getId(), c.employee().getFullName(),
                c.plannedThisWeek().toMinutes() / 60.0, c.employee().getContractHoursPerWeek(),
                c.eligible(), reasons);
    }
}
