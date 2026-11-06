package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.domain.Area;
import io.github.ahmedsh10.shiftwise.domain.Assignment;
import io.github.ahmedsh10.shiftwise.domain.Employee;
import io.github.ahmedsh10.shiftwise.domain.ShiftTemplate;
import io.github.ahmedsh10.shiftwise.repository.AreaRepository;
import io.github.ahmedsh10.shiftwise.repository.AssignmentRepository;
import io.github.ahmedsh10.shiftwise.repository.EmployeeRepository;
import io.github.ahmedsh10.shiftwise.repository.ShiftTemplateRepository;
import io.github.ahmedsh10.shiftwise.web.dto.AssignmentResponse;
import io.github.ahmedsh10.shiftwise.web.dto.CreateAssignmentRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AssignmentService {

    private static final int MAX_RANGE_DAYS = 62;

    private final AssignmentRepository assignments;
    private final EmployeeRepository employees;
    private final AreaRepository areas;
    private final ShiftTemplateRepository templates;

    public AssignmentService(AssignmentRepository assignments, EmployeeRepository employees,
                             AreaRepository areas, ShiftTemplateRepository templates) {
        this.assignments = assignments;
        this.employees = employees;
        this.areas = areas;
        this.templates = templates;
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> list(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("'to' must not be before 'from'");
        }
        if (from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw new IllegalArgumentException("Range is limited to " + MAX_RANGE_DAYS + " days");
        }
        return assignments.findByWorkDateBetweenOrderByWorkDateAscIdAsc(from, to).stream()
                .map(AssignmentResponse::from).toList();
    }

    @Transactional
    public AssignmentResponse create(CreateAssignmentRequest request) {
        Employee employee = employees.findWithSkillsById(request.employeeId())
                .orElseThrow(() -> new NotFoundException("Employee", request.employeeId()));
        Area area = areas.findById(request.areaId())
                .orElseThrow(() -> new NotFoundException("Area", request.areaId()));
        ShiftTemplate shift = templates.findById(request.shiftTemplateId())
                .orElseThrow(() -> new NotFoundException("Shift template", request.shiftTemplateId()));

        List<Assignment> sameDay = assignments.findByEmployeeIdAndWorkDate(employee.getId(), request.date());
        List<RuleViolation> violations = AssignmentRules.check(employee, area, shift, request.date(), sameDay);
        if (!violations.isEmpty()) {
            throw new RuleViolationException(violations);
        }

        try {
            Assignment saved = assignments.saveAndFlush(new Assignment(employee, area, shift, request.date()));
            return AssignmentResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            // Two supervisors booking the same person at the same moment: the unique key wins.
            throw new ConflictException("This employee already has that shift on " + request.date());
        }
    }

    @Transactional
    public void delete(Long id) {
        if (!assignments.existsById(id)) {
            throw new NotFoundException("Assignment", id);
        }
        assignments.deleteById(id);
    }
}
