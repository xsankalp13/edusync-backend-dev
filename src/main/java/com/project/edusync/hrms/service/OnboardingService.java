package com.project.edusync.hrms.service;

import com.project.edusync.hrms.dto.onboarding.OnboardingDTOs.*;
import com.project.edusync.hrms.model.enums.OnboardingStatus;

import java.util.List;
import java.util.UUID;

public interface OnboardingService {
    TemplateResponseDTO createTemplate(TemplateCreateDTO dto);
    TemplateResponseDTO updateTemplate(UUID uuid, TemplateCreateDTO dto);
    List<TemplateResponseDTO> listTemplates();
    void deleteTemplate(UUID uuid);

    RecordResponseDTO createRecord(RecordCreateDTO dto);
    List<RecordResponseDTO> listRecords(String staffRef, OnboardingStatus status);
    RecordResponseDTO getRecord(UUID uuid);
    RecordResponseDTO completeTask(UUID recordUuid, Long taskId, String actorRef, String remarks);
    RecordResponseDTO getStaffOnboarding(String staffRef);
}

