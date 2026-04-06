package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.hrms.dto.leave.LeaveTypeConfigCreateDTO;
import com.project.edusync.hrms.dto.leave.LeaveTypeConfigResponseDTO;
import com.project.edusync.hrms.model.entity.LeaveTypeConfig;
import com.project.edusync.hrms.repository.LeaveTypeConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveTypeConfigServiceImplTest {

    @Mock
    private LeaveTypeConfigRepository leaveTypeConfigRepository;

    @InjectMocks
    private LeaveTypeConfigServiceImpl service;

    @Test
    void createThrowsConflictForDuplicateCode() {
        LeaveTypeConfigCreateDTO dto = new LeaveTypeConfigCreateDTO(
                "CL",
                "Casual Leave",
                null,
                12,
                false,
                0,
                false,
                0,
                null,
                false,
                null,
                true,
                null,
                null,
                1
        );

        when(leaveTypeConfigRepository.existsByLeaveCodeIgnoreCaseAndActiveTrue("CL")).thenReturn(true);

        assertThrows(EdusyncException.class, () -> service.create(dto));
    }

    @Test
    void createNormalizesCodeAndGrades() {
        LeaveTypeConfigCreateDTO dto = new LeaveTypeConfigCreateDTO(
                " el ",
                "Earned Leave",
                null,
                15,
                true,
                30,
                false,
                7,
                20,
                false,
                null,
                true,
                null,
                Set.of(" pgt ", "tgt"),
                3
        );

        when(leaveTypeConfigRepository.existsByLeaveCodeIgnoreCaseAndActiveTrue("EL")).thenReturn(false);
        when(leaveTypeConfigRepository.save(any(LeaveTypeConfig.class))).thenAnswer(invocation -> {
            LeaveTypeConfig saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        LeaveTypeConfigResponseDTO response = service.create(dto);

        assertEquals("EL", response.leaveCode());
        assertEquals(2, response.applicableGrades().size());
    }
}

