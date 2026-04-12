package com.project.edusync.em.model.service.serviceImpl;

import com.project.edusync.common.exception.emException.EdusyncException;
import com.project.edusync.common.exception.emException.ExamNotFoundException;
import com.project.edusync.em.model.dto.RequestDTO.ExamRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.ExamResponseDTO;
import com.project.edusync.em.model.entity.Exam;
import com.project.edusync.em.model.mapper.ExamMapper;
import com.project.edusync.em.model.repository.ExamRepository;
import com.project.edusync.em.model.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the ExamService.
 * This class contains the business logic for managing Exams.
 */
@Service
@RequiredArgsConstructor // Injects final fields via constructor (examRepository, examMapper)
@Transactional // All public methods will be transactional by default
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final ExamMapper examMapper; // Injected by @RequiredArgsConstructor

    /**
     * Creates a new exam based on the DTO.
     */
    @Override
    public ExamResponseDTO createExam(ExamRequestDTO requestDTO) {
        // 1. Convert DTO to Entity
        Exam exam = examMapper.toEntity(requestDTO);

        // 2. Set business logic defaults
        exam.setPublished(false);

        // 3. Save to database
        Exam savedExam = examRepository.save(exam);

        // 4. Convert saved Entity back to Response DTO and return
        return examMapper.toResponseDTO(savedExam);
    }

    /**
     * Retrieves a single exam by its public UUID.
     */
    @Override
    @Transactional(readOnly = true) // Optimize for read-only query
    public ExamResponseDTO getExamByUuid(UUID uuid) {
        Exam exam = findExamByUuid(uuid); // Use helper method
        return examMapper.toResponseDTO(exam);
    }

    /**
     * Retrieves a list of all exams.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ExamResponseDTO> getAllExams() {
        return examRepository.findAll().stream()
                .map(examMapper::toResponseDTO) // Uses the mapper for each item
                .collect(Collectors.toList());
    }

    /**
     * Updates an existing exam. The mapper handles null-checks (PATCH behavior).
     */
    @Override
    public ExamResponseDTO updateExam(UUID uuid, ExamRequestDTO requestDTO) {
        Exam existingExam = findExamByUuid(uuid);

        // 1. Use the mapper to update only non-null fields from the DTO
        // This is the clean "PATCH" logic
        examMapper.updateEntityFromDto(requestDTO, existingExam);

        // 2. Save the updated entity
        Exam updatedExam = examRepository.save(existingExam);

        // 3. Return the updated DTO
        return examMapper.toResponseDTO(updatedExam);
    }

    /**
     * Marks an exam as "published".
     */
    @Override
    public ExamResponseDTO publishExam(UUID uuid, Boolean published) {
        Exam exam = findExamByUuid(uuid);
        if (published != null) {
            exam.setPublished(published);
        }
        Exam publishedExam = examRepository.save(exam);
        return examMapper.toResponseDTO(publishedExam);
    }

    /**
     * Publishes the timetable to students' dashboards.
     */
    @Override
    public ExamResponseDTO publishTimetable(UUID uuid) {
        Exam exam = findExamByUuid(uuid);
        exam.setTimetablePublished(true);
        // Also ensure the exam itself is published
        exam.setPublished(true);
        Exam saved = examRepository.save(exam);
        return examMapper.toResponseDTO(saved);
    }

    /**
     * Deletes an exam if it has no schedules.
     */
    @Override
    public void deleteExam(UUID uuid) {
        Exam exam = findExamByUuid(uuid);

        // Business logic: Check for dependencies before deleting
        if (exam.getSchedules() != null && !exam.getSchedules().isEmpty()) {
            throw new EdusyncException(
                    "EM-409", // Custom error code
                    "Cannot delete exam with active schedules. Please delete schedules first.",
                    HttpStatus.CONFLICT // 409 Conflict
            );
        }

        examRepository.delete(exam);
    }

    /**
     * Retrieves a list of upcoming exams (only published).
     */
    @Override
    @Transactional(readOnly = true)
    public List<ExamResponseDTO> getUpcomingExams() {
        return examRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getPublished()))
                .map(examMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // --- Helper Method ---

    /**
     * Private helper to find an Exam by UUID or throw a 404 error.
     */
    private Exam findExamByUuid(UUID uuid) {
        // Uses the new custom exception
        return examRepository.findByUuid(uuid)
                .orElseThrow(() -> new ExamNotFoundException(uuid));
    }
}