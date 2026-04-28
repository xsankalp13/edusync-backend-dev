package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.designation.BulkDesignationAssignRequestDTO;
import com.project.edusync.hrms.dto.designation.BulkDesignationAssignResultDTO;
import com.project.edusync.hrms.dto.designation.StaffDesignationCreateUpdateDTO;
import com.project.edusync.hrms.dto.designation.StaffDesignationResponseDTO;
import com.project.edusync.hrms.model.entity.SalaryTemplate;
import com.project.edusync.hrms.model.entity.StaffDesignation;
import com.project.edusync.hrms.model.entity.StaffGrade;
import com.project.edusync.hrms.model.entity.StaffSalaryMapping;
import com.project.edusync.hrms.repository.SalaryTemplateRepository;
import com.project.edusync.hrms.repository.StaffDesignationRepository;
import com.project.edusync.hrms.repository.StaffGradeRepository;
import com.project.edusync.hrms.repository.StaffSalaryMappingRepository;
import com.project.edusync.hrms.service.StaffDesignationService;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffDesignationServiceImpl implements StaffDesignationService {

    private final StaffDesignationRepository staffDesignationRepository;
    private final StaffRepository staffRepository;
    private final SalaryTemplateRepository salaryTemplateRepository;
    private final StaffGradeRepository staffGradeRepository;
    private final StaffSalaryMappingRepository staffSalaryMappingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StaffDesignationResponseDTO> list(StaffCategory category, Boolean active) {
        List<StaffDesignation> designations;
        if (category != null && active != null) {
            designations = staffDesignationRepository.findByCategoryAndActiveOrderBySortOrderAscDesignationNameAsc(category, active);
        } else if (category != null) {
            designations = staffDesignationRepository.findByCategoryOrderBySortOrderAscDesignationNameAsc(category);
        } else if (active != null) {
            designations = staffDesignationRepository.findByActiveOrderBySortOrderAscDesignationNameAsc(active);
        } else {
            designations = staffDesignationRepository.findByActiveTrueOrderBySortOrderAscDesignationNameAsc();
        }
        return designations.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StaffDesignationResponseDTO getById(Long designationId) {
        return toResponse(findActiveById(designationId));
    }

    @Override
    @Transactional(readOnly = true)
    public StaffDesignationResponseDTO getByIdentifier(String identifier) {
        return toResponse(findActiveByIdentifier(identifier));
    }

    @Override
    @Transactional
    public StaffDesignationResponseDTO create(StaffDesignationCreateUpdateDTO dto) {
        String code = normalizeCode(dto.designationCode());
        if (staffDesignationRepository.existsByDesignationCodeIgnoreCaseAndActiveTrue(code)) {
            throw new EdusyncException("Designation code already exists: " + code, HttpStatus.CONFLICT);
        }

        StaffDesignation designation = new StaffDesignation();
        apply(designation, code, dto);
        return toResponse(staffDesignationRepository.save(designation));
    }

    @Override
    @Transactional
    public StaffDesignationResponseDTO update(Long designationId, StaffDesignationCreateUpdateDTO dto) {
        StaffDesignation designation = findActiveById(designationId);
        String code = normalizeCode(dto.designationCode());

        if (staffDesignationRepository.existsByDesignationCodeIgnoreCaseAndActiveTrueAndIdNot(code, designationId)) {
            throw new EdusyncException("Designation code already exists: " + code, HttpStatus.CONFLICT);
        }

        apply(designation, code, dto);
        return toResponse(staffDesignationRepository.save(designation));
    }

    @Override
    @Transactional
    public StaffDesignationResponseDTO updateByIdentifier(String identifier, StaffDesignationCreateUpdateDTO dto) {
        StaffDesignation designation = findActiveByIdentifier(identifier);
        return update(designation.getId(), dto);
    }

    @Override
    @Transactional
    public void delete(Long designationId) {
        StaffDesignation designation = findActiveById(designationId);
        long linkedStaffCount = staffRepository.countByDesignation_IdAndIsActiveTrue(designationId);
        if (linkedStaffCount > 0) {
            throw new EdusyncException(
                    "Cannot delete: " + linkedStaffCount + " staff members are using this designation",
                    HttpStatus.BAD_REQUEST
            );
        }

        designation.setActive(false);
        staffDesignationRepository.save(designation);
    }

    @Override
    @Transactional
    public void deleteByIdentifier(String identifier) {
        StaffDesignation designation = findActiveByIdentifier(identifier);
        delete(designation.getId());
    }

    /**
     * Bulk-assigns the resolved designation to each staff ref (UUID or employeeId).
     * Processes each entry independently so one bad ref never aborts the batch.
     */
    @Override
    @Transactional
    public BulkDesignationAssignResultDTO bulkAssignToDesignation(
            String designationRef, BulkDesignationAssignRequestDTO dto) {

        StaffDesignation designation = findActiveByIdentifier(designationRef);
        log.info("[BulkDesignationAssign] Assigning {} staff to designation '{}'",
                dto.staffRefs().size(), designation.getDesignationCode());

        List<String> successfulEmployeeIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (String ref : dto.staffRefs()) {
            try {
                Staff staff = resolveStaff(ref);
                staff.setDesignation(designation);
                staff.setJobTitle(designation.getDesignationName());
                staffRepository.save(staff);
                successfulEmployeeIds.add(staff.getEmployeeId());

                // ── Auto-Provision: create salary mapping if designation has a default template
                //    and the staff does NOT already have an active salary mapping ──
                autoProvisionSalaryMapping(staff, designation);

                log.info("[BulkDesignationAssign] Assigned employeeId='{}' to designation '{}'",
                        staff.getEmployeeId(), designation.getDesignationCode());
            } catch (Exception e) {
                String msg = String.format("ref '%s': %s", ref, e.getMessage());
                errors.add(msg);
                log.warn("[BulkDesignationAssign] Failed for ref='{}': {}", ref, e.getMessage());
            }
        }

        int total = dto.staffRefs().size();
        int success = successfulEmployeeIds.size();
        log.info("[BulkDesignationAssign] Completed: total={}, success={}, failed={}",
                total, success, errors.size());

        return new BulkDesignationAssignResultDTO(
                designation.getDesignationCode(),
                designation.getDesignationName(),
                total,
                success,
                errors.size(),
                successfulEmployeeIds,
                errors
        );
    }

    /**
     * Auto-provisions a salary mapping for the given staff from the designation's
     * default salary template — but ONLY if the staff has no active current mapping.
     * This is safe and non-destructive: existing negotiated salaries are never overwritten.
     */
    private void autoProvisionSalaryMapping(Staff staff, StaffDesignation designation) {
        SalaryTemplate defaultTemplate = designation.getDefaultSalaryTemplate();
        if (defaultTemplate == null || !defaultTemplate.isActive()) {
            return;
        }

        boolean hasActiveMapping = staffSalaryMappingRepository
                .findByStaff_IdAndActiveTrueOrderByEffectiveFromDesc(staff.getId())
                .stream()
                .anyMatch(m -> m.getEffectiveTo() == null || !m.getEffectiveTo().isBefore(LocalDate.now()));

        if (hasActiveMapping) {
            log.info("[AutoProvision] Staff '{}' already has an active salary mapping — skipping",
                    staff.getEmployeeId());
            return;
        }

        StaffSalaryMapping mapping = new StaffSalaryMapping();
        mapping.setStaff(staff);
        mapping.setTemplate(defaultTemplate);
        mapping.setEffectiveFrom(LocalDate.now());
        mapping.setRemarks("Auto-provisioned from designation: " + designation.getDesignationCode());
        staffSalaryMappingRepository.save(mapping);

        log.info("[AutoProvision] Created salary mapping for staff '{}' using template '{}' from designation '{}'",
                staff.getEmployeeId(), defaultTemplate.getTemplateName(), designation.getDesignationCode());
    }

    /**
     * Bulk-unassigns staff from their current designation.
     * Clears designation to null, reverts jobTitle to StaffType name.
     * Salary mappings are preserved (staff keeps salary even after losing designation).
     */
    @Override
    @Transactional
    public BulkDesignationAssignResultDTO bulkUnassignFromDesignation(BulkDesignationAssignRequestDTO dto) {
        log.info("[BulkDesignationUnassign] Unassigning {} staff from their designations",
                dto.staffRefs().size());

        List<String> successfulEmployeeIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (String ref : dto.staffRefs()) {
            try {
                Staff staff = resolveStaff(ref);
                String previousDesignation = staff.getDesignation() != null
                        ? staff.getDesignation().getDesignationCode() : "NONE";

                staff.setDesignation(null);
                // Fallback jobTitle to StaffType name to satisfy NOT NULL constraint
                staff.setJobTitle(staff.getStaffType() != null
                        ? staff.getStaffType().name().replace("_", " ")
                        : "Staff");
                staffRepository.save(staff);
                successfulEmployeeIds.add(staff.getEmployeeId());

                log.info("[BulkDesignationUnassign] Unassigned employeeId='{}' from designation '{}'",
                        staff.getEmployeeId(), previousDesignation);
            } catch (Exception e) {
                String msg = String.format("ref '%s': %s", ref, e.getMessage());
                errors.add(msg);
                log.warn("[BulkDesignationUnassign] Failed for ref='{}': {}", ref, e.getMessage());
            }
        }

        int total = dto.staffRefs().size();
        int success = successfulEmployeeIds.size();
        log.info("[BulkDesignationUnassign] Completed: total={}, success={}, failed={}",
                total, success, errors.size());

        return new BulkDesignationAssignResultDTO(
                null, // no single designation for unassign
                "Unassigned",
                total,
                success,
                errors.size(),
                successfulEmployeeIds,
                errors
        );
    }

    /**
     * Resolves a Staff entity from a ref that is either a UUID string or an employeeId.
     */
    private Staff resolveStaff(String ref) {
        if (ref == null || ref.isBlank()) {
            throw new ResourceNotFoundException("Staff ref cannot be blank");
        }
        // Try UUID first
        try {
            UUID uuid = UUID.fromString(ref.trim());
            Optional<Staff> byUuid = staffRepository.findByUuid(uuid);
            if (byUuid.isPresent()) return byUuid.get();
        } catch (IllegalArgumentException ignored) { /* not a UUID */ }
        // Fall back to employeeId
        return staffRepository.findByEmployeeId(ref.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Staff not found for ref '" + ref + "' (tried UUID and employeeId)"));
    }

    private StaffDesignation findActiveByIdentifier(String identifier) {
        StaffDesignation designation = PublicIdentifierResolver.resolve(
                identifier,
                staffDesignationRepository::findByUuid,
                staffDesignationRepository::findById,
                "Staff designation"
        );
        if (!designation.isActive()) {
            throw new ResourceNotFoundException("Staff designation not found with identifier: " + identifier);
        }
        return designation;
    }

    private StaffDesignation findActiveById(Long designationId) {
        StaffDesignation designation = staffDesignationRepository.findById(designationId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff designation not found with id: " + designationId));

        if (!designation.isActive()) {
            throw new ResourceNotFoundException("Staff designation not found with id: " + designationId);
        }
        return designation;
    }

    private void apply(StaffDesignation designation, String code, StaffDesignationCreateUpdateDTO dto) {
        designation.setDesignationCode(code);
        designation.setDesignationName(dto.designationName().trim());
        designation.setCategory(dto.category());
        designation.setDescription(dto.description());
        designation.setSortOrder(dto.sortOrder() == null ? 0 : dto.sortOrder());

        if (dto.defaultSalaryTemplateRef() != null && !dto.defaultSalaryTemplateRef().isBlank()) {
            SalaryTemplate template = PublicIdentifierResolver.resolve(
                    dto.defaultSalaryTemplateRef(),
                    salaryTemplateRepository::findByUuid,
                    salaryTemplateRepository::findById,
                    "Salary template"
            );
            designation.setDefaultSalaryTemplate(template);
        } else {
            designation.setDefaultSalaryTemplate(null);
        }

        if (dto.defaultGradeRef() != null && !dto.defaultGradeRef().isBlank()) {
            StaffGrade grade = PublicIdentifierResolver.resolve(
                    dto.defaultGradeRef(),
                    staffGradeRepository::findByUuid,
                    staffGradeRepository::findById,
                    "Staff grade"
            );
            designation.setDefaultGrade(grade);
        } else {
            designation.setDefaultGrade(null);
        }
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }

    private StaffDesignationResponseDTO toResponse(StaffDesignation designation) {
        return new StaffDesignationResponseDTO(
                designation.getId(),
                designation.getUuid() != null ? designation.getUuid().toString() : null,
                designation.getDesignationCode(),
                designation.getDesignationName(),
                designation.getCategory(),
                designation.getDescription(),
                designation.getSortOrder(),
                designation.isActive(),
                designation.getDefaultSalaryTemplate() != null ? designation.getDefaultSalaryTemplate().getId() : null,
                designation.getDefaultSalaryTemplate() != null ? designation.getDefaultSalaryTemplate().getTemplateName() : null,
                designation.getDefaultGrade() != null ? designation.getDefaultGrade().getId() : null,
                designation.getDefaultGrade() != null ? designation.getDefaultGrade().getGradeCode() : null,
                designation.getDefaultGrade() != null ? designation.getDefaultGrade().getGradeName() : null,
                designation.getCreatedAt(),
                designation.getUpdatedAt()
        );
    }
}


