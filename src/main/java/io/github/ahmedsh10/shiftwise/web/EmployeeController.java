package io.github.ahmedsh10.shiftwise.web;

import io.github.ahmedsh10.shiftwise.repository.EmployeeRepository;
import io.github.ahmedsh10.shiftwise.service.EmployeeService;
import io.github.ahmedsh10.shiftwise.service.NotFoundException;
import io.github.ahmedsh10.shiftwise.web.dto.CreateEmployeeRequest;
import io.github.ahmedsh10.shiftwise.web.dto.EmployeeResponse;
import io.github.ahmedsh10.shiftwise.web.dto.UpdateSkillsRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /** The signed-in user's own profile. */
    @GetMapping("/me")
    public EmployeeResponse me(@AuthenticationPrincipal Jwt jwt) {
        Number id = jwt.getClaim("employeeId");
        if (id == null) {
            throw new NotFoundException("Employee", jwt.getSubject());
        }
        return employeeService.get(id.longValue());
    }

    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public List<EmployeeResponse> list() {
        return employeeService.list();
    }

    @GetMapping("/employees/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public EmployeeResponse get(@PathVariable Long id) {
        return employeeService.get(id);
    }

    @PostMapping("/employees")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeResponse create(@Valid @RequestBody CreateEmployeeRequest request) {
        return employeeService.create(request);
    }

    @PutMapping("/employees/{id}/skills")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public EmployeeResponse updateSkills(@PathVariable Long id, @Valid @RequestBody UpdateSkillsRequest request) {
        return employeeService.updateSkills(id, request.areaIds());
    }

    @PatchMapping("/employees/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeResponse setActive(@PathVariable Long id, @RequestParam boolean value) {
        return employeeService.setActive(id, value);
    }
}
