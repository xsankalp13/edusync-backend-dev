package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.calendar.BulkOperationResultDTO;
import com.project.edusync.hrms.dto.salary.ComponentOverrideDTO;
import com.project.edusync.hrms.dto.salary.ComputedComponentDTO;
import com.project.edusync.hrms.dto.salary.ComputedSalaryBreakdownDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingBulkCreateDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingCreateDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingResponseDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingUpdateDTO;
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
import com.project.edusync.hrms.service.StaffSalaryMappingService;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StaffSalaryMappingServiceImpl implements StaffSalaryMappingService {

    private final StaffSalaryMappingRepository staffSalaryMappingRepository;
    private final StaffSalaryComponentOverrideRepository overrideRepository;
    private final StaffRepository staffRepository;
    private final SalaryTemplateRepository salaryTemplateRepository;
    private final SalaryTemplateComponentRepository salaryTemplateComponentRepository;
    private final SalaryComponentRepository salaryComponentRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<StaffSalaryMappingResponseDTO> listMappings(Pageable pageable) {
        return staffSalaryMappingRepository.findByActiveTrue(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffSalaryMappingResponseDTO> getMappingsByStaffId(Long staffId) {
        return staffSalaryMappingRepository.findByStaff_IdAndActiveTrueOrderByEffectiveFromDesc(staffId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffSalaryMappingResponseDTO> getMappingsByStaffIdentifier(String staffIdentifier) {
        Staff staff = findActiveStaffByIdentifier(staffIdentifier);
        return getMappingsByStaffId(staff.getId());
    }

    @Override
    @Transactional
    public StaffSalaryMappingResponseDTO create(StaffSalaryMappingCreateDTO dto) {
        validateDateRange(dto.effectiveFrom(), dto.effectiveTo());
        Staff staff = findActiveStaffByIdentifier(dto.staffRef());
        SalaryTemplate template = findActiveTemplateByIdentifier(dto.templateRef());

        StaffSalaryMapping current = staffSalaryMappingRepository
                .findFirstByStaff_IdAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByEffectiveFromDesc(
                        staff.getId(),
                        dto.effectiveFrom()
                )
                .orElse(null);

        if (current != null && dto.effectiveTo() == null) {
            current.setEffectiveTo(dto.effectiveFrom().minusDays(1));
            staffSalaryMappingRepository.save(current);
        }

        assertNoOverlappingRange(staff.getId(), dto.effectiveFrom(), dto.effectiveTo(), null);

        StaffSalaryMapping mapping = new StaffSalaryMapping();
        mapping.setStaff(staff);
        mapping.setTemplate(template);
        mapping.setEffectiveFrom(dto.effectiveFrom());
        mapping.setEffectiveTo(dto.effectiveTo());
        mapping.setRemarks(dto.remarks());

        StaffSalaryMapping saved = staffSalaryMappingRepository.save(mapping);
        replaceOverrides(saved, dto.overrides());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public StaffSalaryMappingResponseDTO update(Long mappingId, StaffSalaryMappingUpdateDTO dto) {
        validateDateRange(dto.effectiveFrom(), dto.effectiveTo());
        StaffSalaryMapping mapping = findActiveMapping(mappingId);
        SalaryTemplate template = findActiveTemplateByIdentifier(dto.templateRef());

        mapping.setTemplate(template);
        mapping.setEffectiveFrom(dto.effectiveFrom());
        mapping.setEffectiveTo(dto.effectiveTo());
        mapping.setRemarks(dto.remarks());

        StaffSalaryMapping saved = staffSalaryMappingRepository.save(mapping);
        overrideRepository.deleteByMapping_Id(mappingId);
        replaceOverrides(saved, dto.overrides());

        assertNoOverlappingRange(mapping.getStaff().getId(), dto.effectiveFrom(), dto.effectiveTo(), mappingId);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public StaffSalaryMappingResponseDTO updateByIdentifier(String identifier, StaffSalaryMappingUpdateDTO dto) {
        StaffSalaryMapping mapping = findActiveMappingByIdentifier(identifier);
        return update(mapping.getId(), dto);
    }

    @Override
    @Transactional
    public BulkOperationResultDTO bulkCreate(StaffSalaryMappingBulkCreateDTO dto) {
        validateDateRange(dto.effectiveFrom(), dto.effectiveTo());
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (String staffRef : dto.staffRefs()) {
            try {
                create(new StaffSalaryMappingCreateDTO(
                        staffRef,
                        dto.templateRef(),
                        dto.effectiveFrom(),
                        dto.effectiveTo(),
                        dto.remarks(),
                        List.of()
                ));
                successCount++;
            } catch (Exception ex) {
                errors.add("staffRef=" + staffRef + ": " + ex.getMessage());
            }
        }

        int total = dto.staffRefs().size();
        return new BulkOperationResultDTO(total, successCount, total - successCount, errors);
    }

    @Override
    @Transactional(readOnly = true)
    public ComputedSalaryBreakdownDTO computeBreakdown(Long mappingId) {
        StaffSalaryMapping mapping = findActiveMapping(mappingId);
        List<SalaryTemplateComponent> templateComponents = salaryTemplateComponentRepository
                .findByTemplate_IdAndActiveTrueOrderByComponent_SortOrderAscComponent_ComponentCodeAsc(mapping.getTemplate().getId());

        Map<Long, BigDecimal> configuredValueByComponentId = new HashMap<>();
        for (SalaryTemplateComponent tc : templateComponents) {
            configuredValueByComponentId.put(tc.getComponent().getId(), tc.getValue());
        }

        List<StaffSalaryComponentOverride> overrides = overrideRepository.findByMapping_IdAndActiveTrue(mappingId);
        Set<Long> overriddenComponentIds = new HashSet<>();
        for (StaffSalaryComponentOverride override : overrides) {
            configuredValueByComponentId.put(override.getComponent().getId(), override.getOverrideValue());
            overriddenComponentIds.add(override.getComponent().getId());
        }

        BigDecimal basicAmount = BigDecimal.ZERO;
        SalaryTemplateComponent basicComponent = templateComponents.stream()
                .filter(tc -> "BASIC".equalsIgnoreCase(tc.getComponent().getComponentCode()))
                .findFirst()
                .orElse(null);

        if (basicComponent != null) {
            BigDecimal configured = configuredValueByComponentId.getOrDefault(basicComponent.getComponent().getId(), BigDecimal.ZERO);
            basicAmount = computeAmount(basicComponent.getComponent().getCalculationMethod(), configured, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        List<ComputedComponentDTO> earnings = new ArrayList<>();
        List<ComputedComponentDTO> deductions = new ArrayList<>();

        Map<Long, BigDecimal> computedAmountByComponentId = new HashMap<>();

        List<SalaryTemplateComponent> orderedTemplateComponents = templateComponents.stream()
                .sorted(Comparator.comparing(tc -> tc.getComponent().getSortOrder() == null ? Integer.MAX_VALUE : tc.getComponent().getSortOrder()))
                .toList();

        BigDecimal grossWithoutGrossPercentEarnings = BigDecimal.ZERO;
        BigDecimal grossPercentTotal = BigDecimal.ZERO;

        for (SalaryTemplateComponent tc : orderedTemplateComponents) {
            SalaryComponent component = tc.getComponent();
            if (component.getType() != SalaryComponentType.EARNING) {
                continue;
            }

            BigDecimal configured = configuredValueByComponentId.getOrDefault(component.getId(), tc.getValue());

            if (component.getCalculationMethod() == SalaryCalculationMethod.PERCENTAGE_OF_GROSS) {
                grossPercentTotal = grossPercentTotal.add(configured);
                continue;
            }

            BigDecimal amount = computeAmount(component.getCalculationMethod(), configured, basicAmount, BigDecimal.ZERO);
            computedAmountByComponentId.put(component.getId(), amount);
            grossWithoutGrossPercentEarnings = grossWithoutGrossPercentEarnings.add(amount);
        }

        if (grossPercentTotal.compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw new EdusyncException("Total percentage-of-gross earnings cannot be >= 100", HttpStatus.BAD_REQUEST);
        }

        BigDecimal gross = grossWithoutGrossPercentEarnings;
        if (grossPercentTotal.compareTo(BigDecimal.ZERO) > 0) {
            gross = grossWithoutGrossPercentEarnings
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(100).subtract(grossPercentTotal), 2, RoundingMode.HALF_UP);
        }

        for (SalaryTemplateComponent tc : orderedTemplateComponents) {
            SalaryComponent component = tc.getComponent();
            if (component.getType() == SalaryComponentType.EARNING) {
                BigDecimal configured = configuredValueByComponentId.getOrDefault(component.getId(), tc.getValue());
                BigDecimal amount = computedAmountByComponentId.get(component.getId());
                if (amount == null) {
                    amount = computeAmount(component.getCalculationMethod(), configured, basicAmount, gross);
                }

                earnings.add(new ComputedComponentDTO(
                        component.getComponentCode(),
                        component.getComponentName(),
                        component.getCalculationMethod().name(),
                        configured,
                        amount,
                        overriddenComponentIds.contains(component.getId())
                ));
            }
        }

        Set<String> taxableEarningCodes = templateComponents.stream()
                .map(SalaryTemplateComponent::getComponent)
                .filter(component -> component.getType() == SalaryComponentType.EARNING && component.isTaxable())
                .map(SalaryComponent::getComponentCode)
                .filter(code -> code != null && !code.isBlank())
                .map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toSet());

        BigDecimal taxableMonthlyEarnings = earnings.stream()
                .filter(e -> e.componentCode() != null && taxableEarningCodes.contains(e.componentCode().toUpperCase()))
                .map(ComputedComponentDTO::computedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDeductions = BigDecimal.ZERO;
        for (SalaryTemplateComponent tc : orderedTemplateComponents) {
            SalaryComponent component = tc.getComponent();
            if (component.getType() != SalaryComponentType.DEDUCTION) {
                continue;
            }

            BigDecimal configured = configuredValueByComponentId.getOrDefault(component.getId(), tc.getValue());
            BigDecimal amount = computeAmount(component.getCalculationMethod(), configured, basicAmount, gross);
            amount = applyStatutoryRules(component, configured, amount, basicAmount, gross, taxableMonthlyEarnings);
            totalDeductions = totalDeductions.add(amount);

            deductions.add(new ComputedComponentDTO(
                    component.getComponentCode(),
                    component.getComponentName(),
                    component.getCalculationMethod().name(),
                    configured,
                    amount,
                    overriddenComponentIds.contains(component.getId())
            ));
        }

        BigDecimal net = gross.subtract(totalDeductions).setScale(2, RoundingMode.HALF_UP);

        Staff staff = mapping.getStaff();
        return new ComputedSalaryBreakdownDTO(
                staff.getId(),
                staffFullName(staff),
                staff.getEmployeeId(),
                mapping.getTemplate().getTemplateName(),
                mapping.getTemplate().getGrade() != null ? mapping.getTemplate().getGrade().getGradeCode() : null,
                earnings,
                deductions,
                gross.setScale(2, RoundingMode.HALF_UP),
                totalDeductions.setScale(2, RoundingMode.HALF_UP),
                net,
                gross.setScale(2, RoundingMode.HALF_UP),
                !overrides.isEmpty()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ComputedSalaryBreakdownDTO computeBreakdownByIdentifier(String identifier) {
        StaffSalaryMapping mapping = findActiveMappingByIdentifier(identifier);
        return computeBreakdown(mapping.getId());
    }

    private BigDecimal computeAmount(
            SalaryCalculationMethod method,
            BigDecimal configured,
            BigDecimal basicAmount,
            BigDecimal grossAmount
    ) {
        BigDecimal amount = switch (method) {
            case FIXED -> configured;
            case PERCENTAGE_OF_BASIC -> basicAmount.multiply(configured).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case PERCENTAGE_OF_GROSS -> grossAmount.multiply(configured).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        };
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal applyStatutoryRules(
            SalaryComponent component,
            BigDecimal configured,
            BigDecimal defaultAmount,
            BigDecimal basicAmount,
            BigDecimal grossAmount,
            BigDecimal taxableMonthlyEarnings
    ) {
        if (!component.isStatutory()) {
            return defaultAmount;
        }

        String code = component.getComponentCode() == null ? "" : component.getComponentCode().toUpperCase();
        return switch (code) {
            case "PF_EMP", "PF_ER" -> {
                BigDecimal pfWageBase = basicAmount.min(new BigDecimal("15000"));
                yield pfWageBase.multiply(configured).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            case "ESI" -> grossAmount.compareTo(new BigDecimal("21000")) <= 0
                    ? grossAmount.multiply(configured).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            case "TDS" -> computeMonthlyTds(taxableMonthlyEarnings);
            default -> defaultAmount;
        };
    }

    private BigDecimal computeMonthlyTds(BigDecimal taxableMonthlyEarnings) {
        BigDecimal annualTaxable = taxableMonthlyEarnings.multiply(BigDecimal.valueOf(12));
        BigDecimal annualTax = BigDecimal.ZERO;

        annualTax = annualTax.add(slabTax(annualTaxable, 300000, 700000, 0.05));
        annualTax = annualTax.add(slabTax(annualTaxable, 700000, 1000000, 0.10));
        annualTax = annualTax.add(slabTax(annualTaxable, 1000000, 1200000, 0.15));
        annualTax = annualTax.add(slabTax(annualTaxable, 1200000, 1500000, 0.20));
        annualTax = annualTax.add(slabTax(annualTaxable, 1500000, Integer.MAX_VALUE, 0.30));

        return annualTax.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal slabTax(BigDecimal annualTaxable, int lowerExclusive, int upperInclusive, double rate) {
        BigDecimal lower = BigDecimal.valueOf(lowerExclusive);
        BigDecimal upper = BigDecimal.valueOf(upperInclusive);
        if (annualTaxable.compareTo(lower) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal taxableInSlab = annualTaxable.min(upper).subtract(lower);
        if (taxableInSlab.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return taxableInSlab.multiply(BigDecimal.valueOf(rate)).setScale(2, RoundingMode.HALF_UP);
    }

    private void replaceOverrides(StaffSalaryMapping mapping, List<ComponentOverrideDTO> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return;
        }

        Set<Long> uniqueIds = new HashSet<>();
        for (ComponentOverrideDTO override : overrides) {
            SalaryComponent component = findActiveSalaryComponentByIdentifier(override.componentRef());
            if (!uniqueIds.add(component.getId())) {
                throw new EdusyncException("Duplicate override componentRef: " + override.componentRef(), HttpStatus.BAD_REQUEST);
            }

            StaffSalaryComponentOverride entity = new StaffSalaryComponentOverride();
            entity.setMapping(mapping);
            entity.setComponent(component);
            entity.setOverrideValue(override.overrideValue());
            entity.setReason(override.reason());
            overrideRepository.save(entity);
        }
    }

    private SalaryComponent findActiveSalaryComponentByIdentifier(String identifier) {
        SalaryComponent component = PublicIdentifierResolver.resolve(
                identifier,
                salaryComponentRepository::findByUuid,
                salaryComponentRepository::findById,
                "Salary component"
        );
        if (!component.isActive()) {
            throw new EdusyncException("Selected salary component is inactive", HttpStatus.BAD_REQUEST);
        }
        return component;
    }

    private StaffSalaryMapping findActiveMapping(Long mappingId) {
        StaffSalaryMapping mapping = staffSalaryMappingRepository.findById(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff salary mapping not found with id: " + mappingId));

        if (!mapping.isActive()) {
            throw new ResourceNotFoundException("Staff salary mapping not found with id: " + mappingId);
        }
        return mapping;
    }

    private StaffSalaryMapping findActiveMappingByIdentifier(String identifier) {
        StaffSalaryMapping mapping = PublicIdentifierResolver.resolve(
                identifier,
                staffSalaryMappingRepository::findByUuid,
                staffSalaryMappingRepository::findById,
                "Staff salary mapping"
        );
        if (!mapping.isActive()) {
            throw new ResourceNotFoundException("Staff salary mapping not found with identifier: " + identifier);
        }
        return mapping;
    }

    private Staff findActiveStaff(Long staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with id: " + staffId));

        if (!staff.isActive()) {
            throw new EdusyncException("Cannot assign salary mapping to inactive staff", HttpStatus.BAD_REQUEST);
        }
        return staff;
    }

    private Staff findActiveStaffByIdentifier(String identifier) {
        Staff staff = PublicIdentifierResolver.resolve(
                identifier,
                staffRepository::findByUuid,
                staffRepository::findById,
                "Staff"
        );
        if (!staff.isActive()) {
            throw new ResourceNotFoundException("Staff not found with identifier: " + identifier);
        }
        return staff;
    }

    private SalaryTemplate findActiveTemplate(Long templateId) {
        SalaryTemplate template = salaryTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Salary template not found with id: " + templateId));

        if (!template.isActive()) {
            throw new EdusyncException("Selected salary template is inactive", HttpStatus.BAD_REQUEST);
        }
        return template;
    }

    private SalaryTemplate findActiveTemplateByIdentifier(String identifier) {
        SalaryTemplate template = PublicIdentifierResolver.resolve(
                identifier,
                salaryTemplateRepository::findByUuid,
                salaryTemplateRepository::findById,
                "Salary template"
        );

        if (!template.isActive()) {
            throw new EdusyncException("Selected salary template is inactive", HttpStatus.BAD_REQUEST);
        }
        return template;
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (toDate != null && toDate.isBefore(fromDate)) {
            throw new EdusyncException("effectiveTo cannot be before effectiveFrom", HttpStatus.BAD_REQUEST);
        }
    }

    private void assertNoOverlappingRange(Long staffId, LocalDate fromDate, LocalDate toDate, Long excludeMappingId) {
        LocalDate effectiveTo = toDate == null ? LocalDate.of(9999, 12, 31) : toDate;
        boolean overlapExists = staffSalaryMappingRepository.existsOverlappingRange(
                staffId,
                fromDate,
                effectiveTo,
                LocalDate.of(9999, 12, 31),
                excludeMappingId
        );

        if (overlapExists) {
            throw new EdusyncException("Salary mapping dates overlap an existing mapping", HttpStatus.CONFLICT);
        }
    }

    private StaffSalaryMappingResponseDTO toResponse(StaffSalaryMapping mapping) {
        List<ComponentOverrideDTO> overrides = overrideRepository.findByMapping_IdAndActiveTrue(mapping.getId()).stream()
                .map(override -> new ComponentOverrideDTO(
                        String.valueOf(override.getComponent().getId()),
                        override.getOverrideValue(),
                        override.getReason()
                ))
                .toList();

        Staff staff = mapping.getStaff();
        return new StaffSalaryMappingResponseDTO(
                mapping.getId(),
                mapping.getUuid() != null ? mapping.getUuid().toString() : null,
                staff.getId(),
                staffFullName(staff),
                staff.getEmployeeId(),
                mapping.getTemplate().getId(),
                mapping.getTemplate().getTemplateName(),
                mapping.getTemplate().getGrade() != null ? mapping.getTemplate().getGrade().getId() : null,
                mapping.getTemplate().getGrade() != null ? mapping.getTemplate().getGrade().getGradeCode() : null,
                mapping.getEffectiveFrom(),
                mapping.getEffectiveTo(),
                mapping.getRemarks(),
                mapping.isActive(),
                mapping.getCreatedAt(),
                overrides
        );
    }

    private String staffFullName(Staff staff) {
        return (staff.getUserProfile().getFirstName() + " " + staff.getUserProfile().getLastName()).trim();
    }
}



