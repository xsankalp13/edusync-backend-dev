package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.grade.StaffGradeAssignmentCreateDTO;
import com.project.edusync.hrms.dto.grade.StaffGradeAssignmentResponseDTO;
import com.project.edusync.hrms.model.entity.StaffGrade;
import com.project.edusync.hrms.model.entity.StaffGradeAssignment;
import com.project.edusync.hrms.repository.StaffGradeAssignmentRepository;
import com.project.edusync.hrms.repository.StaffGradeRepository;
import com.project.edusync.hrms.service.StaffGradeAssignmentService;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffGradeAssignmentServiceImpl implements StaffGradeAssignmentService {

    private final StaffGradeAssignmentRepository assignmentRepository;
    private final StaffGradeRepository staffGradeRepository;
    private final StaffRepository staffRepository;
    private final AuthUtil authUtil;

    @Override
    @Transactional
    public StaffGradeAssignmentResponseDTO assign(StaffGradeAssignmentCreateDTO dto) {
        Staff staff = findActiveStaffByIdentifier(dto.staffRef());
        if (!staff.isActive()) {
            throw new EdusyncException("Cannot assign grade to inactive staff", HttpStatus.BAD_REQUEST);
        }

        StaffGrade grade = findActiveGradeByIdentifier(dto.gradeRef());
        if (!grade.isActive()) {
            throw new EdusyncException("Selected grade is inactive", HttpStatus.BAD_REQUEST);
        }

        StaffGradeAssignment current = assignmentRepository.findByStaff_IdAndActiveTrueAndEffectiveToIsNull(staff.getId())
                .orElse(null);

        if (current != null) {
            if (dto.effectiveFrom().isBefore(current.getEffectiveFrom())) {
                throw new EdusyncException("effectiveFrom cannot be before current active assignment start date", HttpStatus.BAD_REQUEST);
            }
            current.setEffectiveTo(dto.effectiveFrom().minusDays(1));
            assignmentRepository.save(current);
        }

        Staff promotedBy = staffRepository.findByUserProfile_User_Id(authUtil.getCurrentUserId()).orElse(null);

        StaffGradeAssignment assignment = new StaffGradeAssignment();
        assignment.setStaff(staff);
        assignment.setGrade(grade);
        assignment.setEffectiveFrom(dto.effectiveFrom());
        assignment.setPromotionOrderRef(dto.promotionOrderRef());
        assignment.setPromotedBy(promotedBy);
        assignment.setRemarks(dto.remarks());

        return toResponse(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional(readOnly = true)
    public StaffGradeAssignmentResponseDTO getCurrentAssignment(Long staffId) {
        StaffGradeAssignment assignment = assignmentRepository.findByStaff_IdAndActiveTrueAndEffectiveToIsNull(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Current grade assignment not found for staff id: " + staffId));
        return toResponse(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffGradeAssignmentResponseDTO getCurrentAssignmentByIdentifier(String staffIdentifier) {
        Staff staff = findActiveStaffByIdentifier(staffIdentifier);
        return getCurrentAssignment(staff.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffGradeAssignmentResponseDTO> getHistory(Long staffId) {
        return assignmentRepository.findByStaff_IdAndActiveTrueOrderByEffectiveFromDesc(staffId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffGradeAssignmentResponseDTO> getHistoryByIdentifier(String staffIdentifier) {
        Staff staff = findActiveStaffByIdentifier(staffIdentifier);
        return getHistory(staff.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StaffGradeAssignmentResponseDTO> listCurrentAssignments(Pageable pageable) {
        return assignmentRepository.findByActiveTrueAndEffectiveToIsNull(pageable)
                .map(this::toResponse);
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

    private StaffGrade findActiveGradeByIdentifier(String identifier) {
        StaffGrade grade = PublicIdentifierResolver.resolve(
                identifier,
                staffGradeRepository::findByUuid,
                staffGradeRepository::findById,
                "Staff grade"
        );
        if (!grade.isActive()) {
            throw new ResourceNotFoundException("Staff grade not found with identifier: " + identifier);
        }
        return grade;
    }

    private StaffGradeAssignmentResponseDTO toResponse(StaffGradeAssignment assignment) {
        Staff staff = assignment.getStaff();
        String staffName = (staff.getUserProfile().getFirstName() + " " + staff.getUserProfile().getLastName()).trim();

        return new StaffGradeAssignmentResponseDTO(
                assignment.getId(),
                assignment.getUuid() != null ? assignment.getUuid().toString() : null,
                staff.getId(),
                staffName,
                assignment.getGrade().getId(),
                assignment.getGrade().getGradeCode(),
                assignment.getGrade().getGradeName(),
                assignment.getEffectiveFrom(),
                assignment.getEffectiveTo(),
                assignment.getPromotionOrderRef(),
                assignment.getPromotedBy() != null ? assignment.getPromotedBy().getId() : null,
                assignment.getRemarks(),
                assignment.getCreatedAt()
        );
    }
}

