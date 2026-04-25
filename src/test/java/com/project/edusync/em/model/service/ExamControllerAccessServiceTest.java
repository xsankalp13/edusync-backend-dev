package com.project.edusync.em.model.service;

import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.em.model.repository.ExamControllerAssignmentRepository;
import com.project.edusync.em.model.repository.ExamRepository;
import com.project.edusync.em.model.repository.ExamScheduleRepository;
import com.project.edusync.em.model.repository.InvigilationRepository;
import com.project.edusync.iam.model.entity.Role;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamControllerAccessServiceTest {

    @Mock
    private AuthUtil authUtil;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private ExamRepository examRepository;
    @Mock
    private ExamScheduleRepository examScheduleRepository;
    @Mock
    private InvigilationRepository invigilationRepository;
    @Mock
    private ExamControllerAssignmentRepository assignmentRepository;

    @InjectMocks
    private ExamControllerAccessService accessService;

    @Test
    void canAccessExam_allowsAdminWithoutAssignment() {
        User admin = new User();
        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");
        admin.setRoles(Set.of(adminRole));

        when(authUtil.getCurrentUser()).thenReturn(admin);

        assertTrue(accessService.canAccessExam(10L));
    }

    @Test
    void canAccessExam_requiresAssignmentForExamController() {
        User controller = new User();
        controller.setId(11L);
        Role controllerRole = new Role();
        controllerRole.setName("ROLE_EXAM_CONTROLLER");
        controller.setRoles(Set.of(controllerRole));

        Staff staff = new Staff();
        staff.setId(7L);

        when(authUtil.getCurrentUser()).thenReturn(controller);
        when(staffRepository.findByUserProfile_User_Id(11L)).thenReturn(Optional.of(staff));
        when(assignmentRepository.existsByExamIdAndStaffIdAndActiveTrue(10L, 7L)).thenReturn(true);

        assertTrue(accessService.canAccessExam(10L));
    }

    @Test
    void canAccessExamUuid_deniesWhenExamMissing() {
        UUID examUuid = UUID.randomUUID();
        when(examRepository.findIdByUuid(examUuid)).thenReturn(Optional.empty());

        assertFalse(accessService.canAccessExamUuid(examUuid));
    }
}

