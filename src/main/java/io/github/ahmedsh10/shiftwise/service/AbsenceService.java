package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.domain.Absence;
import io.github.ahmedsh10.shiftwise.domain.Assignment;
import io.github.ahmedsh10.shiftwise.domain.Employee;
import io.github.ahmedsh10.shiftwise.repository.AbsenceRepository;
import io.github.ahmedsh10.shiftwise.repository.AssignmentRepository;
import io.github.ahmedsh10.shiftwise.repository.EmployeeRepository;
import io.github.ahmedsh10.shiftwise.web.dto.AbsenceResponse;
import io.github.ahmedsh10.shiftwise.web.dto.AssignmentResponse;
import io.github.ahmedsh10.shiftwise.web.dto.CreateAbsenceRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AbsenceService {

    private static final int MAX_ABSENCE_DAYS = 366;

    private final AbsenceRepository absences;
    private final AssignmentRepository assignments;
    private final EmployeeRepository employees;

    public AbsenceService(AbsenceRepository absences, AssignmentRepository assignments, EmployeeRepository employees) {
        this.absences = absences;
        this.assignments = assignments;
        this.employees = employees;
    }

    /**
     * Records the absence and cancels the employee's shifts in that period. The cancelled shifts are
     * returned so the supervisor sees straight away which gaps need filling.
     */
    @Transactional
    public AbsenceResponse create(CreateAbsenceRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        if (request.startDate().plusDays(MAX_ABSENCE_DAYS).isBefore(request.endDate())) {
            throw new IllegalArgumentException("An absence can last at most " + MAX_ABSENCE_DAYS + " days");
        }
        Employee employee = employees.findByIdForUpdate(request.employeeId())
                .orElseThrow(() -> new NotFoundException("Employee", request.employeeId()));

        Absence absence = absences.save(new Absence(employee, request.startDate(), request.endDate(), request.reason()));

        List<Assignment> cancelled = assignments.findByEmployeeIdAndWorkDateBetween(
                employee.getId(), request.startDate(), request.endDate());
        List<AssignmentResponse> removed = cancelled.stream().map(AssignmentResponse::from).toList();
        assignments.deleteAll(cancelled);

        return AbsenceResponse.from(absence, removed);
    }

    @Transactional(readOnly = true)
    public List<AbsenceResponse> list(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("'to' must not be before 'from'");
        }
        return absences.findOverlapping(from, to).stream()
                .map(a -> AbsenceResponse.from(a, List.of()))
                .toList();
    }

    /** Removing an absence does not bring cancelled shifts back: the supervisor reassigns on purpose. */
    @Transactional
    public void delete(Long id) {
        if (!absences.existsById(id)) {
            throw new NotFoundException("Absence", id);
        }
        absences.deleteById(id);
    }
}
