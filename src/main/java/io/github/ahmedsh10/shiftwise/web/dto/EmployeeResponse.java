package io.github.ahmedsh10.shiftwise.web.dto;

import io.github.ahmedsh10.shiftwise.domain.Area;
import io.github.ahmedsh10.shiftwise.domain.Employee;

import java.util.List;

public record EmployeeResponse(Long id, String fullName, String email, String role,
                               int contractHoursPerWeek, boolean active, List<String> skills) {

    public static EmployeeResponse from(Employee e) {
        List<String> skills = e.getSkills().stream().map(Area::getCode).sorted().toList();
        return new EmployeeResponse(e.getId(), e.getFullName(), e.getEmail(), e.getRole().name(),
                e.getContractHoursPerWeek(), e.isActive(), skills);
    }
}
