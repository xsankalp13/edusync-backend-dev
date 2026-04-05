package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.hrms.dto.grade.StaffGradeCreateDTO;
import com.project.edusync.hrms.dto.grade.StaffGradeResponseDTO;
import com.project.edusync.hrms.model.entity.StaffGrade;
import com.project.edusync.hrms.model.enums.TeachingWing;
import com.project.edusync.hrms.repository.StaffGradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffGradeServiceImplTest {

    @Mock
    private StaffGradeRepository staffGradeRepository;

    @InjectMocks
    private StaffGradeServiceImpl service;

    @Test
    void createRejectsDuplicateCode() {
        StaffGradeCreateDTO dto = new StaffGradeCreateDTO(
                "PRT",
                "Primary Teacher",
                TeachingWing.PRIMARY,
                new BigDecimal("25000"),
                new BigDecimal("45000"),
                1,
                2,
                null
        );

        when(staffGradeRepository.existsByGradeCodeIgnoreCaseAndActiveTrue("PRT")).thenReturn(true);
        assertThrows(EdusyncException.class, () -> service.createGrade(dto));
    }

    @Test
    void createNormalizesCode() {
        StaffGradeCreateDTO dto = new StaffGradeCreateDTO(
                " prt ",
                "Primary Teacher",
                TeachingWing.PRIMARY,
                new BigDecimal("25000"),
                new BigDecimal("45000"),
                1,
                2,
                null
        );

        when(staffGradeRepository.existsByGradeCodeIgnoreCaseAndActiveTrue("PRT")).thenReturn(false);
        when(staffGradeRepository.save(any(StaffGrade.class))).thenAnswer(invocation -> {
            StaffGrade grade = invocation.getArgument(0);
            grade.setId(1L);
            return grade;
        });

        StaffGradeResponseDTO response = service.createGrade(dto);
        assertEquals("PRT", response.gradeCode());
    }
}

