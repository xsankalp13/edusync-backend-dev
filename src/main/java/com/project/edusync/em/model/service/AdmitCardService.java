package com.project.edusync.em.model.service;

import com.project.edusync.em.model.dto.ResponseDTO.AdmitCardGenerationProgressDTO;
import com.project.edusync.em.model.dto.ResponseDTO.AdmitCardGenerationResponseDTO;
import com.project.edusync.em.model.dto.ResponseDTO.ScheduleAdmitCardStatusDTO;

import java.util.List;
import java.util.UUID;

public interface AdmitCardService {

    AdmitCardGenerationResponseDTO generateAdmitCards(UUID examUuid);

    AdmitCardGenerationResponseDTO generateAdmitCardsForSchedule(UUID examUuid, Long scheduleId);

    byte[] generateBatchAdmitCardsPdf(UUID examUuid, Long scheduleId);

    List<ScheduleAdmitCardStatusDTO> getAdmitCardStatusByExam(UUID examUuid);

    AdmitCardGenerationProgressDTO getGenerationProgress(UUID examUuid);

    com.project.edusync.em.model.dto.ResponseDTO.AdmitCardResponseDTO getStudentAdmitCard(UUID examUuid);
}
