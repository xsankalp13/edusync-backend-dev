package com.project.edusync.em.model.service;

import com.project.edusync.common.exception.BadRequestException;
import com.project.edusync.em.model.dto.request.GradingScaleRequestDTO;
import com.project.edusync.em.model.dto.response.GradingScaleResponseDTO;
import com.project.edusync.em.model.entity.GradingScale;
import com.project.edusync.em.model.repository.GradingScaleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradingScaleService {
    private final GradingScaleRepository gradingScaleRepository;

    @Transactional
    public GradingScaleResponseDTO createGrade(GradingScaleRequestDTO dto) {
        validateGradingScale(dto, null);
        GradingScale scale = new GradingScale();
        scale.setMinMarks(dto.getMinMarks());
        scale.setMaxMarks(dto.getMaxMarks());
        scale.setGrade(dto.getGrade());
        GradingScale saved = gradingScaleRepository.save(scale);
        return toResponse(saved);
    }

    public List<GradingScaleResponseDTO> getGrades() {
        return gradingScaleRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    private void validateGradingScale(GradingScaleRequestDTO dto, Long excludeId) {
        if (dto.getMinMarks() == null || dto.getMaxMarks() == null) {
            throw new BadRequestException("Min and max marks are required");
        }
        if (dto.getMinMarks() >= dto.getMaxMarks()) {
            throw new BadRequestException("minMarks must be less than maxMarks");
        }
        List<GradingScale> all = gradingScaleRepository.findAll();
        for (GradingScale scale : all) {
            if (excludeId != null && scale.getId().equals(excludeId)) continue;
            if (rangesOverlap(dto.getMinMarks(), dto.getMaxMarks(), scale.getMinMarks(), scale.getMaxMarks())) {
                throw new BadRequestException("Grade range overlaps with existing grade: " + scale.getGrade());
            }
            if (scale.getGrade().equalsIgnoreCase(dto.getGrade()) && (excludeId == null || !scale.getId().equals(excludeId))) {
                throw new BadRequestException("Grade must be unique");
            }
        }
    }

    private boolean rangesOverlap(int min1, int max1, int min2, int max2) {
        return min1 <= max2 && max1 >= min2;
    }

    private GradingScaleResponseDTO toResponse(GradingScale scale) {
        GradingScaleResponseDTO dto = new GradingScaleResponseDTO();
        dto.setId(scale.getId());
        dto.setMinMarks(scale.getMinMarks());
        dto.setMaxMarks(scale.getMaxMarks());
        dto.setGrade(scale.getGrade());
        return dto;
    }
}

