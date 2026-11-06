package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.domain.Area;
import io.github.ahmedsh10.shiftwise.domain.Employee;
import io.github.ahmedsh10.shiftwise.repository.AreaRepository;
import io.github.ahmedsh10.shiftwise.repository.EmployeeRepository;
import io.github.ahmedsh10.shiftwise.web.dto.CreateEmployeeRequest;
import io.github.ahmedsh10.shiftwise.web.dto.EmployeeResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class EmployeeService {

    private final EmployeeRepository employees;
    private final AreaRepository areas;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employees, AreaRepository areas, PasswordEncoder passwordEncoder) {
        this.employees = employees;
        this.areas = areas;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> list() {
        return employees.findAllByOrderByFullNameAsc().stream().map(EmployeeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(Long id) {
        return EmployeeResponse.from(load(id));
    }

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        if (employees.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email " + request.email() + " is already used");
        }
        Employee employee = new Employee(request.fullName(), request.email().toLowerCase(),
                passwordEncoder.encode(request.password()), request.role(), request.contractHoursPerWeek());
        employee.setSkills(resolveAreas(request.skillAreaIds()));
        return EmployeeResponse.from(employees.save(employee));
    }

    @Transactional
    public EmployeeResponse updateSkills(Long id, Set<Long> areaIds) {
        Employee employee = load(id);
        employee.setSkills(resolveAreas(areaIds));
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public EmployeeResponse setActive(Long id, boolean active) {
        Employee employee = load(id);
        employee.setActive(active);
        return EmployeeResponse.from(employee);
    }

    private Employee load(Long id) {
        return employees.findWithSkillsById(id).orElseThrow(() -> new NotFoundException("Employee", id));
    }

    private Set<Area> resolveAreas(Set<Long> areaIds) {
        Set<Area> result = new HashSet<>();
        if (areaIds == null) {
            return result;
        }
        for (Long areaId : areaIds) {
            result.add(areas.findById(areaId).orElseThrow(() -> new NotFoundException("Area", areaId)));
        }
        return result;
    }
}
