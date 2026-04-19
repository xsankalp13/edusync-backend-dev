package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.AppraisalCycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppraisalCycleRepository extends JpaRepository<AppraisalCycle, Long> {
    Optional<AppraisalCycle> findByUuid(UUID uuid);
    List<AppraisalCycle> findAllByActiveTrue();
}

