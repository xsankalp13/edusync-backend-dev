package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.ApprovalChainConfig;
import com.project.edusync.hrms.model.enums.ApprovalActionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalChainConfigRepository extends JpaRepository<ApprovalChainConfig, Long> {
    Optional<ApprovalChainConfig> findByUuid(UUID uuid);
    List<ApprovalChainConfig> findByActionTypeAndActiveTrue(ApprovalActionType actionType);
    Optional<ApprovalChainConfig> findFirstByActionTypeAndActiveTrue(ApprovalActionType actionType);
    List<ApprovalChainConfig> findAllByActiveTrue();
}

