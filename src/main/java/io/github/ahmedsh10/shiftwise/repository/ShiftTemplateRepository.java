package io.github.ahmedsh10.shiftwise.repository;

import io.github.ahmedsh10.shiftwise.domain.ShiftTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, Long> {

    Optional<ShiftTemplate> findByCode(String code);
}
