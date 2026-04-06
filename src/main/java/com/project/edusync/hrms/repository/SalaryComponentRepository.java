package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.SalaryComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalaryComponentRepository extends JpaRepository<SalaryComponent, Long> {

    List<SalaryComponent> findByActiveTrueOrderBySortOrderAscComponentCodeAsc();

    boolean existsByComponentCodeIgnoreCaseAndActiveTrue(String componentCode);

    boolean existsByComponentCodeIgnoreCaseAndActiveTrueAndIdNot(String componentCode, Long id);

    Optional<SalaryComponent> findByUuid(java.util.UUID uuid);
}

