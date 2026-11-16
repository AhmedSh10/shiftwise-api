package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.domain.Assignment;
import io.github.ahmedsh10.shiftwise.domain.Employee;
import io.github.ahmedsh10.shiftwise.domain.SwapRequest;
import io.github.ahmedsh10.shiftwise.domain.SwapStatus;
import io.github.ahmedsh10.shiftwise.repository.AssignmentRepository;
import io.github.ahmedsh10.shiftwise.repository.EmployeeRepository;
import io.github.ahmedsh10.shiftwise.repository.SwapRequestRepository;
import io.github.ahmedsh10.shiftwise.security.CurrentUser;
import io.github.ahmedsh10.shiftwise.web.dto.CreateSwapRequest;
import io.github.ahmedsh10.shiftwise.web.dto.SwapResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Shift swap workflow: the employee asks a colleague, the colleague accepts, a supervisor approves.
 * All rules are checked for the colleague when the request is made and again at approval,
 * because their schedule may have changed in between.
 */
@Service
public class SwapService {

    private static final EnumSet<SwapStatus> OPEN = EnumSet.of(SwapStatus.PENDING_COLLEAGUE, SwapStatus.PENDING_SUPERVISOR);

    private final SwapRequestRepository swaps;
    private final AssignmentRepository assignments;
    private final EmployeeRepository employees;
    private final ScheduleContextLoader contextLoader;
    private final RuleSettings ruleSettings;

    public SwapService(SwapRequestRepository swaps, AssignmentRepository assignments, EmployeeRepository employees,
                       ScheduleContextLoader contextLoader, RuleSettings ruleSettings) {
        this.swaps = swaps;
        this.assignments = assignments;
        this.employees = employees;
        this.contextLoader = contextLoader;
        this.ruleSettings = ruleSettings;
    }

    @Transactional
    public SwapResponse request(CreateSwapRequest request, CurrentUser user) {
        Assignment assignment = assignments.findById(request.assignmentId())
                .orElseThrow(() -> new NotFoundException("Assignment", request.assignmentId()));
        if (!Objects.equals(assignment.getEmployee().getId(), user.employeeId())) {
            throw new ForbiddenException("You can only offer your own shifts");
        }
        if (Objects.equals(request.targetEmployeeId(), user.employeeId())) {
            throw new IllegalArgumentException("Choose a colleague, not yourself");
        }
        if (swaps.existsByAssignmentIdAndStatusIn(assignment.getId(), OPEN)) {
            throw new ConflictException("This shift already has an open swap request");
        }
        Employee target = employees.findById(request.targetEmployeeId())
                .orElseThrow(() -> new NotFoundException("Employee", request.targetEmployeeId()));

        checkRulesFor(target, assignment);

        return SwapResponse.from(swaps.save(new SwapRequest(assignment, assignment.getEmployee(), target)));
    }

    @Transactional
    public SwapResponse accept(Long id, CurrentUser user) {
        SwapRequest swap = load(id);
        requireStatus(swap, SwapStatus.PENDING_COLLEAGUE);
        if (!Objects.equals(swap.getTarget().getId(), user.employeeId())) {
            throw new ForbiddenException("Only the colleague who was asked can accept");
        }
        swap.moveTo(SwapStatus.PENDING_SUPERVISOR);
        return SwapResponse.from(swap);
    }

    @Transactional
    public SwapResponse approve(Long id) {
        SwapRequest swap = load(id);
        requireStatus(swap, SwapStatus.PENDING_SUPERVISOR);

        // Lock both people, always in id order so two approvals can never deadlock each other.
        Long a = swap.getRequester().getId();
        Long b = swap.getTarget().getId();
        lock(Math.min(a, b));
        lock(Math.max(a, b));

        Assignment assignment = swap.getAssignment();
        if (!Objects.equals(assignment.getEmployee().getId(), swap.getRequester().getId())) {
            throw new ConflictException("The shift no longer belongs to the requester");
        }
        checkRulesFor(swap.getTarget(), assignment);

        assignment.reassignTo(swap.getTarget());
        swap.moveTo(SwapStatus.APPROVED);
        return SwapResponse.from(swap);
    }

    /** The colleague (while it waits for them) or a supervisor can say no. */
    @Transactional
    public SwapResponse reject(Long id, CurrentUser user) {
        SwapRequest swap = load(id);
        if (!swap.getStatus().isOpen()) {
            throw new ConflictException("This swap is already " + swap.getStatus());
        }
        boolean colleague = Objects.equals(swap.getTarget().getId(), user.employeeId())
                && swap.getStatus() == SwapStatus.PENDING_COLLEAGUE;
        if (!colleague && !user.isSupervisorOrAdmin()) {
            throw new ForbiddenException("You cannot reject this swap");
        }
        swap.moveTo(SwapStatus.REJECTED);
        return SwapResponse.from(swap);
    }

    @Transactional
    public SwapResponse cancel(Long id, CurrentUser user) {
        SwapRequest swap = load(id);
        if (!swap.getStatus().isOpen()) {
            throw new ConflictException("This swap is already " + swap.getStatus());
        }
        if (!Objects.equals(swap.getRequester().getId(), user.employeeId())) {
            throw new ForbiddenException("Only the requester can cancel");
        }
        swap.moveTo(SwapStatus.CANCELLED);
        return SwapResponse.from(swap);
    }

    /** Supervisors see every swap (optionally only open ones); employees see the ones they are part of. */
    @Transactional(readOnly = true)
    public List<SwapResponse> list(CurrentUser user, boolean openOnly) {
        List<SwapRequest> found;
        if (user.isSupervisorOrAdmin()) {
            found = openOnly ? swaps.findByStatusInOrderByCreatedAtDesc(OPEN) : swaps.findAllByOrderByCreatedAtDesc();
        } else {
            found = swaps.findInvolving(user.employeeId()).stream()
                    .filter(s -> !openOnly || s.getStatus().isOpen())
                    .toList();
        }
        return found.stream().map(SwapResponse::from).toList();
    }

    private void checkRulesFor(Employee target, Assignment assignment) {
        ScheduleContext context = contextLoader.load(target.getId(), assignment.getWorkDate());
        List<RuleViolation> violations = AssignmentRules.check(target, assignment.getArea(),
                assignment.getShiftTemplate(), assignment.getWorkDate(), context, ruleSettings);
        if (!violations.isEmpty()) {
            throw new RuleViolationException(violations);
        }
    }

    private void lock(Long employeeId) {
        employees.findByIdForUpdate(employeeId).orElseThrow(() -> new NotFoundException("Employee", employeeId));
    }

    private SwapRequest load(Long id) {
        return swaps.findById(id).orElseThrow(() -> new NotFoundException("Swap request", id));
    }

    private static void requireStatus(SwapRequest swap, SwapStatus expected) {
        if (swap.getStatus() != expected) {
            throw new ConflictException("Swap is " + swap.getStatus() + ", expected " + expected);
        }
    }
}
