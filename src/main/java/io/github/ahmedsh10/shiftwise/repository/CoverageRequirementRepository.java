package io.github.ahmedsh10.shiftwise.repository;

import io.github.ahmedsh10.shiftwise.domain.CoverageRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoverageRequirementRepository extends JpaRepository<CoverageRequirement, Long> {

    Optional<CoverageRequirement> findByAreaIdAndShiftTemplateId(Long areaId, Long shiftTemplateId);
}
