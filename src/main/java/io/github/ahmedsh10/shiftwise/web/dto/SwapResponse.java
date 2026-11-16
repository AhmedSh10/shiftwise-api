package io.github.ahmedsh10.shiftwise.web.dto;

import io.github.ahmedsh10.shiftwise.domain.SwapRequest;

import java.time.Instant;

public record SwapResponse(Long id, String status, AssignmentResponse assignment,
                           Long requesterId, String requesterName, Long targetEmployeeId, String targetName,
                           Instant createdAt, Instant decidedAt) {

    public static SwapResponse from(SwapRequest s) {
        return new SwapResponse(s.getId(), s.getStatus().name(), AssignmentResponse.from(s.getAssignment()),
                s.getRequester().getId(), s.getRequester().getFullName(),
                s.getTarget().getId(), s.getTarget().getFullName(),
                s.getCreatedAt(), s.getDecidedAt());
    }
}
