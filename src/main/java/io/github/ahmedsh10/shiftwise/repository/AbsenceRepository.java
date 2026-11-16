package io.github.ahmedsh10.shiftwise.repository;

import io.github.ahmedsh10.shiftwise.domain.Absence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    @Query("""
            select count(a) > 0 from Absence a
            where a.employee.id = :employeeId and a.startDate <= :date and a.endDate >= :date
            """)
    boolean isAbsentOn(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);

    /** Absences that overlap the [from, to] range. */
    @Query("""
            select a from Absence a
            where a.startDate <= :to and a.endDate >= :from
            order by a.startDate, a.id
            """)
    List<Absence> findOverlapping(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
