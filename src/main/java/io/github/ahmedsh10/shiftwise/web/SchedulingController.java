package io.github.ahmedsh10.shiftwise.web;

import io.github.ahmedsh10.shiftwise.service.ScheduleGenerator;
import io.github.ahmedsh10.shiftwise.service.SuggestionService;
import io.github.ahmedsh10.shiftwise.web.dto.GenerateScheduleRequest;
import io.github.ahmedsh10.shiftwise.web.dto.GenerateScheduleResponse;
import io.github.ahmedsh10.shiftwise.web.dto.SuggestionResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class SchedulingController {

    private final SuggestionService suggestionService;
    private final ScheduleGenerator scheduleGenerator;

    public SchedulingController(SuggestionService suggestionService, ScheduleGenerator scheduleGenerator) {
        this.suggestionService = suggestionService;
        this.scheduleGenerator = scheduleGenerator;
    }

    /** Who can fill this slot, fairest first. includeBlocked=true also lists trained people who are blocked, and why. */
    @GetMapping("/suggestions")
    public SuggestionResponse suggestions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Long areaId,
            @RequestParam Long shiftTemplateId,
            @RequestParam(defaultValue = "false") boolean includeBlocked) {
        return suggestionService.suggest(date, areaId, shiftTemplateId, includeBlocked);
    }

    /** Fills a week's open slots. Use dryRun=true to preview the plan without saving it. */
    @PostMapping("/schedules/generate")
    public GenerateScheduleResponse generate(@Valid @RequestBody GenerateScheduleRequest request) {
        return scheduleGenerator.generate(request);
    }
}
