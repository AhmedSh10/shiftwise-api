package io.github.ahmedsh10.shiftwise.repository;

import io.github.ahmedsh10.shiftwise.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

    List<Assignment> findByWorkDateBetweenOrderByWorkDateAscIdAsc(LocalDate from, LocalDate to);

    List<Assignment> findByWorkDate(LocalDate workDate);

    long countByAreaIdAndShiftTemplateIdAndWorkDate(Long areaId, Long shiftTemplateId, LocalDate workDate);

    List<Assignment> findByEmployeeIdAndWorkDateBetween(Long employeeId, LocalDate from, LocalDate to);

    List<Assignment> findByWorkDateBetween(LocalDate from, LocalDate to);
}
