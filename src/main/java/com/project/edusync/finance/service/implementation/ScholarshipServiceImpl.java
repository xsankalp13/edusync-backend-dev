package com.project.edusync.finance.service.implementation;

import com.project.edusync.finance.dto.scholarship.ScholarshipAssignmentCreateDTO;
import com.project.edusync.finance.dto.scholarship.ScholarshipAssignmentDTO;
import com.project.edusync.finance.dto.scholarship.ScholarshipTypeCreateDTO;
import com.project.edusync.finance.dto.scholarship.ScholarshipTypeDTO;
import com.project.edusync.finance.mapper.ScholarshipMapper;
import com.project.edusync.finance.model.entity.ScholarshipAssignment;
import com.project.edusync.finance.model.entity.ScholarshipType;
import com.project.edusync.finance.repository.ScholarshipAssignmentRepository;
import com.project.edusync.finance.repository.ScholarshipTypeRepository;
import com.project.edusync.finance.service.ScholarshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScholarshipServiceImpl implements ScholarshipService {

    private final ScholarshipTypeRepository typeRepository;
    private final ScholarshipAssignmentRepository assignmentRepository;
    private final ScholarshipMapper mapper;

    @Override
    @Transactional
    public ScholarshipTypeDTO createType(ScholarshipTypeCreateDTO dto) {
        ScholarshipType type = ScholarshipType.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .discountType(dto.getDiscountType())
                .discountValue(dto.getDiscountValue())
                .eligibilityCriteria(dto.getEligibilityCriteria())
                .maxRecipients(dto.getMaxRecipients())
                .activeCount(0)
                .totalDiscountIssued(BigDecimal.ZERO)
                .build();
        return mapper.toDto(typeRepository.save(type));
    }

    @Override
    public List<ScholarshipTypeDTO> getAllTypes() {
        return typeRepository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ScholarshipAssignmentDTO assignScholarship(ScholarshipAssignmentCreateDTO dto) {
        ScholarshipType type = typeRepository.findById(dto.getScholarshipId())
                .orElseThrow(() -> new RuntimeException("Scholarship Type not found"));

        if (type.getMaxRecipients() != null && type.getActiveCount() >= type.getMaxRecipients()) {
            throw new RuntimeException("Scholarship max recipients reached");
        }

        ScholarshipAssignment assignment = ScholarshipAssignment.builder()
                .studentId(dto.getStudentId())
                .studentName(dto.getStudentName())
                .scholarshipType(type)
                .discountType(type.getDiscountType())
                .discountValue(type.getDiscountValue())
                .effectiveFrom(LocalDate.now())
                .reason(dto.getReason())
                .status("ACTIVE")
                .build();

        type.setActiveCount(type.getActiveCount() + 1);
        typeRepository.save(type);

        return mapper.toDto(assignmentRepository.save(assignment));
    }

    @Override
    public List<ScholarshipAssignmentDTO> getAllAssignments() {
        return assignmentRepository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ScholarshipAssignmentDTO revokeAssignment(Long assignmentId) {
        ScholarshipAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        
        if ("ACTIVE".equalsIgnoreCase(assignment.getStatus())) {
            assignment.setStatus("REVOKED");
            assignment.setEffectiveTo(LocalDate.now());
            
            ScholarshipType type = assignment.getScholarshipType();
            type.setActiveCount(Math.max(0, type.getActiveCount() - 1));
            typeRepository.save(type);
        }
        
        return mapper.toDto(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    public ScholarshipAssignmentDTO activateAssignment(Long assignmentId) {
        ScholarshipAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        if (!"ACTIVE".equalsIgnoreCase(assignment.getStatus())) {
            ScholarshipType type = assignment.getScholarshipType();
            
            // Check capacity
            if (type.getMaxRecipients() != null && type.getActiveCount() >= type.getMaxRecipients()) {
                throw new RuntimeException("Cannot activate: Scholarship max recipients reached");
            }

            assignment.setStatus("ACTIVE");
            assignment.setEffectiveTo(null);
            
            type.setActiveCount(type.getActiveCount() + 1);
            typeRepository.save(type);
        }

        return mapper.toDto(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    public void deleteAssignment(Long assignmentId) {
        ScholarshipAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        // If it was active, decrease the count
        if ("ACTIVE".equalsIgnoreCase(assignment.getStatus())) {
            ScholarshipType type = assignment.getScholarshipType();
            type.setActiveCount(Math.max(0, type.getActiveCount() - 1));
            typeRepository.save(type);
        }

        assignmentRepository.delete(assignment);
    }

    @Override
    @Transactional
    public void deleteBulkAssignments(List<Long> assignmentIds) {
        for (Long id : assignmentIds) {
            deleteAssignment(id);
        }
    }
}
