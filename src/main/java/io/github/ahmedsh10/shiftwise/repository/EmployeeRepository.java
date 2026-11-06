package io.github.ahmedsh10.shiftwise.repository;

import io.github.ahmedsh10.shiftwise.domain.Employee;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "skills")
    List<Employee> findAllByOrderByFullNameAsc();

    @EntityGraph(attributePaths = "skills")
    Optional<Employee> findWithSkillsById(Long id);
}
