package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.salary.SalaryTemplateCreateDTO;
import com.project.edusync.hrms.dto.salary.SalaryTemplateResponseDTO;
import com.project.edusync.hrms.dto.salary.SalaryTemplateUpdateDTO;
import com.project.edusync.hrms.dto.salary.TemplateComponentInputDTO;
import com.project.edusync.hrms.dto.salary.TemplateComponentResponseDTO;
import com.project.edusync.hrms.model.entity.SalaryComponent;
import com.project.edusync.hrms.model.entity.SalaryTemplate;
import com.project.edusync.hrms.model.entity.SalaryTemplateComponent;
import com.project.edusync.hrms.model.entity.StaffGrade;
import com.project.edusync.hrms.repository.SalaryComponentRepository;
import com.project.edusync.hrms.repository.SalaryTemplateComponentRepository;
import com.project.edusync.hrms.repository.SalaryTemplateRepository;
import com.project.edusync.hrms.repository.StaffGradeRepository;
import com.project.edusync.hrms.service.SalaryTemplateService;
import com.project.edusync.uis.model.enums.StaffCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SalaryTemplateServiceImpl implements SalaryTemplateService {

    private final SalaryTemplateRepository salaryTemplateRepository;
    private final SalaryTemplateComponentRepository salaryTemplateComponentRepository;
    private final SalaryComponentRepository salaryComponentRepository;
    private final StaffGradeRepository staffGradeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SalaryTemplateResponseDTO> listAll(StaffCategory category) {
        List<SalaryTemplate> templates = category == null
                ? salaryTemplateRepository.findByActiveTrueOrderByTemplateNameAsc()
                : salaryTemplateRepository.findApplicableForCategory(category);

        return templates.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SalaryTemplateResponseDTO getById(Long templateId) {
        return toResponse(findActiveTemplateById(templateId));
    }

    @Override
    @Transactional(readOnly = true)
    public SalaryTemplateResponseDTO getByIdentifier(String identifier) {
        return toResponse(findActiveTemplateByIdentifier(identifier));
    }

    @Override
    @Transactional
    public SalaryTemplateResponseDTO create(SalaryTemplateCreateDTO dto) {
        SalaryTemplate template = new SalaryTemplate();
        StaffGrade grade = resolveGrade(dto.gradeRef());

        template.setTemplateName(dto.templateName().trim());
        template.setDescription(dto.description());
        template.setGrade(grade);
        template.setAcademicYear(normalizeAcademicYear(dto.academicYear()));
        template.setApplicableCategory(dto.applicableCategory());

        SalaryTemplate savedTemplate = salaryTemplateRepository.save(template);
        replaceTemplateComponents(savedTemplate, dto.components());

        return toResponse(savedTemplate);
    }

    @Override
    @Transactional
    public SalaryTemplateResponseDTO update(Long templateId, SalaryTemplateUpdateDTO dto) {
        SalaryTemplate template = findActiveTemplateById(templateId);
        StaffGrade grade = resolveGrade(dto.gradeRef());

        template.setTemplateName(dto.templateName().trim());
        template.setDescription(dto.description());
        template.setGrade(grade);
        template.setAcademicYear(normalizeAcademicYear(dto.academicYear()));
        template.setApplicableCategory(dto.applicableCategory());

        SalaryTemplate savedTemplate = salaryTemplateRepository.save(template);
        salaryTemplateComponentRepository.deleteByTemplate_Id(templateId);
        replaceTemplateComponents(savedTemplate, dto.components());

        return toResponse(savedTemplate);
    }

    @Override
    @Transactional
    public SalaryTemplateResponseDTO updateByIdentifier(String identifier, SalaryTemplateUpdateDTO dto) {
        SalaryTemplate template = findActiveTemplateByIdentifier(identifier);
        return update(template.getId(), dto);
    }

    @Override
    @Transactional
    public void delete(Long templateId) {
        SalaryTemplate template = findActiveTemplateById(templateId);
        template.setActive(false);
        salaryTemplateRepository.save(template);
    }

    @Override
    @Transactional
    public void deleteByIdentifier(String identifier) {
        SalaryTemplate template = findActiveTemplateByIdentifier(identifier);
        delete(template.getId());
    }

    private StaffGrade resolveGrade(String gradeRef) {
        if (gradeRef == null || gradeRef.isBlank()) {
            return null;
        }

        StaffGrade grade = PublicIdentifierResolver.resolve(
                gradeRef,
                staffGradeRepository::findByUuid,
                staffGradeRepository::findById,
                "Staff grade"
        );

        if (!grade.isActive()) {
            throw new EdusyncException("Selected grade is inactive", HttpStatus.BAD_REQUEST);
        }
        return grade;
    }

    private SalaryTemplate findActiveTemplateById(Long templateId) {
        SalaryTemplate template = salaryTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Salary template not found with id: " + templateId));

        if (!template.isActive()) {
            throw new ResourceNotFoundException("Salary template not found with id: " + templateId);
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
            throw new ResourceNotFoundException("Salary template not found with identifier: " + identifier);
        }
        return template;
    }

    private void replaceTemplateComponents(SalaryTemplate template, List<TemplateComponentInputDTO> components) {
        if (components == null || components.isEmpty()) {
            throw new EdusyncException("At least one template component is required", HttpStatus.BAD_REQUEST);
        }

        Set<Long> uniqueComponentIds = new HashSet<>();
        for (TemplateComponentInputDTO input : components) {
            SalaryComponent component = resolveActiveComponent(input.componentRef());
            if (!uniqueComponentIds.add(component.getId())) {
                throw new EdusyncException("Duplicate componentRef in template payload: " + input.componentRef(), HttpStatus.BAD_REQUEST);
            }

            SalaryTemplateComponent templateComponent = new SalaryTemplateComponent();
            templateComponent.setTemplate(template);
            templateComponent.setComponent(component);
            templateComponent.setValue(input.value());
            salaryTemplateComponentRepository.save(templateComponent);
        }
    }

    private SalaryComponent resolveActiveComponent(String componentRef) {
        SalaryComponent component = PublicIdentifierResolver.resolve(
                componentRef,
                salaryComponentRepository::findByUuid,
                salaryComponentRepository::findById,
                "Salary component"
        );

        if (!component.isActive()) {
            throw new EdusyncException("Selected salary component is inactive: " + component.getComponentCode(), HttpStatus.BAD_REQUEST);
        }
        return component;
    }

    private String normalizeAcademicYear(String academicYear) {
        String[] parts = academicYear.trim().split("-");
        if (parts.length != 2) {
            throw new EdusyncException("Invalid academicYear format. Expected YYYY-YYYY", HttpStatus.BAD_REQUEST);
        }

        try {
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);
            if (end != start + 1) {
                throw new EdusyncException("Invalid academicYear format. Expected YYYY-YYYY", HttpStatus.BAD_REQUEST);
            }
            return start + "-" + end;
        } catch (NumberFormatException ex) {
            throw new EdusyncException("Invalid academicYear format. Expected YYYY-YYYY", HttpStatus.BAD_REQUEST);
        }
    }

    private SalaryTemplateResponseDTO toResponse(SalaryTemplate template) {
        List<TemplateComponentResponseDTO> components = salaryTemplateComponentRepository
                .findByTemplate_IdAndActiveTrueOrderByComponent_SortOrderAscComponent_ComponentCodeAsc(template.getId())
                .stream()
                .map(tc -> new TemplateComponentResponseDTO(
                        tc.getComponent().getId(),
                        tc.getComponent().getComponentCode(),
                        tc.getComponent().getComponentName(),
                        tc.getComponent().getType(),
                        tc.getComponent().getCalculationMethod(),
                        tc.getValue()
                ))
                .toList();

        return new SalaryTemplateResponseDTO(
                template.getId(),
                template.getUuid() != null ? template.getUuid().toString() : null,
                template.getTemplateName(),
                template.getDescription(),
                template.getGrade() != null ? template.getGrade().getId() : null,
                template.getGrade() != null ? template.getGrade().getGradeCode() : null,
                template.getGrade() != null ? template.getGrade().getGradeName() : null,
                template.getAcademicYear(),
                template.getApplicableCategory(),
                template.isActive(),
                template.getCreatedAt(),
                template.getUpdatedAt(),
                components
        );
    }
}

