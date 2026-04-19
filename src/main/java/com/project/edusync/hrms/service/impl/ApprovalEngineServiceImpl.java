package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.hrms.dto.approval.*;
import com.project.edusync.hrms.model.entity.*;
import com.project.edusync.hrms.model.enums.*;
import com.project.edusync.hrms.repository.*;
import com.project.edusync.hrms.service.ApprovalEngineService;
import com.project.edusync.hrms.service.event.ApprovalCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApprovalEngineServiceImpl implements ApprovalEngineService {

    private final ApprovalChainConfigRepository chainConfigRepository;
    private final ApprovalChainStepRepository chainStepRepository;
    private final ApprovalRequestRepository requestRepository;
    private final ApprovalStepRecordRepository stepRecordRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final String SCHOOL_ADMIN_ROLE = "ROLE_SCHOOL_ADMIN";

    @Override
    @Transactional
    public ApprovalChainConfigResponseDTO createChain(ApprovalChainConfigCreateDTO dto) {
        ApprovalChainConfig config = new ApprovalChainConfig();
        config.setActionType(dto.actionType());
        config.setChainName(dto.chainName());
        config.setActive(true);
        ApprovalChainConfig saved = chainConfigRepository.save(config);
        saveSteps(saved, dto.steps());
        return toConfigResponse(saved, dto.steps());
    }

    @Override
    @Transactional
    public ApprovalChainConfigResponseDTO updateChain(UUID uuid, ApprovalChainConfigCreateDTO dto) {
        ApprovalChainConfig config = chainConfigRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalChainConfig not found: " + uuid));
        config.setChainName(dto.chainName());
        config.setActionType(dto.actionType());
        chainStepRepository.deleteByChainConfig_Id(config.getId());
        ApprovalChainConfig saved = chainConfigRepository.save(config);
        saveSteps(saved, dto.steps());
        return toConfigResponse(saved, dto.steps());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalChainConfigResponseDTO> listChains(ApprovalActionType actionType) {
        List<ApprovalChainConfig> configs = actionType != null
                ? chainConfigRepository.findByActionTypeAndActiveTrue(actionType)
                : chainConfigRepository.findAllByActiveTrue();
        return configs.stream().map(c -> {
            List<ApprovalChainStepDTO> steps = chainStepRepository
                    .findByChainConfig_IdOrderByStepOrderAsc(c.getId())
                    .stream().map(s -> new ApprovalChainStepDTO(s.getStepOrder(), s.getApproverRole(), s.getStepLabel()))
                    .toList();
            return toConfigResponse(c, steps);
        }).toList();
    }

    @Override
    @Transactional
    public void deleteChain(UUID uuid) {
        ApprovalChainConfig config = chainConfigRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalChainConfig not found: " + uuid));
        config.setActive(false);
        chainConfigRepository.save(config);
    }

    @Override
    @Transactional
    public Optional<ApprovalRequest> submitForApproval(ApprovalActionType actionType, String entityType,
                                                        UUID entityRef, UUID requestedByRef) {
        Optional<ApprovalChainConfig> chainOpt = chainConfigRepository.findFirstByActionTypeAndActiveTrue(actionType);
        if (chainOpt.isEmpty()) return Optional.empty();

        ApprovalChainConfig chain = chainOpt.get();
        List<ApprovalChainStep> steps = chainStepRepository.findByChainConfig_IdOrderByStepOrderAsc(chain.getId());
        if (steps.isEmpty()) return Optional.empty();

        ApprovalRequest request = new ApprovalRequest();
        request.setActionType(actionType);
        request.setEntityType(entityType);
        request.setEntityRef(entityRef);
        request.setRequestedByRef(requestedByRef);
        request.setRequestedAt(LocalDateTime.now());
        request.setCurrentStepOrder(steps.get(0).getStepOrder());
        request.setFinalStatus(ApprovalStatus.PENDING);
        ApprovalRequest saved = requestRepository.save(request);

        for (ApprovalChainStep step : steps) {
            ApprovalStepRecord record = new ApprovalStepRecord();
            record.setRequest(saved);
            record.setStepOrder(step.getStepOrder());
            record.setApproverRole(step.getApproverRole());
            record.setStatus(ApprovalStatus.PENDING);
            stepRecordRepository.save(record);
        }
        return Optional.of(saved);
    }

    @Override
    @Transactional
    public ApprovalRequest advance(UUID requestUuid, UUID actorRef, String actorRole,
                                    String remarks, boolean approved) {
        ApprovalRequest request = requestRepository.findByUuid(requestUuid)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRequest not found: " + requestUuid));

        if (request.getFinalStatus() != ApprovalStatus.PENDING) {
            throw new EdusyncException("This approval request is already " + request.getFinalStatus(), HttpStatus.BAD_REQUEST);
        }

        boolean isAdmin = SCHOOL_ADMIN_ROLE.equals(actorRole);
        List<ApprovalStepRecord> stepRecords = stepRecordRepository.findByRequest_IdOrderByStepOrderAsc(request.getId());

        if (isAdmin) {
            // Bypass: approve all pending steps
            for (ApprovalStepRecord sr : stepRecords) {
                if (sr.getStatus() == ApprovalStatus.PENDING) {
                    sr.setStatus(approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
                    sr.setApproverRef(actorRef);
                    sr.setRemarks(remarks);
                    sr.setActedAt(LocalDateTime.now());
                    stepRecordRepository.save(sr);
                }
            }
        } else {
            ApprovalStepRecord currentStep = stepRecords.stream()
                    .filter(sr -> sr.getStepOrder() == request.getCurrentStepOrder()
                            && sr.getStatus() == ApprovalStatus.PENDING)
                    .findFirst()
                    .orElseThrow(() -> new EdusyncException("No pending step found at order " + request.getCurrentStepOrder(), HttpStatus.BAD_REQUEST));

            if (!currentStep.getApproverRole().equals(actorRole)) {
                throw new EdusyncException("Your role does not match the required approver role for this step", HttpStatus.FORBIDDEN);
            }

            currentStep.setStatus(approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
            currentStep.setApproverRef(actorRef);
            currentStep.setRemarks(remarks);
            currentStep.setActedAt(LocalDateTime.now());
            stepRecordRepository.save(currentStep);
        }

        if (!approved) {
            request.setFinalStatus(ApprovalStatus.REJECTED);
            request.setCompletedAt(LocalDateTime.now());
        } else {
            boolean allApproved = stepRecords.stream().allMatch(sr -> sr.getStatus() == ApprovalStatus.APPROVED);
            if (allApproved) {
                request.setFinalStatus(ApprovalStatus.APPROVED);
                request.setCompletedAt(LocalDateTime.now());
                eventPublisher.publishEvent(new ApprovalCompletedEvent(request.getActionType(), request.getEntityRef()));
            } else {
                // Advance to next pending step
                stepRecords.stream()
                        .filter(sr -> sr.getStatus() == ApprovalStatus.PENDING)
                        .min(java.util.Comparator.comparingInt(ApprovalStepRecord::getStepOrder))
                        .ifPresent(next -> request.setCurrentStepOrder(next.getStepOrder()));
            }
        }
        return requestRepository.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalRequestDetailDTO getRequestDetail(UUID uuid) {
        ApprovalRequest request = requestRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRequest not found: " + uuid));
        List<ApprovalStepRecordDTO> steps = stepRecordRepository.findByRequest_IdOrderByStepOrderAsc(request.getId())
                .stream().map(sr -> new ApprovalStepRecordDTO(sr.getStepOrder(), sr.getApproverRole(),
                        sr.getApproverRef(), sr.getStatus(), sr.getRemarks(), sr.getActedAt()))
                .toList();
        return new ApprovalRequestDetailDTO(request.getUuid(), request.getActionType(), request.getEntityType(),
                request.getEntityRef(), request.getRequestedByRef(), request.getRequestedAt(),
                request.getCurrentStepOrder(), request.getFinalStatus(), request.getCompletedAt(), steps);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalRequestDetailDTO> listRequests(ApprovalStatus status, ApprovalActionType actionType) {
        List<ApprovalRequest> requests;
        if (status != null && actionType != null) {
            requests = requestRepository.findByFinalStatusAndActionType(status, actionType);
        } else if (status != null) {
            requests = requestRepository.findByFinalStatus(status);
        } else if (actionType != null) {
            requests = requestRepository.findByActionType(actionType);
        } else {
            requests = requestRepository.findAllByActiveTrue();
        }
        return requests.stream().map(r -> {
            List<ApprovalStepRecordDTO> steps = stepRecordRepository.findByRequest_IdOrderByStepOrderAsc(r.getId())
                    .stream().map(sr -> new ApprovalStepRecordDTO(sr.getStepOrder(), sr.getApproverRole(),
                            sr.getApproverRef(), sr.getStatus(), sr.getRemarks(), sr.getActedAt()))
                    .toList();
            return new ApprovalRequestDetailDTO(r.getUuid(), r.getActionType(), r.getEntityType(),
                    r.getEntityRef(), r.getRequestedByRef(), r.getRequestedAt(),
                    r.getCurrentStepOrder(), r.getFinalStatus(), r.getCompletedAt(), steps);
        }).toList();
    }

    private void saveSteps(ApprovalChainConfig config, List<ApprovalChainStepDTO> steps) {
        for (ApprovalChainStepDTO stepDto : steps) {
            ApprovalChainStep step = new ApprovalChainStep();
            step.setChainConfig(config);
            step.setStepOrder(stepDto.stepOrder());
            step.setApproverRole(stepDto.approverRole());
            step.setStepLabel(stepDto.stepLabel());
            chainStepRepository.save(step);
        }
    }

    private ApprovalChainConfigResponseDTO toConfigResponse(ApprovalChainConfig config, List<ApprovalChainStepDTO> steps) {
        return new ApprovalChainConfigResponseDTO(config.getUuid(), config.getActionType(),
                config.getChainName(), config.isActive(), steps, config.getCreatedAt());
    }
}

