package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.hrms.dto.salary.ComponentOverrideDTO;
import com.project.edusync.hrms.dto.salary.ComputedSalaryBreakdownDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingCreateDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingResponseDTO;
import com.project.edusync.hrms.model.entity.SalaryComponent;
import com.project.edusync.hrms.model.entity.SalaryTemplate;
import com.project.edusync.hrms.model.entity.SalaryTemplateComponent;
import com.project.edusync.hrms.model.entity.StaffSalaryComponentOverride;
import com.project.edusync.hrms.model.entity.StaffSalaryMapping;
import com.project.edusync.hrms.model.enums.SalaryCalculationMethod;
import com.project.edusync.hrms.model.enums.SalaryComponentType;
import com.project.edusync.hrms.repository.SalaryComponentRepository;
import com.project.edusync.hrms.repository.SalaryTemplateComponentRepository;
import com.project.edusync.hrms.repository.SalaryTemplateRepository;
import com.project.edusync.hrms.repository.StaffSalaryComponentOverrideRepository;
import com.project.edusync.hrms.repository.StaffSalaryMappingRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffSalaryMappingServiceImplTest {

    @Mock
    private StaffSalaryMappingRepository staffSalaryMappingRepository;
    @Mock
    private StaffSalaryComponentOverrideRepository overrideRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private SalaryTemplateRepository salaryTemplateRepository;
    @Mock
    private SalaryTemplateComponentRepository salaryTemplateComponentRepository;
    @Mock
    private SalaryComponentRepository salaryComponentRepository;

    @InjectMocks
    private StaffSalaryMappingServiceImpl service;

    @Test
    void createPersistsMappingAndOverrides() {
        Staff staff = buildStaff();
        SalaryTemplate template = buildTemplate();
        SalaryComponent basic = buildComponent(1L, "BASIC", SalaryComponentType.EARNING, SalaryCalculationMethod.FIXED);

        when(staffRepository.findById(101L)).thenReturn(Optional.of(staff));
        when(salaryTemplateRepository.findById(10L)).thenReturn(Optional.of(template));
        when(staffSalaryMappingRepository.findFirstByStaff_IdAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByEffectiveFromDesc(101L, LocalDate.of(2026, 4, 1)))
                .thenReturn(Optional.empty());
        when(staffSalaryMappingRepository.existsOverlappingRange(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(9999, 12, 31),
                LocalDate.of(9999, 12, 31),
                null
        )).thenReturn(false);
        when(staffSalaryMappingRepository.save(any(StaffSalaryMapping.class))).thenAnswer(invocation -> {
            StaffSalaryMapping mapping = invocation.getArgument(0);
            mapping.setId(1001L);
            return mapping;
        });
        when(salaryComponentRepository.findById(1L)).thenReturn(Optional.of(basic));
        when(overrideRepository.findByMapping_IdAndActiveTrue(1001L)).thenReturn(List.of());

        StaffSalaryMappingCreateDTO request = new StaffSalaryMappingCreateDTO(
                "101",
                "10",
                LocalDate.of(2026, 4, 1),
                null,
                "Initial mapping",
                List.of(new ComponentOverrideDTO("1", new BigDecimal("12000"), "Seniority"))
        );

        StaffSalaryMappingResponseDTO response = service.create(request);

        assertEquals(1001L, response.mappingId());
        assertEquals("Template A", response.templateName());
    }

    @Test
    void createThrowsConflictWhenEffectiveRangeOverlaps() {
        Staff staff = buildStaff();
        SalaryTemplate template = buildTemplate();

        when(staffRepository.findById(101L)).thenReturn(Optional.of(staff));
        when(salaryTemplateRepository.findById(10L)).thenReturn(Optional.of(template));
        when(staffSalaryMappingRepository.findFirstByStaff_IdAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByEffectiveFromDesc(101L, LocalDate.of(2026, 4, 1)))
                .thenReturn(Optional.empty());
        when(staffSalaryMappingRepository.existsOverlappingRange(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30),
                LocalDate.of(9999, 12, 31),
                null
        )).thenReturn(true);

        StaffSalaryMappingCreateDTO request = new StaffSalaryMappingCreateDTO(
                "101",
                "10",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30),
                "Overlap",
                List.of()
        );

        assertThrows(EdusyncException.class, () -> service.create(request));
    }

    @Test
    void createResolvesOverrideComponentByUuidReference() {
        Staff staff = buildStaff();
        SalaryTemplate template = buildTemplate();
        SalaryComponent basic = buildComponent(1L, "BASIC", SalaryComponentType.EARNING, SalaryCalculationMethod.FIXED);
        UUID componentUuid = UUID.fromString("22222222-2222-2222-2222-222222222222");
        basic.setUuid(componentUuid);

        when(staffRepository.findById(101L)).thenReturn(Optional.of(staff));
        when(salaryTemplateRepository.findById(10L)).thenReturn(Optional.of(template));
        when(staffSalaryMappingRepository.findFirstByStaff_IdAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByEffectiveFromDesc(101L, LocalDate.of(2026, 4, 1)))
                .thenReturn(Optional.empty());
        when(staffSalaryMappingRepository.existsOverlappingRange(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(9999, 12, 31),
                LocalDate.of(9999, 12, 31),
                null
        )).thenReturn(false);
        when(staffSalaryMappingRepository.save(any(StaffSalaryMapping.class))).thenAnswer(invocation -> {
            StaffSalaryMapping mapping = invocation.getArgument(0);
            mapping.setId(1001L);
            return mapping;
        });
        when(salaryComponentRepository.findByUuid(componentUuid)).thenReturn(Optional.of(basic));
        when(overrideRepository.findByMapping_IdAndActiveTrue(1001L)).thenReturn(List.of());

        StaffSalaryMappingCreateDTO request = new StaffSalaryMappingCreateDTO(
                "101",
                "10",
                LocalDate.of(2026, 4, 1),
                null,
                "Initial mapping",
                List.of(new ComponentOverrideDTO(componentUuid.toString(), new BigDecimal("12000"), "Seniority"))
        );

        StaffSalaryMappingResponseDTO response = service.create(request);

        assertEquals(1001L, response.mappingId());
        verify(salaryComponentRepository).findByUuid(componentUuid);
        verify(salaryComponentRepository, never()).findById(1L);
    }

    @Test
    void computeBreakdownAppliesOverride() {
        Staff staff = buildStaff();
        SalaryTemplate template = buildTemplate();

        SalaryComponent basic = buildComponent(1L, "BASIC", SalaryComponentType.EARNING, SalaryCalculationMethod.FIXED);
        SalaryComponent hra = buildComponent(2L, "HRA", SalaryComponentType.EARNING, SalaryCalculationMethod.PERCENTAGE_OF_BASIC);
        SalaryComponent pf = buildComponent(3L, "PF_EMP", SalaryComponentType.DEDUCTION, SalaryCalculationMethod.PERCENTAGE_OF_BASIC);

        StaffSalaryMapping mapping = new StaffSalaryMapping();
        mapping.setId(1001L);
        mapping.setActive(true);
        mapping.setStaff(staff);
        mapping.setTemplate(template);

        SalaryTemplateComponent t1 = templateComponent(template, basic, new BigDecimal("10000"));
        SalaryTemplateComponent t2 = templateComponent(template, hra, new BigDecimal("40"));
        SalaryTemplateComponent t3 = templateComponent(template, pf, new BigDecimal("12"));

        StaffSalaryComponentOverride override = new StaffSalaryComponentOverride();
        override.setMapping(mapping);
        override.setComponent(basic);
        override.setOverrideValue(new BigDecimal("12000"));

        when(staffSalaryMappingRepository.findById(1001L)).thenReturn(Optional.of(mapping));
        when(salaryTemplateComponentRepository.findByTemplate_IdAndActiveTrueOrderByComponent_SortOrderAscComponent_ComponentCodeAsc(10L))
                .thenReturn(List.of(t1, t2, t3));
        when(overrideRepository.findByMapping_IdAndActiveTrue(1001L)).thenReturn(List.of(override));

        ComputedSalaryBreakdownDTO result = service.computeBreakdown(1001L);

        assertEquals(new BigDecimal("16800.00"), result.grossPay());
        assertEquals(new BigDecimal("1440.00"), result.totalDeductions());
        assertEquals(true, result.hasOverrides());
    }

    @Test
    void computeBreakdownSupportsPercentageOfGrossEarnings() {
        Staff staff = buildStaff();
        SalaryTemplate template = buildTemplate();

        SalaryComponent basic = buildComponent(1L, "BASIC", SalaryComponentType.EARNING, SalaryCalculationMethod.FIXED);
        SalaryComponent hra = buildComponent(2L, "HRA", SalaryComponentType.EARNING, SalaryCalculationMethod.PERCENTAGE_OF_BASIC);
        SalaryComponent bonus = buildComponent(4L, "BONUS", SalaryComponentType.EARNING, SalaryCalculationMethod.PERCENTAGE_OF_GROSS);

        StaffSalaryMapping mapping = new StaffSalaryMapping();
        mapping.setId(2001L);
        mapping.setActive(true);
        mapping.setStaff(staff);
        mapping.setTemplate(template);

        SalaryTemplateComponent t1 = templateComponent(template, basic, new BigDecimal("10000"));
        SalaryTemplateComponent t2 = templateComponent(template, hra, new BigDecimal("40"));
        SalaryTemplateComponent t3 = templateComponent(template, bonus, new BigDecimal("10"));

        when(staffSalaryMappingRepository.findById(2001L)).thenReturn(Optional.of(mapping));
        when(salaryTemplateComponentRepository.findByTemplate_IdAndActiveTrueOrderByComponent_SortOrderAscComponent_ComponentCodeAsc(10L))
                .thenReturn(List.of(t1, t2, t3));
        when(overrideRepository.findByMapping_IdAndActiveTrue(2001L)).thenReturn(List.of());

        ComputedSalaryBreakdownDTO result = service.computeBreakdown(2001L);

        assertEquals(new BigDecimal("15555.56"), result.grossPay());
        assertEquals(new BigDecimal("15555.56"), result.netPay());
    }

    @Test
    void computeBreakdownAppliesStatutoryCapsAndThresholds() {
        Staff staff = buildStaff();
        SalaryTemplate template = buildTemplate();

        SalaryComponent basic = buildComponent(1L, "BASIC", SalaryComponentType.EARNING, SalaryCalculationMethod.FIXED);
        basic.setTaxable(true);

        SalaryComponent pfEmp = buildComponent(2L, "PF_EMP", SalaryComponentType.DEDUCTION, SalaryCalculationMethod.PERCENTAGE_OF_BASIC);
        pfEmp.setStatutory(true);
        SalaryComponent esi = buildComponent(3L, "ESI", SalaryComponentType.DEDUCTION, SalaryCalculationMethod.PERCENTAGE_OF_GROSS);
        esi.setStatutory(true);

        StaffSalaryMapping mapping = new StaffSalaryMapping();
        mapping.setId(3001L);
        mapping.setActive(true);
        mapping.setStaff(staff);
        mapping.setTemplate(template);

        SalaryTemplateComponent t1 = templateComponent(template, basic, new BigDecimal("50000"));
        SalaryTemplateComponent t2 = templateComponent(template, pfEmp, new BigDecimal("12"));
        SalaryTemplateComponent t3 = templateComponent(template, esi, new BigDecimal("1.75"));

        when(staffSalaryMappingRepository.findById(3001L)).thenReturn(Optional.of(mapping));
        when(salaryTemplateComponentRepository.findByTemplate_IdAndActiveTrueOrderByComponent_SortOrderAscComponent_ComponentCodeAsc(10L))
                .thenReturn(List.of(t1, t2, t3));
        when(overrideRepository.findByMapping_IdAndActiveTrue(3001L)).thenReturn(List.of());

        ComputedSalaryBreakdownDTO result = service.computeBreakdown(3001L);

        BigDecimal pfAmount = result.deductions().stream()
                .filter(d -> "PF_EMP".equalsIgnoreCase(d.componentCode()))
                .findFirst().orElseThrow().computedAmount();
        BigDecimal esiAmount = result.deductions().stream()
                .filter(d -> "ESI".equalsIgnoreCase(d.componentCode()))
                .findFirst().orElseThrow().computedAmount();

        assertEquals(new BigDecimal("1800.00"), pfAmount);
        assertEquals(new BigDecimal("0.00"), esiAmount);
    }

    @Test
    void computeBreakdownComputesMonthlyTdsFromTaxableEarnings() {
        Staff staff = buildStaff();
        SalaryTemplate template = buildTemplate();

        SalaryComponent basic = buildComponent(1L, "BASIC", SalaryComponentType.EARNING, SalaryCalculationMethod.FIXED);
        basic.setTaxable(true);
        SalaryComponent specialAllowance = buildComponent(2L, "SP_ALLOW", SalaryComponentType.EARNING, SalaryCalculationMethod.FIXED);
        specialAllowance.setTaxable(true);
        SalaryComponent tds = buildComponent(3L, "TDS", SalaryComponentType.DEDUCTION, SalaryCalculationMethod.FIXED);
        tds.setStatutory(true);

        StaffSalaryMapping mapping = new StaffSalaryMapping();
        mapping.setId(3002L);
        mapping.setActive(true);
        mapping.setStaff(staff);
        mapping.setTemplate(template);

        SalaryTemplateComponent t1 = templateComponent(template, basic, new BigDecimal("50000"));
        SalaryTemplateComponent t2 = templateComponent(template, specialAllowance, new BigDecimal("20000"));
        SalaryTemplateComponent t3 = templateComponent(template, tds, BigDecimal.ZERO);

        when(staffSalaryMappingRepository.findById(3002L)).thenReturn(Optional.of(mapping));
        when(salaryTemplateComponentRepository.findByTemplate_IdAndActiveTrueOrderByComponent_SortOrderAscComponent_ComponentCodeAsc(10L))
                .thenReturn(List.of(t1, t2, t3));
        when(overrideRepository.findByMapping_IdAndActiveTrue(3002L)).thenReturn(List.of());

        ComputedSalaryBreakdownDTO result = service.computeBreakdown(3002L);

        BigDecimal tdsAmount = result.deductions().stream()
                .filter(d -> "TDS".equalsIgnoreCase(d.componentCode()))
                .findFirst().orElseThrow().computedAmount();

        assertEquals(new BigDecimal("2833.33"), tdsAmount);
    }

    private Staff buildStaff() {
        Staff staff = new Staff();
        staff.setId(101L);
        staff.setActive(true);
        staff.setEmployeeId("EMP001");

        UserProfile profile = new UserProfile();
        profile.setFirstName("Ravi");
        profile.setLastName("Kumar");
        staff.setUserProfile(profile);
        return staff;
    }

    private SalaryTemplate buildTemplate() {
        SalaryTemplate template = new SalaryTemplate();
        template.setId(10L);
        template.setActive(true);
        template.setTemplateName("Template A");
        return template;
    }

    private SalaryComponent buildComponent(Long id, String code, SalaryComponentType type, SalaryCalculationMethod method) {
        SalaryComponent component = new SalaryComponent();
        component.setId(id);
        component.setActive(true);
        component.setComponentCode(code);
        component.setComponentName(code + " Name");
        component.setType(type);
        component.setCalculationMethod(method);
        component.setDefaultValue(BigDecimal.ZERO);
        return component;
    }

    private SalaryTemplateComponent templateComponent(SalaryTemplate template, SalaryComponent component, BigDecimal value) {
        SalaryTemplateComponent tc = new SalaryTemplateComponent();
        tc.setTemplate(template);
        tc.setComponent(component);
        tc.setValue(value);
        tc.setActive(true);
        return tc;
    }
}


