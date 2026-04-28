package com.project.edusync.em.model.service.serviceImpl;

import com.project.edusync.common.exception.emException.EdusyncException;
import com.project.edusync.common.exception.emException.ExamNotFoundException;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.em.model.dto.RequestDTO.ExamRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.ExamResponseDTO;
import com.project.edusync.em.model.entity.Exam;
import com.project.edusync.em.model.mapper.ExamMapper;
import com.project.edusync.em.model.repository.ExamControllerAssignmentRepository;
import com.project.edusync.em.model.repository.ExamRepository;
import com.project.edusync.em.model.service.ExamService;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
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

    private static final String ROLE_EXAM_CONTROLLER = "ROLE_EXAM_CONTROLLER";
    private static final Set<String> ADMIN_ROLES = Set.of("ROLE_ADMIN", "ROLE_SCHOOL_ADMIN", "ROLE_SUPER_ADMIN");

    private final ExamRepository examRepository;
    private final ExamMapper examMapper; // Injected by @RequiredArgsConstructor
    private final AuthUtil authUtil;
    private final StaffRepository staffRepository;
    private final ExamControllerAssignmentRepository assignmentRepository;

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
    public ExamResponseDTO getExamByUuid(UUID uuid) {
        Exam exam = findExamByUuid(uuid); // Use helper method
        ExamResponseDTO dto = examMapper.toResponseDTO(exam);
        assignmentRepository.findByExamIdAndActiveTrue(exam.getId()).ifPresent(assignment -> {
            dto.setAssignedControllerId(assignment.getStaff().getId());
            dto.setAssignedControllerName(assignment.getStaff().getUserProfile().getFirstName() + " " + assignment.getStaff().getUserProfile().getLastName());
            dto.setRemainingAttempts(3 - assignment.getChangeCount());
        });
        return dto;
    }

    /**
     * Retrieves a list of all exams.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ExamResponseDTO> getAllExams() {
        User user = authUtil.getCurrentUser();

        List<Exam> visibleExams;
        if (hasAnyRole(user, ADMIN_ROLES)) {
            visibleExams = examRepository.findAll();
        } else if (hasRole(user, ROLE_EXAM_CONTROLLER)) {
            Long staffId = staffRepository.findByUserProfile_User_Id(user.getId())
                .map(staff -> staff.getId())
                .orElse(null);
            if (staffId == null) {
                visibleExams = Collections.emptyList();
            } else {
                List<Long> assignedExamIds = assignmentRepository.findActiveExamIdsByStaffId(staffId);
                visibleExams = assignedExamIds.isEmpty()
                    ? Collections.emptyList()
                    : examRepository.findAllById(assignedExamIds);
            }
        } else {
            visibleExams = examRepository.findAll();
        }

        return visibleExams.stream()
                .map(exam -> {
                    ExamResponseDTO dto = examMapper.toResponseDTO(exam);
                    assignmentRepository.findByExamIdAndActiveTrue(exam.getId()).ifPresent(assignment -> {
                        dto.setAssignedControllerId(assignment.getStaff().getId());
                        dto.setAssignedControllerName(assignment.getStaff().getUserProfile().getFirstName() + " " + assignment.getStaff().getUserProfile().getLastName());
                        dto.setRemainingAttempts(3 - assignment.getChangeCount());
                    });
                    return dto;
                })
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
        User user = authUtil.getCurrentUser();

        List<Exam> visibleExams;
        if (hasAnyRole(user, ADMIN_ROLES)) {
            visibleExams = examRepository.findAll();
        } else if (hasRole(user, ROLE_EXAM_CONTROLLER)) {
            Long staffId = staffRepository.findByUserProfile_User_Id(user.getId())
                    .map(staff -> staff.getId())
                    .orElse(null);
            if (staffId == null) {
                visibleExams = Collections.emptyList();
            } else {
                List<Long> assignedExamIds = assignmentRepository.findActiveExamIdsByStaffId(staffId);
                visibleExams = assignedExamIds.isEmpty()
                        ? Collections.emptyList()
                        : examRepository.findAllById(assignedExamIds);
            }
        } else {
            visibleExams = examRepository.findAll();
        }

        return visibleExams.stream()
                .filter(e -> Boolean.TRUE.equals(e.getPublished()))
                .map(exam -> {
                    ExamResponseDTO dto = examMapper.toResponseDTO(exam);
                    assignmentRepository.findByExamIdAndActiveTrue(exam.getId()).ifPresent(assignment -> {
                        dto.setAssignedControllerId(assignment.getStaff().getId());
                        dto.setAssignedControllerName(assignment.getStaff().getUserProfile().getFirstName() + " " + assignment.getStaff().getUserProfile().getLastName());
                        dto.setRemainingAttempts(3 - assignment.getChangeCount());
                    });
                    return dto;
                })
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

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> roleName.equals(role.getName()));
    }

    private boolean hasAnyRole(User user, Set<String> roleNames) {
        return user.getRoles().stream().anyMatch(role -> roleNames.contains(role.getName()));
    }
}
