package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.hrms.dto.salary.SalaryTemplateCreateDTO;
import com.project.edusync.hrms.dto.salary.SalaryTemplateResponseDTO;
import com.project.edusync.hrms.dto.salary.TemplateComponentInputDTO;
import com.project.edusync.hrms.model.entity.SalaryComponent;
import com.project.edusync.hrms.model.entity.SalaryTemplate;
import com.project.edusync.hrms.model.enums.SalaryCalculationMethod;
import com.project.edusync.hrms.model.enums.SalaryComponentType;
import com.project.edusync.hrms.repository.SalaryComponentRepository;
import com.project.edusync.hrms.repository.SalaryTemplateComponentRepository;
import com.project.edusync.hrms.repository.SalaryTemplateRepository;
import com.project.edusync.hrms.repository.StaffGradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalaryTemplateServiceImplTest {

    @Mock
    private SalaryTemplateRepository salaryTemplateRepository;
    @Mock
    private SalaryTemplateComponentRepository salaryTemplateComponentRepository;
    @Mock
    private SalaryComponentRepository salaryComponentRepository;
    @Mock
    private StaffGradeRepository staffGradeRepository;

    @InjectMocks
    private SalaryTemplateServiceImpl service;

    @Test
    void createRejectsDuplicateComponentIdsInPayload() {
        SalaryComponent basic = buildComponent(1L, "BASIC");
        SalaryTemplateCreateDTO dto = new SalaryTemplateCreateDTO(
                "PRT Standard",
                null,
                null,
                "2026-2027",
                List.of(
                        new TemplateComponentInputDTO(1L, new BigDecimal("10000")),
                        new TemplateComponentInputDTO(1L, new BigDecimal("12000"))
                )
        );

        when(salaryTemplateRepository.save(any(SalaryTemplate.class))).thenAnswer(invocation -> {
            SalaryTemplate template = invocation.getArgument(0);
            template.setId(10L);
            return template;
        });
        when(salaryComponentRepository.findById(1L)).thenReturn(Optional.of(basic));

        assertThrows(EdusyncException.class, () -> service.create(dto));
    }

    @Test
    void createReturnsSavedTemplateResponse() {
        SalaryComponent basic = buildComponent(1L, "BASIC");
        SalaryTemplateCreateDTO dto = new SalaryTemplateCreateDTO(
                "PRT Standard",
                "Default template",
                null,
                "2026-2027",
                List.of(new TemplateComponentInputDTO(1L, new BigDecimal("10000")))
        );

        when(salaryTemplateRepository.save(any(SalaryTemplate.class))).thenAnswer(invocation -> {
            SalaryTemplate template = invocation.getArgument(0);
            template.setId(10L);
            return template;
        });
        when(salaryComponentRepository.findById(1L)).thenReturn(Optional.of(basic));
        when(salaryTemplateComponentRepository.findByTemplate_IdAndActiveTrueOrderByComponent_SortOrderAscComponent_ComponentCodeAsc(10L))
                .thenReturn(List.of());

        SalaryTemplateResponseDTO response = service.create(dto);
        assertEquals("PRT Standard", response.templateName());
        assertEquals("2026-2027", response.academicYear());
    }

    private SalaryComponent buildComponent(Long id, String code) {
        SalaryComponent component = new SalaryComponent();
        component.setId(id);
        component.setComponentCode(code);
        component.setComponentName(code + " Name");
        component.setType(SalaryComponentType.EARNING);
        component.setCalculationMethod(SalaryCalculationMethod.FIXED);
        component.setDefaultValue(BigDecimal.ZERO);
        component.setActive(true);
        return component;
    }
}

