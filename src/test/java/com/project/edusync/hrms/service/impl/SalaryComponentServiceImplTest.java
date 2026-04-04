package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.hrms.dto.salary.SalaryComponentCreateDTO;
import com.project.edusync.hrms.dto.salary.SalaryComponentResponseDTO;
import com.project.edusync.hrms.model.entity.SalaryComponent;
import com.project.edusync.hrms.model.enums.SalaryCalculationMethod;
import com.project.edusync.hrms.model.enums.SalaryComponentType;
import com.project.edusync.hrms.repository.SalaryComponentRepository;
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
class SalaryComponentServiceImplTest {

    @Mock
    private SalaryComponentRepository salaryComponentRepository;

    @InjectMocks
    private SalaryComponentServiceImpl service;

    @Test
    void createThrowsConflictOnDuplicateCode() {
        SalaryComponentCreateDTO dto = new SalaryComponentCreateDTO(
                "BASIC",
                "Basic Pay",
                SalaryComponentType.EARNING,
                SalaryCalculationMethod.FIXED,
                BigDecimal.ZERO,
                true,
                false,
                1
        );

        when(salaryComponentRepository.existsByComponentCodeIgnoreCaseAndActiveTrue("BASIC")).thenReturn(true);
        assertThrows(EdusyncException.class, () -> service.create(dto));
    }

    @Test
    void createNormalizesCode() {
        SalaryComponentCreateDTO dto = new SalaryComponentCreateDTO(
                " hra ",
                "House Rent Allowance",
                SalaryComponentType.EARNING,
                SalaryCalculationMethod.PERCENTAGE_OF_BASIC,
                new BigDecimal("40"),
                true,
                false,
                2
        );

        when(salaryComponentRepository.existsByComponentCodeIgnoreCaseAndActiveTrue("HRA")).thenReturn(false);
        when(salaryComponentRepository.save(any(SalaryComponent.class))).thenAnswer(invocation -> {
            SalaryComponent component = invocation.getArgument(0);
            component.setId(1L);
            return component;
        });

        SalaryComponentResponseDTO response = service.create(dto);
        assertEquals("HRA", response.componentCode());
    }
}

