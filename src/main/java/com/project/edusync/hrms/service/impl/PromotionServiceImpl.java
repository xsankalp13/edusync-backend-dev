package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.promotion.PromotionCreateDTO;
import com.project.edusync.hrms.dto.promotion.PromotionResponseDTO;
import com.project.edusync.hrms.dto.promotion.PromotionReviewDTO;
import com.project.edusync.hrms.model.entity.PromotionRequest;
import com.project.edusync.hrms.model.entity.SalaryTemplate;
import com.project.edusync.hrms.model.entity.StaffDesignation;
import com.project.edusync.hrms.model.entity.StaffGrade;
import com.project.edusync.hrms.model.entity.StaffGradeAssignment;
import com.project.edusync.hrms.model.entity.StaffSalaryMapping;
import com.project.edusync.hrms.model.enums.PromotionStatus;
import com.project.edusync.hrms.repository.PromotionRequestRepository;
import com.project.edusync.hrms.repository.SalaryTemplateRepository;
import com.project.edusync.hrms.repository.StaffDesignationRepository;
import com.project.edusync.hrms.repository.StaffGradeAssignmentRepository;
import com.project.edusync.hrms.repository.StaffGradeRepository;
import com.project.edusync.hrms.repository.StaffSalaryMappingRepository;
import com.project.edusync.hrms.service.PromotionService;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRequestRepository promotionRequestRepository;
    private final StaffRepository staffRepository;
    private final StaffDesignationRepository staffDesignationRepository;
    private final StaffGradeRepository staffGradeRepository;
    private final SalaryTemplateRepository salaryTemplateRepository;
    private final StaffGradeAssignmentRepository staffGradeAssignmentRepository;
    private final StaffSalaryMappingRepository staffSalaryMappingRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PromotionResponseDTO> list(PromotionStatus status, Pageable pageable) {
        if (status != null) {
            return promotionRequestRepository.findByStatusAndActiveTrue(status, pageable).map(this::toResponse);
        }
        return promotionRequestRepository.findByActiveTrue(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponseDTO getByIdentifier(String identifier) {
        return toResponse(findActiveByIdentifier(identifier));
    }

    @Override
    @Transactional
    public PromotionResponseDTO initiate(PromotionCreateDTO dto, Long initiatedByUserId) {
        Staff staff = findStaff(dto.staffRef());
        StaffDesignation currentDesignation = staff.getDesignation();

        StaffGrade currentGrade = staffGradeAssignmentRepository
                .findByStaff_IdAndActiveTrueAndEffectiveToIsNull(staff.getId())
                .map(StaffGradeAssignment::getGrade)
                .orElse(null);

        StaffDesignation proposedDesignation = findDesignation(dto.proposedDesignationRef());

        StaffGrade proposedGrade = null;
        if (dto.proposedGradeRef() != null && !dto.proposedGradeRef().isBlank()) {
            proposedGrade = findGrade(dto.proposedGradeRef());
        } else if (proposedDesignation.getDefaultGrade() != null) {
            proposedGrade = proposedDesignation.getDefaultGrade();
        }

        SalaryTemplate newSalaryTemplate = null;
        if (dto.newSalaryTemplateRef() != null && !dto.newSalaryTemplateRef().isBlank()) {
            newSalaryTemplate = findSalaryTemplate(dto.newSalaryTemplateRef());
        } else if (proposedDesignation.getDefaultSalaryTemplate() != null) {
            newSalaryTemplate = proposedDesignation.getDefaultSalaryTemplate();
        }

        PromotionRequest request = new PromotionRequest();
        request.setStaff(staff);
        request.setCurrentDesignation(currentDesignation);
        request.setProposedDesignation(proposedDesignation);
        request.setCurrentGrade(currentGrade);
        request.setProposedGrade(proposedGrade);
        request.setNewSalaryTemplate(newSalaryTemplate);
        request.setEffectiveDate(dto.effectiveDate());
        request.setStatus(PromotionStatus.PENDING);
        request.setInitiatedByUserId(initiatedByUserId);
        request.setOrderReference(dto.orderReference());
        request.setRemarks(dto.remarks());
        request.setActive(true);

        return toResponse(promotionRequestRepository.save(request));
    }

    @Override
    @Transactional
    public PromotionResponseDTO approve(String identifier, Long approverUserId, PromotionReviewDTO dto) {
        PromotionRequest request = findActiveByIdentifier(identifier);
        
        if (request.getStatus() != PromotionStatus.PENDING) {
            throw new EdusyncException("Only pending requests can be approved", HttpStatus.BAD_REQUEST);
        }

        request.setStatus(PromotionStatus.APPROVED);
        request.setApprovedByUserId(approverUserId);
        request.setApprovedOn(LocalDateTime.now());
        if (dto.remarks() != null && !dto.remarks().isBlank()) {
            request.setRemarks(request.getRemarks() + " | Approver: " + dto.remarks());
        }

        Staff staff = request.getStaff();
        staff.setDesignation(request.getProposedDesignation());
        staffRepository.save(staff);

        if (request.getProposedGrade() != null) {
            staffGradeAssignmentRepository.findByStaff_IdAndActiveTrueAndEffectiveToIsNull(staff.getId())
                    .ifPresent(assignment -> {
                        assignment.setEffectiveTo(request.getEffectiveDate().minusDays(1));
                        staffGradeAssignmentRepository.save(assignment);
                    });

            StaffGradeAssignment newAssignment = new StaffGradeAssignment();
            newAssignment.setStaff(staff);
            newAssignment.setGrade(request.getProposedGrade());
            newAssignment.setEffectiveFrom(request.getEffectiveDate());
            newAssignment.setPromotionOrderRef(request.getOrderReference());
            newAssignment.setActive(true);
            staffGradeAssignmentRepository.save(newAssignment);
        }

        if (request.getNewSalaryTemplate() != null) {
            staffSalaryMappingRepository.findByStaff_IdAndActiveTrueAndEffectiveToIsNull(staff.getId())
                    .ifPresent(mapping -> {
                        mapping.setEffectiveTo(request.getEffectiveDate().minusDays(1));
                        staffSalaryMappingRepository.save(mapping);
                    });

            StaffSalaryMapping newMapping = new StaffSalaryMapping();
            newMapping.setStaff(staff);
            newMapping.setTemplate(request.getNewSalaryTemplate());
            newMapping.setEffectiveFrom(request.getEffectiveDate());
            newMapping.setActive(true);
            staffSalaryMappingRepository.save(newMapping);
        }

        return toResponse(promotionRequestRepository.save(request));
    }

    @Override
    @Transactional
    public PromotionResponseDTO reject(String identifier, Long approverUserId, PromotionReviewDTO dto) {
        PromotionRequest request = findActiveByIdentifier(identifier);
        
        if (request.getStatus() != PromotionStatus.PENDING) {
            throw new EdusyncException("Only pending requests can be rejected", HttpStatus.BAD_REQUEST);
        }

        request.setStatus(PromotionStatus.REJECTED);
        request.setApprovedByUserId(approverUserId);
        request.setApprovedOn(LocalDateTime.now());
        if (dto.remarks() != null && !dto.remarks().isBlank()) {
            request.setRemarks(request.getRemarks() + " | Reason: " + dto.remarks());
        }

        return toResponse(promotionRequestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> getStaffHistory(String staffRef) {
        Staff staff = findStaff(staffRef);
        return promotionRequestRepository.findByStaff_IdAndActiveTrueOrderByEffectiveDateDesc(staff.getId())
                .stream().map(this::toResponse).toList();
    }

    private PromotionRequest findActiveByIdentifier(String identifier) {
        PromotionRequest request = PublicIdentifierResolver.resolve(
                identifier,
                promotionRequestRepository::findByUuid,
                promotionRequestRepository::findById,
                "Promotion request"
        );
        if (!request.isActive()) {
            throw new ResourceNotFoundException("Promotion request not found: " + identifier);
        }
        return request;
    }

    private Staff findStaff(String ref) {
        Staff staff = PublicIdentifierResolver.resolve(
                ref,
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

    private StaffGrade findGrade(String ref) {
        return PublicIdentifierResolver.resolve(
                ref,
                staffGradeRepository::findByUuid,
                staffGradeRepository::findById,
                "Staff grade"
        );
    }

    private SalaryTemplate findSalaryTemplate(String ref) {
        return PublicIdentifierResolver.resolve(
                ref,
                salaryTemplateRepository::findByUuid,
                salaryTemplateRepository::findById,
                "Salary template"
        );
    }

    private PromotionResponseDTO toResponse(PromotionRequest request) {
        return new PromotionResponseDTO(
                request.getId(),
                request.getUuid() != null ? request.getUuid().toString() : null,
                request.getStaff().getId(),
                request.getStaff().getUserProfile().getFirstName() + " " + request.getStaff().getUserProfile().getLastName(),
                request.getStaff().getEmployeeId(),
                request.getCurrentDesignation() != null ? request.getCurrentDesignation().getId() : null,
                request.getCurrentDesignation() != null ? request.getCurrentDesignation().getDesignationCode() : null,
                request.getCurrentDesignation() != null ? request.getCurrentDesignation().getDesignationName() : null,
                request.getProposedDesignation().getId(),
                request.getProposedDesignation().getDesignationCode(),
                request.getProposedDesignation().getDesignationName(),
                request.getCurrentGrade() != null ? request.getCurrentGrade().getId() : null,
                request.getCurrentGrade() != null ? request.getCurrentGrade().getGradeCode() : null,
                request.getProposedGrade() != null ? request.getProposedGrade().getId() : null,
                request.getProposedGrade() != null ? request.getProposedGrade().getGradeCode() : null,
                request.getNewSalaryTemplate() != null ? request.getNewSalaryTemplate().getId() : null,
                request.getNewSalaryTemplate() != null ? request.getNewSalaryTemplate().getTemplateName() : null,
                request.getEffectiveDate(),
                request.getStatus(),
                request.getInitiatedByUserId(),
                request.getApprovedByUserId(),
                request.getApprovedByName(),
                request.getApprovedOn(),
                request.getOrderReference(),
                request.getRemarks(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
