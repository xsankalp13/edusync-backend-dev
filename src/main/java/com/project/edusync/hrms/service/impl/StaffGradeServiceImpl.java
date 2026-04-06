package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.grade.StaffGradeCreateDTO;
import com.project.edusync.hrms.dto.grade.StaffGradeResponseDTO;
import com.project.edusync.hrms.dto.grade.StaffGradeUpdateDTO;
import com.project.edusync.hrms.model.entity.StaffGrade;
import com.project.edusync.hrms.repository.StaffGradeRepository;
import com.project.edusync.hrms.service.StaffGradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffGradeServiceImpl implements StaffGradeService {

    private final StaffGradeRepository staffGradeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StaffGradeResponseDTO> listGrades() {
        return staffGradeRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public StaffGradeResponseDTO createGrade(StaffGradeCreateDTO dto) {
        String code = dto.gradeCode().trim().toUpperCase();
        if (staffGradeRepository.existsByGradeCodeIgnoreCaseAndActiveTrue(code)) {
            throw new EdusyncException("Grade code already exists: " + code, HttpStatus.CONFLICT);
        }

        StaffGrade grade = new StaffGrade();
        apply(grade, code, dto);
        return toResponse(staffGradeRepository.save(grade));
    }

    @Override
    @Transactional
    public StaffGradeResponseDTO updateGrade(Long gradeId, StaffGradeUpdateDTO dto) {
        StaffGrade grade = findActiveById(gradeId);
        String code = dto.gradeCode().trim().toUpperCase();

        if (staffGradeRepository.existsByGradeCodeIgnoreCaseAndActiveTrueAndIdNot(code, gradeId)) {
            throw new EdusyncException("Grade code already exists: " + code, HttpStatus.CONFLICT);
        }

        apply(grade, code, dto);
        return toResponse(staffGradeRepository.save(grade));
    }

    @Override
    @Transactional
    public StaffGradeResponseDTO updateGradeByIdentifier(String identifier, StaffGradeUpdateDTO dto) {
        StaffGrade grade = findActiveByIdentifier(identifier);
        return updateGrade(grade.getId(), dto);
    }

    @Override
    @Transactional
    public void deleteGrade(Long gradeId) {
        StaffGrade grade = findActiveById(gradeId);
        grade.setActive(false);
        staffGradeRepository.save(grade);
    }

    @Override
    @Transactional
    public void deleteGradeByIdentifier(String identifier) {
        StaffGrade grade = findActiveByIdentifier(identifier);
        deleteGrade(grade.getId());
    }

    private StaffGrade findActiveByIdentifier(String identifier) {
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

    private StaffGrade findActiveById(Long gradeId) {
        StaffGrade grade = staffGradeRepository.findById(gradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff grade not found with id: " + gradeId));

        if (!grade.isActive()) {
            throw new ResourceNotFoundException("Staff grade not found with id: " + gradeId);
        }
        return grade;
    }

    private void apply(StaffGrade grade, String gradeCode, StaffGradeCreateDTO dto) {
        if (dto.payBandMax().compareTo(dto.payBandMin()) < 0) {
            throw new EdusyncException("payBandMax cannot be less than payBandMin", HttpStatus.BAD_REQUEST);
        }

        grade.setGradeCode(gradeCode);
        grade.setGradeName(dto.gradeName().trim());
        grade.setTeachingWing(dto.teachingWing());
        grade.setPayBandMin(dto.payBandMin());
        grade.setPayBandMax(dto.payBandMax());
        grade.setSortOrder(dto.sortOrder());
        grade.setMinYearsForPromotion(dto.minYearsForPromotion());
        grade.setDescription(dto.description());
    }

    private void apply(StaffGrade grade, String gradeCode, StaffGradeUpdateDTO dto) {
        apply(grade, gradeCode, new StaffGradeCreateDTO(
                dto.gradeCode(),
                dto.gradeName(),
                dto.teachingWing(),
                dto.payBandMin(),
                dto.payBandMax(),
                dto.sortOrder(),
                dto.minYearsForPromotion(),
                dto.description()
        ));
    }

    private StaffGradeResponseDTO toResponse(StaffGrade grade) {
        return new StaffGradeResponseDTO(
                grade.getId(),
                grade.getUuid() != null ? grade.getUuid().toString() : null,
                grade.getGradeCode(),
                grade.getGradeName(),
                grade.getTeachingWing(),
                grade.getPayBandMin(),
                grade.getPayBandMax(),
                grade.getSortOrder(),
                grade.getMinYearsForPromotion(),
                grade.getDescription(),
                grade.isActive(),
                grade.getCreatedAt(),
                grade.getUpdatedAt()
        );
    }
}

