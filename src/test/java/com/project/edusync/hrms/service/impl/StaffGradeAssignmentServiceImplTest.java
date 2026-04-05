package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.hrms.dto.grade.StaffGradeAssignmentCreateDTO;
import com.project.edusync.hrms.dto.grade.StaffGradeAssignmentResponseDTO;
import com.project.edusync.hrms.model.entity.StaffGrade;
import com.project.edusync.hrms.model.entity.StaffGradeAssignment;
import com.project.edusync.hrms.model.enums.TeachingWing;
import com.project.edusync.hrms.repository.StaffGradeAssignmentRepository;
import com.project.edusync.hrms.repository.StaffGradeRepository;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffGradeAssignmentServiceImplTest {

    @Mock
    private StaffGradeAssignmentRepository assignmentRepository;
    @Mock
    private StaffGradeRepository staffGradeRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private AuthUtil authUtil;

    @InjectMocks
    private StaffGradeAssignmentServiceImpl service;

    @Test
    void assignClosesPreviousAssignmentAndCreatesNewOne() {
        Staff staff = buildStaff(101L, "Ravi", "Kumar");
        Staff approver = buildStaff(201L, "Admin", "User");
        StaffGrade oldGrade = buildGrade(1L, "PRT");
        StaffGrade newGrade = buildGrade(2L, "TGT");

        StaffGradeAssignment current = new StaffGradeAssignment();
        current.setId(500L);
        current.setStaff(staff);
        current.setGrade(oldGrade);
        current.setEffectiveFrom(LocalDate.of(2024, 4, 1));
        current.setEffectiveTo(null);
        current.setActive(true);

        when(staffRepository.findById(101L)).thenReturn(Optional.of(staff));
        when(staffGradeRepository.findById(2L)).thenReturn(Optional.of(newGrade));
        when(assignmentRepository.findByStaff_IdAndActiveTrueAndEffectiveToIsNull(101L)).thenReturn(Optional.of(current));
        when(authUtil.getCurrentUserId()).thenReturn(9001L);
        when(staffRepository.findByUserProfile_User_Id(9001L)).thenReturn(Optional.of(approver));
        when(assignmentRepository.save(any(StaffGradeAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StaffGradeAssignmentCreateDTO request = new StaffGradeAssignmentCreateDTO(
                "101",
                "2",
                LocalDate.of(2025, 4, 1),
                "ORD-2025-001",
                "Promotion"
        );

        StaffGradeAssignmentResponseDTO response = service.assign(request);

        assertEquals(LocalDate.of(2025, 3, 31), current.getEffectiveTo());
        assertEquals("TGT", response.gradeCode());
    }

    @Test
    void assignResolvesGradeByUuidReference() {
        Staff staff = buildStaff(101L, "Ravi", "Kumar");
        Staff approver = buildStaff(201L, "Admin", "User");
        StaffGrade oldGrade = buildGrade(1L, "PRT");
        StaffGrade newGrade = buildGrade(2L, "TGT");
        UUID gradeUuid = UUID.fromString("44444444-4444-4444-4444-444444444444");
        newGrade.setUuid(gradeUuid);

        StaffGradeAssignment current = new StaffGradeAssignment();
        current.setId(500L);
        current.setStaff(staff);
        current.setGrade(oldGrade);
        current.setEffectiveFrom(LocalDate.of(2024, 4, 1));
        current.setEffectiveTo(null);
        current.setActive(true);

        when(staffRepository.findById(101L)).thenReturn(Optional.of(staff));
        when(staffGradeRepository.findByUuid(gradeUuid)).thenReturn(Optional.of(newGrade));
        when(assignmentRepository.findByStaff_IdAndActiveTrueAndEffectiveToIsNull(101L)).thenReturn(Optional.of(current));
        when(authUtil.getCurrentUserId()).thenReturn(9001L);
        when(staffRepository.findByUserProfile_User_Id(9001L)).thenReturn(Optional.of(approver));
        when(assignmentRepository.save(any(StaffGradeAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StaffGradeAssignmentCreateDTO request = new StaffGradeAssignmentCreateDTO(
                "101",
                gradeUuid.toString(),
                LocalDate.of(2025, 4, 1),
                "ORD-2025-UUID",
                "Promotion"
        );

        StaffGradeAssignmentResponseDTO response = service.assign(request);

        assertEquals("TGT", response.gradeCode());
        verify(staffGradeRepository).findByUuid(gradeUuid);
        verify(staffGradeRepository, never()).findById(2L);
    }

    private Staff buildStaff(Long id, String firstName, String lastName) {
        Staff staff = new Staff();
        staff.setId(id);
        staff.setActive(true);

        UserProfile profile = new UserProfile();
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        staff.setUserProfile(profile);
        return staff;
    }

    private StaffGrade buildGrade(Long id, String code) {
        StaffGrade grade = new StaffGrade();
        grade.setId(id);
        grade.setActive(true);
        grade.setGradeCode(code);
        grade.setGradeName(code + " Name");
        grade.setTeachingWing(TeachingWing.ALL);
        grade.setPayBandMin(new BigDecimal("10000"));
        grade.setPayBandMax(new BigDecimal("50000"));
        grade.setSortOrder(1);
        return grade;
    }
}

