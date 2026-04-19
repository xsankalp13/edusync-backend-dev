package com.project.edusync.hrms.service.impl;

import com.project.edusync.hrms.dto.calendar.BulkOperationResultDTO;
import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.leavetemplate.BulkAssignByDesignationDTO;
import com.project.edusync.hrms.dto.leavetemplate.LeaveTemplateCreateDTO;
import com.project.edusync.hrms.dto.leavetemplate.LeaveTemplateItemDTO;
import com.project.edusync.hrms.dto.leavetemplate.LeaveTemplateItemRequestDTO;
import com.project.edusync.hrms.dto.leavetemplate.LeaveTemplateResponseDTO;
import com.project.edusync.hrms.dto.leavetemplate.LeaveTemplateUpdateDTO;
import com.project.edusync.hrms.dto.leavetemplate.StaffLeaveTemplateMappingRequestDTO;
import com.project.edusync.hrms.dto.leavetemplate.StaffLeaveTemplateMappingResponseDTO;
import com.project.edusync.hrms.model.entity.LeaveTemplate;
import com.project.edusync.hrms.model.entity.LeaveTemplateItem;
import com.project.edusync.hrms.model.entity.LeaveTypeConfig;
import com.project.edusync.hrms.model.entity.StaffDesignation;
import com.project.edusync.hrms.model.entity.StaffLeaveTemplateMapping;
import com.project.edusync.hrms.repository.LeaveTemplateRepository;
import com.project.edusync.hrms.repository.LeaveTypeConfigRepository;
import com.project.edusync.hrms.repository.StaffDesignationRepository;
import com.project.edusync.hrms.repository.StaffLeaveTemplateMappingRepository;
import com.project.edusync.hrms.service.LeaveTemplateService;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.enums.StaffCategory;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveTemplateServiceImpl implements LeaveTemplateService {

    private final LeaveTemplateRepository leaveTemplateRepository;
    private final StaffLeaveTemplateMappingRepository mappingRepository;
    private final LeaveTypeConfigRepository leaveTypeConfigRepository;
    private final StaffRepository staffRepository;
    private final StaffDesignationRepository staffDesignationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LeaveTemplateResponseDTO> list(String academicYear, StaffCategory category) {
        List<LeaveTemplate> templates;
        if (academicYear != null && category != null) {
            templates = leaveTemplateRepository.findByAcademicYearAndApplicableCategoryAndActiveTrue(academicYear, category);
        } else if (academicYear != null) {
            templates = leaveTemplateRepository.findByAcademicYearAndActiveTrue(academicYear);
        } else if (category != null) {
            templates = leaveTemplateRepository.findByApplicableCategoryAndActiveTrue(category);
        } else {
            templates = leaveTemplateRepository.findByActiveTrue();
        }
        return templates.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveTemplateResponseDTO getByIdentifier(String identifier) {
        return toResponse(findActiveByIdentifier(identifier));
    }

    @Override
    @Transactional
    public LeaveTemplateResponseDTO create(LeaveTemplateCreateDTO dto) {
        LeaveTemplate template = new LeaveTemplate();
        template.setTemplateName(dto.templateName());
        template.setDescription(dto.description());
        template.setAcademicYear(dto.academicYear());
        template.setApplicableCategory(dto.applicableCategory());
        template.setActive(true);

        applyItems(template, dto.items());

        return toResponse(leaveTemplateRepository.save(template));
    }

    @Override
    @Transactional
    public LeaveTemplateResponseDTO update(String identifier, LeaveTemplateUpdateDTO dto) {
        LeaveTemplate template = findActiveByIdentifier(identifier);
        
        template.setTemplateName(dto.templateName());
        template.setDescription(dto.description());
        template.setApplicableCategory(dto.applicableCategory());
        
        template.getItems().clear();
        applyItems(template, dto.items());

        return toResponse(leaveTemplateRepository.save(template));
    }

    @Override
    @Transactional
    public void delete(String identifier) {
        LeaveTemplate template = findActiveByIdentifier(identifier);
        template.setActive(false);
        leaveTemplateRepository.save(template);
    }

    @Override
    @Transactional
    public StaffLeaveTemplateMappingResponseDTO assignToStaff(String templateRef, StaffLeaveTemplateMappingRequestDTO dto) {
        LeaveTemplate template = findActiveByIdentifier(templateRef);
        Staff staff = findStaff(dto.staffRef());
        
        return toMappingResponse(assignTemplateToStaff(template, staff, dto.academicYear()));
    }

    @Override
    @Transactional
    public BulkOperationResultDTO bulkAssignByDesignation(String templateRef, BulkAssignByDesignationDTO dto) {
        LeaveTemplate template = findActiveByIdentifier(templateRef);
        StaffDesignation designation = findDesignation(dto.designationRef());
        
        List<Staff> staffList = staffRepository.findByDesignation_IdAndIsActiveTrue(designation.getId());
        
        int successCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (Staff staff : staffList) {
            try {
                // If staff category doesn't match template category (and template category is not null), skip
                if (template.getApplicableCategory() != null && staff.getCategory() != template.getApplicableCategory()) {
                    errors.add("Staff " + staff.getEmployeeId() + " category mismatch");
                    continue;
                }
                
                assignTemplateToStaff(template, staff, dto.academicYear());
                successCount++;
            } catch (Exception e) {
                log.error("Failed to assign template to staff: {}", staff.getEmployeeId(), e);
                errors.add("Staff " + staff.getEmployeeId() + ": " + e.getMessage());
            }
        }
        
        return new BulkOperationResultDTO(staffList.size(), successCount, staffList.size() - successCount, errors.isEmpty() ? null : errors);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffLeaveTemplateMappingResponseDTO> getStaffMappings(String staffRef) {
        Staff staff = findStaff(staffRef);
        return mappingRepository.findByStaffIdAndActiveTrue(staff.getId())
                .stream().map(this::toMappingResponse).toList();
    }
    
    // --- Private Helpers ---

    private StaffLeaveTemplateMapping assignTemplateToStaff(LeaveTemplate template, Staff staff, String academicYear) {
        Optional<StaffLeaveTemplateMapping> existingOpt = mappingRepository.findByStaffIdAndAcademicYearAndActiveTrue(staff.getId(), academicYear);
        
        StaffLeaveTemplateMapping mapping;
        if (existingOpt.isPresent()) {
            mapping = existingOpt.get();
            mapping.setTemplate(template);
        } else {
            mapping = new StaffLeaveTemplateMapping();
            mapping.setStaff(staff);
            mapping.setTemplate(template);
            mapping.setAcademicYear(academicYear);
            mapping.setEffectiveFrom(LocalDate.now());
            mapping.setActive(true);
        }
        
        return mappingRepository.save(mapping);
    }

    private void applyItems(LeaveTemplate template, List<LeaveTemplateItemRequestDTO> dtos) {
        if (dtos == null) return;
        
        for (LeaveTemplateItemRequestDTO itemDto : dtos) {
            LeaveTypeConfig leaveType = PublicIdentifierResolver.resolve(
                    itemDto.leaveTypeRef(),
                    leaveTypeConfigRepository::findByUuid,
                    leaveTypeConfigRepository::findById,
                    "Leave type"
            );
            
            LeaveTemplateItem item = new LeaveTemplateItem();
            item.setTemplate(template);
            item.setLeaveType(leaveType);
            item.setCustomQuota(itemDto.customQuota());
            template.getItems().add(item);
        }
    }

    private LeaveTemplate findActiveByIdentifier(String identifier) {
        LeaveTemplate template = PublicIdentifierResolver.resolve(
                identifier,
                leaveTemplateRepository::findByUuid,
                leaveTemplateRepository::findById,
                "Leave template"
        );
        if (!template.isActive()) {
            throw new ResourceNotFoundException("Leave template not found: " + identifier);
        }
        return template;
    }

    private Staff findStaff(String staffRef) {
        Staff staff = PublicIdentifierResolver.resolve(
                staffRef,
                staffRepository::findByUuid,
                staffRepository::findById,
                "Staff"
        );
        if (!staff.isActive()) {
            throw new EdusyncException("Staff is not active", HttpStatus.BAD_REQUEST);
        }
        return staff;
    }

    private StaffDesignation findDesignation(String ref) {
        return PublicIdentifierResolver.resolve(
                ref,
                staffDesignationRepository::findByUuid,
                staffDesignationRepository::findById,
                "Staff designation"
        );
    }

    private LeaveTemplateResponseDTO toResponse(LeaveTemplate template) {
        List<LeaveTemplateItemDTO> itemDTOs = template.getItems().stream()
                .filter(LeaveTemplateItem::isActive)
                .map(item -> new LeaveTemplateItemDTO(
                        item.getId(),
                        item.getUuid() != null ? item.getUuid().toString() : null,
                        item.getLeaveType().getId(),
                        item.getLeaveType().getLeaveCode(),
                        item.getLeaveType().getDisplayName(),
                        item.getLeaveType().getAnnualQuota(),
                        item.getCustomQuota()
                )).toList();

        return new LeaveTemplateResponseDTO(
                template.getId(),
                template.getUuid() != null ? template.getUuid().toString() : null,
                template.getTemplateName(),
                template.getDescription(),
                template.getAcademicYear(),
                template.getApplicableCategory(),
                template.isActive(),
                template.getCreatedAt(),
                template.getUpdatedAt(),
                itemDTOs
        );
    }

    private StaffLeaveTemplateMappingResponseDTO toMappingResponse(StaffLeaveTemplateMapping mapping) {
        Staff staff = mapping.getStaff();
        LeaveTemplate template = mapping.getTemplate();
        
        return new StaffLeaveTemplateMappingResponseDTO(
                mapping.getId(),
                mapping.getUuid() != null ? mapping.getUuid().toString() : null,
                staff.getId(),
                staff.getEmployeeId(),
                staff.getUserProfile().getFirstName() + " " + staff.getUserProfile().getLastName(),
                template.getId(),
                template.getUuid() != null ? template.getUuid().toString() : null,
                template.getTemplateName(),
                mapping.getAcademicYear(),
                mapping.getEffectiveFrom(),
                mapping.getEffectiveTo(),
                mapping.isActive(),
                mapping.getCreatedAt()
        );
    }
}
