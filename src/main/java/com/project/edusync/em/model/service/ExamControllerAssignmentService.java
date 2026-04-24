package com.project.edusync.em.model.service;

import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.em.model.dto.request.ExamControllerAssignmentRequestDTO;
import com.project.edusync.em.model.dto.response.ExamControllerAssignmentResponseDTO;
import com.project.edusync.em.model.entity.Exam;
import com.project.edusync.em.model.entity.ExamControllerAssignment;
import com.project.edusync.em.model.repository.ExamControllerAssignmentRepository;
import com.project.edusync.em.model.repository.ExamRepository;
import com.project.edusync.iam.model.entity.Role;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.RoleRepository;
import com.project.edusync.iam.repository.UserRepository;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExamControllerAssignmentService {

    private static final String EXAM_CONTROLLER_ROLE = "ROLE_EXAM_CONTROLLER";

    private final ExamRepository examRepository;
    private final StaffRepository staffRepository;
    private final AuthUtil authUtil;
    private final ExamControllerAssignmentRepository assignmentRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Transactional
    public ExamControllerAssignmentResponseDTO assignController(ExamControllerAssignmentRequestDTO requestDTO) {
        Exam exam = examRepository.findById(requestDTO.getExamId())
            .orElseThrow(() -> new ResourceNotFoundException("Exam", "id", requestDTO.getExamId()));

        Staff staff = staffRepository.findById(requestDTO.getStaffId())
            .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", requestDTO.getStaffId()));

        User mappedUser = resolveStaffUser(staff, requestDTO.getStaffId());
        Role examControllerRole = roleRepository.findByName(EXAM_CONTROLLER_ROLE)
            .orElseThrow(() -> new ResourceNotFoundException("Role", "name", EXAM_CONTROLLER_ROLE));

        boolean alreadyHasRole = mappedUser.getRoles().stream()
            .anyMatch(role -> EXAM_CONTROLLER_ROLE.equals(role.getName()));
        if (!alreadyHasRole) {
            mappedUser.getRoles().add(examControllerRole);
            userRepository.save(mappedUser);
        }

        ExamControllerAssignment assignment = assignmentRepository.findByExamIdAndActiveTrue(requestDTO.getExamId())
            .orElseGet(ExamControllerAssignment::new);

        if (assignment.getId() != null && !assignment.getStaff().getId().equals(staff.getId())) {
            if (assignment.getChangeCount() >= 3) {
                throw new IllegalArgumentException("Maximum assignment changes reached for this exam. Limit is 3.");
            }
            // Revoke logic for the previous staff
            Staff previousStaff = assignment.getStaff();
            long otherAssignments = assignmentRepository.countByStaffIdAndActiveTrueAndExamIdNot(previousStaff.getId(), exam.getId());
            if (otherAssignments == 0) {
                User previousUser = resolveStaffUser(previousStaff, previousStaff.getId());
                previousUser.getRoles().removeIf(role -> EXAM_CONTROLLER_ROLE.equals(role.getName()));
                userRepository.save(previousUser);
            }
            assignment.setChangeCount(assignment.getChangeCount() + 1);
        } else if (assignment.getId() == null) {
            assignment.setChangeCount(0);
        }

        assignment.setExam(exam);
        assignment.setStaff(staff);
        assignment.setActive(true);
        assignment.setAssignedByUserId(authUtil.getCurrentUserId());

        ExamControllerAssignment saved = assignmentRepository.save(assignment);
        String staffName = buildName(staff.getUserProfile().getFirstName(), staff.getUserProfile().getLastName());

        return ExamControllerAssignmentResponseDTO.builder()
            .assignmentId(saved.getId())
            .examId(saved.getExam().getId())
            .staffId(saved.getStaff().getId())
            .staffName(staffName)
            .assignedByUserId(saved.getAssignedByUserId())
            .assignedAt(saved.getAssignedAt())
            .remainingAttempts(3 - saved.getChangeCount())
            .build();
    }

    private String buildName(String firstName, String lastName) {
        String left = firstName == null ? "" : firstName.trim();
        String right = lastName == null ? "" : lastName.trim();
        return (left + " " + right).trim();
    }

    private User resolveStaffUser(Staff staff, Long staffId) {
        if (staff.getUser() != null) {
            return staff.getUser();
        }
        if (staff.getUserProfile() != null && staff.getUserProfile().getUser() != null) {
            return staff.getUserProfile().getUser();
        }
        throw new ResourceNotFoundException("User", "staffId", staffId);
    }
}

