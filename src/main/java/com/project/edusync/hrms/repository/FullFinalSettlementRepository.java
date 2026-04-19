package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.FullFinalSettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FullFinalSettlementRepository extends JpaRepository<FullFinalSettlement, Long> {
    Optional<FullFinalSettlement> findByExitRequest_Id(Long exitRequestId);
    Optional<FullFinalSettlement> findByExitRequest_Uuid(UUID exitRequestUuid);
}

