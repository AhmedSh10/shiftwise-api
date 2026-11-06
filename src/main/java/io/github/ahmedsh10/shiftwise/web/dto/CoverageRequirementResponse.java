package io.github.ahmedsh10.shiftwise.web.dto;

import io.github.ahmedsh10.shiftwise.domain.CoverageRequirement;

public record CoverageRequirementResponse(Long id, Long areaId, String areaCode, Long shiftTemplateId,
                                          String shiftCode, int requiredHeadcount) {

    public static CoverageRequirementResponse from(CoverageRequirement r) {
        return new CoverageRequirementResponse(r.getId(), r.getArea().getId(), r.getArea().getCode(),
                r.getShiftTemplate().getId(), r.getShiftTemplate().getCode(), r.getRequiredHeadcount());
    }
}
