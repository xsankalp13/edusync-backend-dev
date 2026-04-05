package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.leave.LeaveTypeConfigCreateDTO;
import com.project.edusync.hrms.dto.leave.LeaveTypeConfigResponseDTO;
import com.project.edusync.hrms.dto.leave.LeaveTypeConfigUpdateDTO;
import com.project.edusync.hrms.model.entity.LeaveTypeConfig;
import com.project.edusync.hrms.repository.LeaveTypeConfigRepository;
import com.project.edusync.hrms.service.LeaveTypeConfigService;
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
public class LeaveTypeConfigServiceImpl implements LeaveTypeConfigService {

    private final LeaveTypeConfigRepository leaveTypeConfigRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LeaveTypeConfigResponseDTO> getAll(StaffCategory category) {
        List<LeaveTypeConfig> leaveTypes = category == null
                ? leaveTypeConfigRepository.findByActiveTrueOrderBySortOrderAscLeaveCodeAsc()
                : leaveTypeConfigRepository.findApplicableForCategory(category);
        return leaveTypes.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveTypeConfigResponseDTO getById(Long leaveTypeId) {
        return toResponse(findActiveById(leaveTypeId));
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveTypeConfigResponseDTO getByIdentifier(String identifier) {
        return toResponse(findActiveByIdentifier(identifier));
    }

    @Override
    @Transactional
    public LeaveTypeConfigResponseDTO create(LeaveTypeConfigCreateDTO dto) {
        String code = normalizeCode(dto.leaveCode());
        if (leaveTypeConfigRepository.existsByLeaveCodeIgnoreCaseAndActiveTrue(code)) {
            throw new EdusyncException("Leave type code already exists: " + code, HttpStatus.CONFLICT);
        }

        LeaveTypeConfig entity = new LeaveTypeConfig();
        applyDto(entity, code, dto.displayName(), dto.description(), dto.annualQuota(), dto.carryForwardAllowed(), dto.maxCarryForward(),
                dto.encashmentAllowed(), dto.minDaysBeforeApply(), dto.maxConsecutiveDays(), dto.requiresDocument(), dto.documentRequiredAfterDays(),
                dto.isPaid(), dto.applicableCategories(), dto.applicableGrades(), dto.sortOrder());

        return toResponse(leaveTypeConfigRepository.save(entity));
    }

    @Override
    @Transactional
    public LeaveTypeConfigResponseDTO update(Long leaveTypeId, LeaveTypeConfigUpdateDTO dto) {
        LeaveTypeConfig entity = findActiveById(leaveTypeId);
        String code = normalizeCode(dto.leaveCode());

        if (leaveTypeConfigRepository.existsByLeaveCodeIgnoreCaseAndActiveTrueAndIdNot(code, leaveTypeId)) {
            throw new EdusyncException("Leave type code already exists: " + code, HttpStatus.CONFLICT);
        }

        applyDto(entity, code, dto.displayName(), dto.description(), dto.annualQuota(), dto.carryForwardAllowed(), dto.maxCarryForward(),
                dto.encashmentAllowed(), dto.minDaysBeforeApply(), dto.maxConsecutiveDays(), dto.requiresDocument(), dto.documentRequiredAfterDays(),
                dto.isPaid(), dto.applicableCategories(), dto.applicableGrades(), dto.sortOrder());

        return toResponse(leaveTypeConfigRepository.save(entity));
    }

    @Override
    @Transactional
    public LeaveTypeConfigResponseDTO updateByIdentifier(String identifier, LeaveTypeConfigUpdateDTO dto) {
        LeaveTypeConfig entity = findActiveByIdentifier(identifier);
        return update(entity.getId(), dto);
    }

    @Override
    @Transactional
    public void delete(Long leaveTypeId) {
        LeaveTypeConfig entity = findActiveById(leaveTypeId);
        entity.setActive(false);
        leaveTypeConfigRepository.save(entity);
    }

    @Override
    @Transactional
    public void deleteByIdentifier(String identifier) {
        LeaveTypeConfig entity = findActiveByIdentifier(identifier);
        delete(entity.getId());
    }

    private LeaveTypeConfig findActiveByIdentifier(String identifier) {
        LeaveTypeConfig entity = PublicIdentifierResolver.resolve(
                identifier,
                leaveTypeConfigRepository::findByUuid,
                leaveTypeConfigRepository::findById,
                "Leave type"
        );
        if (!entity.isActive()) {
            throw new ResourceNotFoundException("Leave type not found with identifier: " + identifier);
        }
        return entity;
    }

    private LeaveTypeConfig findActiveById(Long leaveTypeId) {
        LeaveTypeConfig entity = leaveTypeConfigRepository.findById(leaveTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id: " + leaveTypeId));

        if (!entity.isActive()) {
            throw new ResourceNotFoundException("Leave type not found with id: " + leaveTypeId);
        }
        return entity;
    }

    private void applyDto(
            LeaveTypeConfig entity,
            String leaveCode,
            String displayName,
            String description,
            Integer annualQuota,
            Boolean carryForwardAllowed,
            Integer maxCarryForward,
            Boolean encashmentAllowed,
            Integer minDaysBeforeApply,
            Integer maxConsecutiveDays,
            Boolean requiresDocument,
            Integer documentRequiredAfterDays,
            Boolean isPaid,
            Set<StaffCategory> applicableCategories,
            Set<String> applicableGrades,
            Integer sortOrder
    ) {
        boolean carryForward = carryForwardAllowed != null && carryForwardAllowed;
        boolean documentRequired = requiresDocument != null && requiresDocument;

        if (documentRequired && documentRequiredAfterDays == null) {
            throw new EdusyncException("documentRequiredAfterDays is required when requiresDocument=true", HttpStatus.BAD_REQUEST);
        }

        entity.setLeaveCode(leaveCode);
        entity.setDisplayName(displayName);
        entity.setDescription(description);
        entity.setAnnualQuota(annualQuota);
        entity.setCarryForwardAllowed(carryForward);
        entity.setMaxCarryForward(carryForward ? defaultIfNull(maxCarryForward, 0) : 0);
        entity.setEncashmentAllowed(encashmentAllowed != null && encashmentAllowed);
        entity.setMinDaysBeforeApply(defaultIfNull(minDaysBeforeApply, 0));
        entity.setMaxConsecutiveDays(maxConsecutiveDays);
        entity.setRequiresDocument(documentRequired);
        entity.setDocumentRequiredAfterDays(documentRequired ? documentRequiredAfterDays : null);
        entity.setPaid(isPaid == null || isPaid);
        entity.setSortOrder(sortOrder);
        entity.setApplicableCategories(normalizeCategories(applicableCategories));
        entity.setApplicableGrades(normalizeGrades(applicableGrades));
    }

    private Set<StaffCategory> normalizeCategories(Set<StaffCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(categories);
    }

    private String normalizeCode(String leaveCode) {
        return leaveCode == null ? null : leaveCode.trim().toUpperCase();
    }

    private Set<String> normalizeGrades(Set<String> grades) {
        if (grades == null || grades.isEmpty()) {
            return new HashSet<>();
        }

        Set<String> normalized = new HashSet<>();
        for (String grade : grades) {
            if (grade != null && !grade.isBlank()) {
                normalized.add(grade.trim().toUpperCase());
            }
        }
        return normalized;
    }

    private int defaultIfNull(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private LeaveTypeConfigResponseDTO toResponse(LeaveTypeConfig entity) {
        return new LeaveTypeConfigResponseDTO(
                entity.getId(),
                entity.getUuid() != null ? entity.getUuid().toString() : null,
                entity.getLeaveCode(),
                entity.getDisplayName(),
                entity.getDescription(),
                entity.getAnnualQuota(),
                entity.isCarryForwardAllowed(),
                entity.getMaxCarryForward(),
                entity.isEncashmentAllowed(),
                entity.getMinDaysBeforeApply(),
                entity.getMaxConsecutiveDays(),
                entity.isRequiresDocument(),
                entity.getDocumentRequiredAfterDays(),
                entity.isPaid(),
                entity.getApplicableCategories(),
                entity.getApplicableGrades(),
                entity.isActive(),
                entity.getSortOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

