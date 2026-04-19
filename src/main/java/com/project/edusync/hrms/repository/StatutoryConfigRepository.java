package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.StatutoryConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StatutoryConfigRepository extends JpaRepository<StatutoryConfig, Long> {
    Optional<StatutoryConfig> findByFinancialYearAndActiveTrue(String financialYear);
    Optional<StatutoryConfig> findByUuid(UUID uuid);
}

