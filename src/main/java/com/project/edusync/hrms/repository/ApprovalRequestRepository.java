package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.ApprovalRequest;
import com.project.edusync.hrms.model.enums.ApprovalActionType;
import com.project.edusync.hrms.model.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    Optional<ApprovalRequest> findByUuid(UUID uuid);
    Optional<ApprovalRequest> findByEntityRefAndFinalStatus(UUID entityRef, ApprovalStatus status);
    List<ApprovalRequest> findByFinalStatus(ApprovalStatus status);
    List<ApprovalRequest> findByActionType(ApprovalActionType actionType);
    List<ApprovalRequest> findByFinalStatusAndActionType(ApprovalStatus status, ApprovalActionType actionType);
    List<ApprovalRequest> findAllByActiveTrue();
}

