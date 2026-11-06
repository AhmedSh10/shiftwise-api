package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.domain.Assignment;
import io.github.ahmedsh10.shiftwise.domain.CoverageRequirement;
import io.github.ahmedsh10.shiftwise.repository.AssignmentRepository;
import io.github.ahmedsh10.shiftwise.repository.CoverageRequirementRepository;
import io.github.ahmedsh10.shiftwise.web.dto.CoverageSlot;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Compares what each area needs with who is actually scheduled. */
@Service
public class CoverageService {

    private final CoverageRequirementRepository requirements;
    private final AssignmentRepository assignments;

    public CoverageService(CoverageRequirementRepository requirements, AssignmentRepository assignments) {
        this.requirements = requirements;
        this.assignments = assignments;
    }

    @Transactional(readOnly = true)
    public List<CoverageSlot> forDate(LocalDate date) {
        Map<String, Long> assignedPerSlot = assignments.findByWorkDate(date).stream()
                .collect(Collectors.groupingBy(CoverageService::slotKey, Collectors.counting()));

        return requirements.findAll(Sort.by("id")).stream()
                .map(r -> toSlot(date, r, assignedPerSlot.getOrDefault(slotKey(r), 0L).intValue()))
                .toList();
    }

    private static CoverageSlot toSlot(LocalDate date, CoverageRequirement r, int assigned) {
        int missing = Math.max(0, r.getRequiredHeadcount() - assigned);
        return new CoverageSlot(date, r.getArea().getCode(), r.getArea().getName(), r.getShiftTemplate().getCode(),
                r.getRequiredHeadcount(), assigned, missing, CoverageSlot.statusFor(missing));
    }

    private static String slotKey(Assignment a) {
        return a.getArea().getId() + ":" + a.getShiftTemplate().getId();
    }

    private static String slotKey(CoverageRequirement r) {
        return r.getArea().getId() + ":" + r.getShiftTemplate().getId();
    }
}
