package com.project.edusync.em.model.service;

import com.project.edusync.em.model.dto.RequestDTO.ExamRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.ExamResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ExamService {

    ExamResponseDTO createExam(ExamRequestDTO requestDTO);

    ExamResponseDTO getExamByUuid(UUID uuid);

    List<ExamResponseDTO> getAllExams();

    ExamResponseDTO updateExam(UUID uuid, ExamRequestDTO requestDTO);

    ExamResponseDTO publishExam(UUID uuid);

    void deleteExam(UUID uuid);
}
