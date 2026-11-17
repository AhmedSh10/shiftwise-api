package io.github.ahmedsh10.shiftwise.repository;

import io.github.ahmedsh10.shiftwise.domain.Employee;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "skills")
    List<Employee> findAllByOrderByFullNameAsc();

    @EntityGraph(attributePaths = "skills")
    Optional<Employee> findWithSkillsById(Long id);

    /**
     * SELECT ... FOR UPDATE on the employee row. Every write to someone's schedule takes this
     * lock first, so two concurrent requests for the same person run one after the other.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e where e.id = :id")
    Optional<Employee> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "skills")
    List<Employee> findByActiveTrueOrderByIdAsc();

    /** Locks every active employee, in id order so concurrent callers cannot deadlock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e where e.active = true order by e.id")
    List<Employee> lockAllActive();
}
