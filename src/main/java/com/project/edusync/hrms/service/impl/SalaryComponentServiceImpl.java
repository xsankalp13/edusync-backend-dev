package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.salary.SalaryComponentCreateDTO;
import com.project.edusync.hrms.dto.salary.SalaryComponentResponseDTO;
import com.project.edusync.hrms.dto.salary.SalaryComponentUpdateDTO;
import com.project.edusync.hrms.model.entity.SalaryComponent;
import com.project.edusync.hrms.repository.SalaryComponentRepository;
import com.project.edusync.hrms.service.SalaryComponentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalaryComponentServiceImpl implements SalaryComponentService {

    private final SalaryComponentRepository salaryComponentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SalaryComponentResponseDTO> listAll() {
        return salaryComponentRepository.findByActiveTrueOrderBySortOrderAscComponentCodeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SalaryComponentResponseDTO create(SalaryComponentCreateDTO dto) {
        String code = dto.componentCode().trim().toUpperCase();
        if (salaryComponentRepository.existsByComponentCodeIgnoreCaseAndActiveTrue(code)) {
            throw new EdusyncException("Salary component code already exists: " + code, HttpStatus.CONFLICT);
        }

        SalaryComponent component = new SalaryComponent();
        apply(component, code, dto.componentName(), dto.type(), dto.calculationMethod(), dto.defaultValue(), dto.isTaxable(), dto.isStatutory(), dto.sortOrder());
        return toResponse(salaryComponentRepository.save(component));
    }

    @Override
    @Transactional
    public SalaryComponentResponseDTO update(Long componentId, SalaryComponentUpdateDTO dto) {
        SalaryComponent component = findActiveById(componentId);
        String code = dto.componentCode().trim().toUpperCase();

        if (salaryComponentRepository.existsByComponentCodeIgnoreCaseAndActiveTrueAndIdNot(code, componentId)) {
            throw new EdusyncException("Salary component code already exists: " + code, HttpStatus.CONFLICT);
        }

        apply(component, code, dto.componentName(), dto.type(), dto.calculationMethod(), dto.defaultValue(), dto.isTaxable(), dto.isStatutory(), dto.sortOrder());
        return toResponse(salaryComponentRepository.save(component));
    }

    @Override
    @Transactional
    public SalaryComponentResponseDTO updateByIdentifier(String identifier, SalaryComponentUpdateDTO dto) {
        SalaryComponent component = findActiveByIdentifier(identifier);
        return update(component.getId(), dto);
    }

    @Override
    @Transactional
    public void delete(Long componentId) {
        SalaryComponent component = findActiveById(componentId);
        component.setActive(false);
        salaryComponentRepository.save(component);
    }

    @Override
    @Transactional
    public void deleteByIdentifier(String identifier) {
        SalaryComponent component = findActiveByIdentifier(identifier);
        delete(component.getId());
    }

    private SalaryComponent findActiveByIdentifier(String identifier) {
        SalaryComponent component = PublicIdentifierResolver.resolve(
                identifier,
                salaryComponentRepository::findByUuid,
                salaryComponentRepository::findById,
                "Salary component"
        );
        if (!component.isActive()) {
            throw new ResourceNotFoundException("Salary component not found with identifier: " + identifier);
        }
        return component;
    }

    private SalaryComponent findActiveById(Long componentId) {
        SalaryComponent component = salaryComponentRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException("Salary component not found with id: " + componentId));

        if (!component.isActive()) {
            throw new ResourceNotFoundException("Salary component not found with id: " + componentId);
        }
        return component;
    }

    private void apply(
            SalaryComponent component,
            String code,
            String name,
            com.project.edusync.hrms.model.enums.SalaryComponentType type,
            com.project.edusync.hrms.model.enums.SalaryCalculationMethod calculationMethod,
            java.math.BigDecimal defaultValue,
            Boolean taxable,
            Boolean statutory,
            Integer sortOrder
    ) {
        component.setComponentCode(code);
        component.setComponentName(name.trim());
        component.setType(type);
        component.setCalculationMethod(calculationMethod);
        component.setDefaultValue(defaultValue);
        component.setTaxable(taxable == null || taxable);
        component.setStatutory(statutory != null && statutory);
        component.setSortOrder(sortOrder);
    }

    private SalaryComponentResponseDTO toResponse(SalaryComponent component) {
        return new SalaryComponentResponseDTO(
                component.getId(),
                component.getUuid() != null ? component.getUuid().toString() : null,
                component.getComponentCode(),
                component.getComponentName(),
                component.getType(),
                component.getCalculationMethod(),
                component.getDefaultValue(),
                component.isTaxable(),
                component.isStatutory(),
                component.getSortOrder(),
                component.isActive(),
                component.getCreatedAt(),
                component.getUpdatedAt()
        );
    }
}

