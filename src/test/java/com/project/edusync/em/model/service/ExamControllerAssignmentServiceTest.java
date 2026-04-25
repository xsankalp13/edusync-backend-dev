package com.project.edusync.em.model.service;

import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.em.model.dto.request.ExamControllerAssignmentRequestDTO;
import com.project.edusync.em.model.entity.Exam;
import com.project.edusync.em.model.entity.ExamControllerAssignment;
import com.project.edusync.em.model.repository.ExamControllerAssignmentRepository;
import com.project.edusync.em.model.repository.ExamRepository;
import com.project.edusync.iam.model.entity.Role;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.RoleRepository;
import com.project.edusync.iam.repository.UserRepository;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamControllerAssignmentServiceTest {

    @Mock
    private ExamRepository examRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private AuthUtil authUtil;
    @Mock
    private ExamControllerAssignmentRepository assignmentRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ExamControllerAssignmentService service;

    @Test
    void assignController_addsRoleWhenMissing() {
        ExamControllerAssignmentRequestDTO request = new ExamControllerAssignmentRequestDTO();
        request.setExamId(10L);
        request.setStaffId(20L);

        Exam exam = new Exam();
        exam.setId(10L);
        Staff staff = buildStaffWithUser(false);

        Role role = new Role();
        role.setName("ROLE_EXAM_CONTROLLER");

        when(examRepository.findById(10L)).thenReturn(Optional.of(exam));
        when(staffRepository.findById(20L)).thenReturn(Optional.of(staff));
        when(roleRepository.findByName("ROLE_EXAM_CONTROLLER")).thenReturn(Optional.of(role));
        when(assignmentRepository.findByExamIdAndActiveTrue(10L)).thenReturn(Optional.empty());
        when(authUtil.getCurrentUserId()).thenReturn(99L);
        when(assignmentRepository.save(any(ExamControllerAssignment.class))).thenAnswer(invocation -> {
            ExamControllerAssignment assignment = invocation.getArgument(0);
            assignment.setId(1L);
            return assignment;
        });

        assertDoesNotThrow(() -> service.assignController(request));

        verify(userRepository).save(any(User.class));
        verify(assignmentRepository).save(any(ExamControllerAssignment.class));
    }

    @Test
    void assignController_skipsRoleSaveWhenAlreadyPresent() {
        ExamControllerAssignmentRequestDTO request = new ExamControllerAssignmentRequestDTO();
        request.setExamId(10L);
        request.setStaffId(20L);

        Exam exam = new Exam();
        exam.setId(10L);
        Staff staff = buildStaffWithUser(true);

        Role role = new Role();
        role.setName("ROLE_EXAM_CONTROLLER");

        when(examRepository.findById(10L)).thenReturn(Optional.of(exam));
        when(staffRepository.findById(20L)).thenReturn(Optional.of(staff));
        when(roleRepository.findByName("ROLE_EXAM_CONTROLLER")).thenReturn(Optional.of(role));
        when(assignmentRepository.findByExamIdAndActiveTrue(10L)).thenReturn(Optional.empty());
        when(authUtil.getCurrentUserId()).thenReturn(99L);
        when(assignmentRepository.save(any(ExamControllerAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> service.assignController(request));

        verify(userRepository, never()).save(any(User.class));
        verify(assignmentRepository).save(any(ExamControllerAssignment.class));
    }

    @Test
    void assignController_failsWhenStaffHasNoMappedUser() {
        ExamControllerAssignmentRequestDTO request = new ExamControllerAssignmentRequestDTO();
        request.setExamId(10L);
        request.setStaffId(20L);

        Exam exam = new Exam();
        exam.setId(10L);

        Staff staff = new Staff();
        staff.setId(20L);

        when(examRepository.findById(10L)).thenReturn(Optional.of(exam));
        when(staffRepository.findById(20L)).thenReturn(Optional.of(staff));

        assertThrows(ResourceNotFoundException.class, () -> service.assignController(request));

        verify(userRepository, never()).save(any(User.class));
        verify(assignmentRepository, never()).save(any(ExamControllerAssignment.class));
    }

    private Staff buildStaffWithUser(boolean includeControllerRole) {
        Staff staff = new Staff();
        staff.setId(20L);

        User user = new User();
        user.setId(55L);
        user.setRoles(new HashSet<>());

        Role teacherRole = new Role();
        teacherRole.setName("ROLE_TEACHER");
        user.getRoles().add(teacherRole);

        if (includeControllerRole) {
            Role controllerRole = new Role();
            controllerRole.setName("ROLE_EXAM_CONTROLLER");
            user.getRoles().add(controllerRole);
        }

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setFirstName("Meena");
        profile.setLastName("Reddy");

        staff.setUserProfile(profile);
        return staff;
    }
}

