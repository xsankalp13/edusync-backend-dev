package com.project.edusync.em.model.service.serviceImpl;

import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.em.model.dto.ResponseDTO.ExamResponseDTO;
import com.project.edusync.em.model.entity.Exam;
import com.project.edusync.em.model.mapper.ExamMapper;
import com.project.edusync.em.model.repository.ExamControllerAssignmentRepository;
import com.project.edusync.em.model.repository.ExamRepository;
import com.project.edusync.iam.model.entity.Role;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamServiceImplTest {

    @Mock
    private ExamRepository examRepository;
    @Mock
    private ExamMapper examMapper;
    @Mock
    private AuthUtil authUtil;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private ExamControllerAssignmentRepository assignmentRepository;

    @InjectMocks
    private ExamServiceImpl examService;

    @Test
    void getAllExams_returnsAllForAdmin() {
        User admin = userWithRoles(7L, "ROLE_ADMIN");
        Exam exam1 = exam(1L);
        Exam exam2 = exam(2L);

        when(authUtil.getCurrentUser()).thenReturn(admin);
        when(examRepository.findAll()).thenReturn(List.of(exam1, exam2));
        when(examMapper.toResponseDTO(exam1)).thenReturn(new ExamResponseDTO());
        when(examMapper.toResponseDTO(exam2)).thenReturn(new ExamResponseDTO());

        List<ExamResponseDTO> result = examService.getAllExams();

        assertEquals(2, result.size());
        verify(examRepository).findAll();
        verify(assignmentRepository, never()).findActiveExamIdsByStaffId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void getAllExams_returnsAssignedOnlyForExamController() {
        User controller = userWithRoles(11L, "ROLE_EXAM_CONTROLLER");
        Staff staff = new Staff();
        staff.setId(55L);

        Exam assignedExam = exam(3L);

        when(authUtil.getCurrentUser()).thenReturn(controller);
        when(staffRepository.findByUserProfile_User_Id(11L)).thenReturn(Optional.of(staff));
        when(assignmentRepository.findActiveExamIdsByStaffId(55L)).thenReturn(List.of(3L));
        when(examRepository.findAllById(List.of(3L))).thenReturn(List.of(assignedExam));
        when(examMapper.toResponseDTO(assignedExam)).thenReturn(new ExamResponseDTO());

        List<ExamResponseDTO> result = examService.getAllExams();

        assertEquals(1, result.size());
        verify(examRepository, never()).findAll();
    }

    @Test
    void getAllExams_returnsEmptyWhenControllerHasNoStaffMapping() {
        User controller = userWithRoles(11L, "ROLE_EXAM_CONTROLLER");

        when(authUtil.getCurrentUser()).thenReturn(controller);
        when(staffRepository.findByUserProfile_User_Id(11L)).thenReturn(Optional.empty());

        List<ExamResponseDTO> result = examService.getAllExams();

        assertEquals(0, result.size());
        verify(examRepository, never()).findAll();
        verify(examRepository, never()).findAllById(org.mockito.ArgumentMatchers.anyCollection());
    }

    private User userWithRoles(Long id, String... roleNames) {
        User user = new User();
        user.setId(id);
        Set<Role> roles = new java.util.HashSet<>();
        for (String roleName : roleNames) {
            Role role = new Role();
            role.setName(roleName);
            roles.add(role);
        }
        user.setRoles(roles);
        return user;
    }

    private Exam exam(Long id) {
        Exam exam = new Exam();
        exam.setId(id);
        return exam;
    }
}

