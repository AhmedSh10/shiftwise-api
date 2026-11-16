package io.github.ahmedsh10.shiftwise.web;

import io.github.ahmedsh10.shiftwise.service.AbsenceService;
import io.github.ahmedsh10.shiftwise.web.dto.AbsenceResponse;
import io.github.ahmedsh10.shiftwise.web.dto.CreateAbsenceRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/absences")
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class AbsenceController {

    private final AbsenceService absenceService;

    public AbsenceController(AbsenceService absenceService) {
        this.absenceService = absenceService;
    }

    /** Records an absence and returns the shifts it cancelled (the new gaps). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AbsenceResponse create(@Valid @RequestBody CreateAbsenceRequest request) {
        return absenceService.create(request);
    }

    @GetMapping
    public List<AbsenceResponse> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return absenceService.list(from, to);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        absenceService.delete(id);
    }
}
