package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.designation.StaffDesignationCreateUpdateDTO;
import com.project.edusync.hrms.dto.designation.StaffDesignationResponseDTO;
import com.project.edusync.hrms.model.entity.StaffDesignation;
import com.project.edusync.hrms.repository.StaffDesignationRepository;
import com.project.edusync.hrms.service.StaffDesignationService;
import com.project.edusync.uis.model.enums.StaffCategory;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffDesignationServiceImpl implements StaffDesignationService {

    private final StaffDesignationRepository staffDesignationRepository;
    private final StaffRepository staffRepository;

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
                designation.getCreatedAt(),
                designation.getUpdatedAt()
        );
    }
}


