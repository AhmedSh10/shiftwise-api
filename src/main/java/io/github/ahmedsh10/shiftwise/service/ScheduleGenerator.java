package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.domain.Assignment;
import io.github.ahmedsh10.shiftwise.domain.CoverageRequirement;
import io.github.ahmedsh10.shiftwise.domain.Employee;
import io.github.ahmedsh10.shiftwise.repository.AbsenceRepository;
import io.github.ahmedsh10.shiftwise.repository.AssignmentRepository;
import io.github.ahmedsh10.shiftwise.repository.CoverageRequirementRepository;
import io.github.ahmedsh10.shiftwise.repository.EmployeeRepository;
import io.github.ahmedsh10.shiftwise.web.dto.AssignmentResponse;
import io.github.ahmedsh10.shiftwise.web.dto.GenerateScheduleRequest;
import io.github.ahmedsh10.shiftwise.web.dto.GenerateScheduleResponse;
import io.github.ahmedsh10.shiftwise.web.dto.GenerateScheduleResponse.UnfilledSlot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Fills the open slots of one week, greedily, with the same rules and ranking a supervisor gets
 * from the suggestions endpoint. Existing assignments are kept; only gaps are filled.
 *
 * Day by day, so each day sees the rest constraints left by the day before. Within a day the slots
 * with the fewest trained people go first, so a scarce skill is not used up on an easy slot.
 */
@Service
public class ScheduleGenerator {

    private final EmployeeRepository employees;
    private final CoverageRequirementRepository requirements;
    private final AssignmentRepository assignments;
    private final AbsenceRepository absences;
    private final RuleSettings ruleSettings;

    public ScheduleGenerator(EmployeeRepository employees, CoverageRequirementRepository requirements,
                             AssignmentRepository assignments, AbsenceRepository absences, RuleSettings ruleSettings) {
        this.employees = employees;
        this.requirements = requirements;
        this.assignments = assignments;
        this.absences = absences;
        this.ruleSettings = ruleSettings;
    }

    @Transactional
    public GenerateScheduleResponse generate(GenerateScheduleRequest request) {
        LocalDate monday = request.weekStart();
        if (monday.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("weekStart must be a Monday");
        }
        LocalDate sunday = monday.plusDays(6);

        // Hold every active employee's row for the whole run: nobody's schedule changes under us.
        List<Employee> staff = employees.lockAllActive();

        LocalDate[] window = ScheduleSnapshot.window(monday, sunday);
        ScheduleSnapshot snapshot = new ScheduleSnapshot(
                assignments.findByWorkDateBetween(window[0], window[1]),
                absences.findOverlapping(window[0], window[1]));

        List<CoverageRequirement> slots = requirements.findAll().stream()
                .sorted(Comparator.comparingLong((CoverageRequirement r) ->
                                staff.stream().filter(e -> e.canWorkIn(r.getArea())).count())
                        .thenComparing(CoverageRequirement::getId))
                .toList();

        List<AssignmentResponse> created = new ArrayList<>();
        List<UnfilledSlot> unfilled = new ArrayList<>();

        for (LocalDate day = monday; !day.isAfter(sunday); day = day.plusDays(1)) {
            for (CoverageRequirement slot : slots) {
                long have = snapshot.countAssigned(slot.getArea().getId(), slot.getShiftTemplate().getId(), day);
                int missing = (int) (slot.getRequiredHeadcount() - have);
                while (missing > 0) {
                    Optional<Candidate> best = ReplacementRanker
                            .rank(staff, slot.getArea(), slot.getShiftTemplate(), day, snapshot, ruleSettings)
                            .stream().filter(Candidate::eligible).findFirst();
                    if (best.isEmpty()) {
                        break;
                    }
                    Assignment planned = new Assignment(best.get().employee(), slot.getArea(), slot.getShiftTemplate(), day);
                    if (!request.dryRun()) {
                        planned = assignments.save(planned);
                    }
                    snapshot.add(planned);
                    created.add(AssignmentResponse.from(planned));
                    missing--;
                }
                if (missing > 0) {
                    unfilled.add(new UnfilledSlot(day, slot.getArea().getCode(), slot.getShiftTemplate().getCode(), missing));
                }
            }
        }

        return new GenerateScheduleResponse(monday, request.dryRun(), created.size(), unfilled.size(), created, unfilled);
    }
}
