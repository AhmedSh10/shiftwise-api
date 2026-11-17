package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.domain.Area;
import io.github.ahmedsh10.shiftwise.domain.CoverageRequirement;
import io.github.ahmedsh10.shiftwise.domain.ShiftTemplate;
import io.github.ahmedsh10.shiftwise.repository.AbsenceRepository;
import io.github.ahmedsh10.shiftwise.repository.AreaRepository;
import io.github.ahmedsh10.shiftwise.repository.AssignmentRepository;
import io.github.ahmedsh10.shiftwise.repository.CoverageRequirementRepository;
import io.github.ahmedsh10.shiftwise.repository.EmployeeRepository;
import io.github.ahmedsh10.shiftwise.repository.ShiftTemplateRepository;
import io.github.ahmedsh10.shiftwise.web.dto.CandidateResponse;
import io.github.ahmedsh10.shiftwise.web.dto.SuggestionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** "Someone called in sick: who can take their shift?" */
@Service
public class SuggestionService {

    private final EmployeeRepository employees;
    private final AreaRepository areas;
    private final ShiftTemplateRepository templates;
    private final CoverageRequirementRepository requirements;
    private final AssignmentRepository assignments;
    private final AbsenceRepository absences;
    private final RuleSettings ruleSettings;

    public SuggestionService(EmployeeRepository employees, AreaRepository areas, ShiftTemplateRepository templates,
                             CoverageRequirementRepository requirements, AssignmentRepository assignments,
                             AbsenceRepository absences, RuleSettings ruleSettings) {
        this.employees = employees;
        this.areas = areas;
        this.templates = templates;
        this.requirements = requirements;
        this.assignments = assignments;
        this.absences = absences;
        this.ruleSettings = ruleSettings;
    }

    @Transactional(readOnly = true)
    public SuggestionResponse suggest(LocalDate date, Long areaId, Long shiftTemplateId, boolean includeBlocked) {
        Area area = areas.findById(areaId).orElseThrow(() -> new NotFoundException("Area", areaId));
        ShiftTemplate shift = templates.findById(shiftTemplateId)
                .orElseThrow(() -> new NotFoundException("Shift template", shiftTemplateId));

        LocalDate[] window = ScheduleSnapshot.window(date, date);
        ScheduleSnapshot snapshot = new ScheduleSnapshot(
                assignments.findByWorkDateBetween(window[0], window[1]),
                absences.findOverlapping(window[0], window[1]));

        int required = requirements.findByAreaIdAndShiftTemplateId(areaId, shiftTemplateId)
                .map(CoverageRequirement::getRequiredHeadcount).orElse(0);
        int assigned = (int) snapshot.countAssigned(areaId, shiftTemplateId, date);

        List<CandidateResponse> candidates = ReplacementRanker
                .rank(employees.findByActiveTrueOrderByIdAsc(), area, shift, date, snapshot, ruleSettings).stream()
                .filter(c -> includeBlocked || c.eligible())
                .map(CandidateResponse::from)
                .toList();

        return new SuggestionResponse(date, area.getCode(), shift.getCode(), required, assigned,
                Math.max(0, required - assigned), candidates);
    }
}
