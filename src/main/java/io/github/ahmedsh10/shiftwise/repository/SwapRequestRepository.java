package io.github.ahmedsh10.shiftwise.repository;

import io.github.ahmedsh10.shiftwise.domain.SwapRequest;
import io.github.ahmedsh10.shiftwise.domain.SwapStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SwapRequestRepository extends JpaRepository<SwapRequest, Long> {

    List<SwapRequest> findByStatusInOrderByCreatedAtDesc(Collection<SwapStatus> statuses);

    List<SwapRequest> findAllByOrderByCreatedAtDesc();

    @Query("""
            select s from SwapRequest s
            where s.requester.id = :employeeId or s.target.id = :employeeId
            order by s.createdAt desc
            """)
    List<SwapRequest> findInvolving(@Param("employeeId") Long employeeId);

    boolean existsByAssignmentIdAndStatusIn(Long assignmentId, Collection<SwapStatus> statuses);
}
