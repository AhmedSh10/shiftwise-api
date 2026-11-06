package io.github.ahmedsh10.shiftwise.web;

import io.github.ahmedsh10.shiftwise.service.CoverageRequirementService;
import io.github.ahmedsh10.shiftwise.service.CoverageService;
import io.github.ahmedsh10.shiftwise.web.dto.CoverageRequirementResponse;
import io.github.ahmedsh10.shiftwise.web.dto.CoverageSlot;
import io.github.ahmedsh10.shiftwise.web.dto.UpsertCoverageRequirementRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CoverageController {

    private final CoverageRequirementService requirementService;
    private final CoverageService coverageService;

    public CoverageController(CoverageRequirementService requirementService, CoverageService coverageService) {
        this.requirementService = requirementService;
        this.coverageService = coverageService;
    }

    @GetMapping("/coverage-requirements")
    public List<CoverageRequirementResponse> requirements() {
        return requirementService.list();
    }

    @PutMapping("/coverage-requirements")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public CoverageRequirementResponse upsert(@Valid @RequestBody UpsertCoverageRequirementRequest request) {
        return requirementService.upsert(request);
    }

    /** Green / yellow / red staffing status of every area and shift for one day. */
    @GetMapping("/coverage")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public List<CoverageSlot> coverage(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return coverageService.forDate(date);
    }
}
