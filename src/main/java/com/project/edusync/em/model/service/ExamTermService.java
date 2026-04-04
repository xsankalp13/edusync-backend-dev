package com.project.edusync.em.model.service;

import com.project.edusync.common.exception.BadRequestException;
import com.project.edusync.em.model.dto.request.ExamTermRequestDTO;
import com.project.edusync.em.model.dto.response.ExamTermResponseDTO;
import com.project.edusync.em.model.entity.ExamTerm;
import com.project.edusync.em.model.repository.ExamTermRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamTermService {
    private final ExamTermRepository examTermRepository;

    @Transactional
    public ExamTermResponseDTO createExamTerm(ExamTermRequestDTO dto) {
        validateExamTerm(dto, null);
        ExamTerm term = new ExamTerm();
        term.setName(dto.getName());
        term.setStartDate(dto.getStartDate());
        term.setEndDate(dto.getEndDate());
        ExamTerm saved = examTermRepository.save(term);
        return toResponse(saved);
    }

    @Transactional
    public ExamTermResponseDTO updateExamTerm(Long id, ExamTermRequestDTO dto) {
        ExamTerm existing = examTermRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("ExamTerm not found"));
        validateExamTerm(dto, id);
        existing.setName(dto.getName());
        existing.setStartDate(dto.getStartDate());
        existing.setEndDate(dto.getEndDate());
        ExamTerm saved = examTermRepository.save(existing);
        return toResponse(saved);
    }

    public List<ExamTermResponseDTO> getAllExamTerms() {
        return examTermRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    private void validateExamTerm(ExamTermRequestDTO dto, Long excludeId) {
        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            throw new BadRequestException("Start date and end date are required");
        }
        if (!dto.getStartDate().isBefore(dto.getEndDate())) {
            throw new BadRequestException("Start date must be before end date");
        }
        List<ExamTerm> all = examTermRepository.findAll();
        for (ExamTerm term : all) {
            if (excludeId != null && term.getId().equals(excludeId)) continue;
            if (datesOverlap(dto.getStartDate(), dto.getEndDate(), term.getStartDate(), term.getEndDate())) {
                throw new BadRequestException("Exam term date range overlaps with existing term: " + term.getName());
            }
        }
        if (examTermRepository.existsByName(dto.getName())) {
            if (excludeId == null || all.stream().noneMatch(t -> t.getId().equals(excludeId) && t.getName().equals(dto.getName()))) {
                throw new BadRequestException("Exam term name must be unique");
            }
        }
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !start1.isAfter(end2) && !end1.isBefore(start2);
    }

    private ExamTermResponseDTO toResponse(ExamTerm term) {
        ExamTermResponseDTO dto = new ExamTermResponseDTO();
        dto.setId(term.getId());
        dto.setName(term.getName());
        dto.setStartDate(term.getStartDate());
        dto.setEndDate(term.getEndDate());
        return dto;
    }
}

