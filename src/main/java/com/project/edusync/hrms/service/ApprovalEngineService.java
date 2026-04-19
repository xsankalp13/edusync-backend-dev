package com.project.edusync.hrms.service;

import com.project.edusync.hrms.dto.approval.ApprovalChainConfigCreateDTO;
import com.project.edusync.hrms.dto.approval.ApprovalChainConfigResponseDTO;
import com.project.edusync.hrms.dto.approval.ApprovalRequestDetailDTO;
import com.project.edusync.hrms.model.entity.ApprovalRequest;
import com.project.edusync.hrms.model.enums.ApprovalActionType;
import com.project.edusync.hrms.model.enums.ApprovalStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalEngineService {

    ApprovalChainConfigResponseDTO createChain(ApprovalChainConfigCreateDTO dto);
    ApprovalChainConfigResponseDTO updateChain(UUID uuid, ApprovalChainConfigCreateDTO dto);
    List<ApprovalChainConfigResponseDTO> listChains(ApprovalActionType actionType);
    void deleteChain(UUID uuid);

    Optional<ApprovalRequest> submitForApproval(ApprovalActionType actionType, String entityType, UUID entityRef, UUID requestedByRef);

    ApprovalRequest advance(UUID requestUuid, UUID actorRef, String actorRole, String remarks, boolean approved);

    ApprovalRequestDetailDTO getRequestDetail(UUID uuid);
    List<ApprovalRequestDetailDTO> listRequests(ApprovalStatus status, ApprovalActionType actionType);
}

