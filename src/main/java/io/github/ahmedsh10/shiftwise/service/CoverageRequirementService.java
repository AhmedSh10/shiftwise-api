package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.domain.Area;
import io.github.ahmedsh10.shiftwise.domain.CoverageRequirement;
import io.github.ahmedsh10.shiftwise.domain.ShiftTemplate;
import io.github.ahmedsh10.shiftwise.repository.AreaRepository;
import io.github.ahmedsh10.shiftwise.repository.CoverageRequirementRepository;
import io.github.ahmedsh10.shiftwise.repository.ShiftTemplateRepository;
import io.github.ahmedsh10.shiftwise.web.dto.CoverageRequirementResponse;
import io.github.ahmedsh10.shiftwise.web.dto.UpsertCoverageRequirementRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CoverageRequirementService {

    private final CoverageRequirementRepository requirements;
    private final AreaRepository areas;
    private final ShiftTemplateRepository templates;

    public CoverageRequirementService(CoverageRequirementRepository requirements, AreaRepository areas,
                                      ShiftTemplateRepository templates) {
        this.requirements = requirements;
        this.areas = areas;
        this.templates = templates;
    }

    @Transactional(readOnly = true)
    public List<CoverageRequirementResponse> list() {
        return requirements.findAll(Sort.by("id")).stream().map(CoverageRequirementResponse::from).toList();
    }

    /** Creates the requirement, or updates the headcount if one exists for this area and shift. */
    @Transactional
    public CoverageRequirementResponse upsert(UpsertCoverageRequirementRequest request) {
        Area area = areas.findById(request.areaId())
                .orElseThrow(() -> new NotFoundException("Area", request.areaId()));
        ShiftTemplate shift = templates.findById(request.shiftTemplateId())
                .orElseThrow(() -> new NotFoundException("Shift template", request.shiftTemplateId()));
        CoverageRequirement requirement = requirements
                .findByAreaIdAndShiftTemplateId(area.getId(), shift.getId())
                .orElseGet(() -> new CoverageRequirement(area, shift, request.requiredHeadcount()));
        requirement.setRequiredHeadcount(request.requiredHeadcount());
        return CoverageRequirementResponse.from(requirements.save(requirement));
    }
}
