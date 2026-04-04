package com.project.edusync.em.model.service.serviceImpl;

import com.project.edusync.common.exception.emException.EdusyncException;
import com.project.edusync.em.model.dto.RequestDTO.PaperQuestionMapRequestDTO;
import com.project.edusync.em.model.dto.RequestDTO.QuestionPaperRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.PaperQuestionMapResponseDTO;
import com.project.edusync.em.model.dto.ResponseDTO.QuestionPaperResponseDTO;
import com.project.edusync.em.model.entity.ExamSchedule;
import com.project.edusync.em.model.entity.PaperQuestionMap;
import com.project.edusync.em.model.entity.QuestionBank;
import com.project.edusync.em.model.entity.QuestionPaper;
import com.project.edusync.em.model.repository.ExamScheduleRepository;
import com.project.edusync.em.model.repository.QuestionBankRepository;
import com.project.edusync.em.model.repository.QuestionPaperRepository;
import com.project.edusync.em.model.service.QuestionPaperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class QuestionPaperServiceImpl implements QuestionPaperService {

    private final QuestionPaperRepository questionPaperRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final QuestionBankRepository questionBankRepository;

    @Override
    public QuestionPaperResponseDTO generateQuestionPaper(QuestionPaperRequestDTO requestDTO) {
        log.info("Generating question paper for schedule ID: {}", requestDTO.getScheduleId());

        ExamSchedule schedule = examScheduleRepository.findById(requestDTO.getScheduleId())
                .orElseThrow(() -> new EdusyncException("EM-404", "Exam Schedule not found", HttpStatus.NOT_FOUND));

        // 1. Check if a paper already exists for this schedule
        if (questionPaperRepository.findByExamSchedule_Id(schedule.getId()).isPresent()) {
            throw new EdusyncException("EM-409", "A question paper already exists for this schedule", HttpStatus.CONFLICT);
        }

        // 2. Validate total marks against schedule
        if (requestDTO.getTotalMarks().compareTo(BigDecimal.valueOf(schedule.getMaxMarks())) != 0) {
            throw new EdusyncException("EM-400",
                    "Paper total marks (" + requestDTO.getTotalMarks() + ") must match schedule max marks (" + schedule.getMaxMarks() + ")",
                    HttpStatus.BAD_REQUEST);
        }

        // 3. Validate sum of individual question marks
        BigDecimal calculatedTotal = requestDTO.getQuestionMappings().stream()
                .map(PaperQuestionMapRequestDTO::getMarksForQuestion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (calculatedTotal.compareTo(requestDTO.getTotalMarks()) != 0) {
            throw new EdusyncException("EM-400",
                    "Sum of question marks (" + calculatedTotal + ") does not match paper total (" + requestDTO.getTotalMarks() + ")",
                    HttpStatus.BAD_REQUEST);
        }

        QuestionPaper paper = new QuestionPaper();
        paper.setExamSchedule(schedule);
        paper.setPaperName(requestDTO.getPaperName());
        paper.setTotalMarks(requestDTO.getTotalMarks());
        paper.setDurationMinutes(requestDTO.getDurationMinutes());
        paper.setInstructions(requestDTO.getInstructions());

        // 4. Create mappings
        Set<PaperQuestionMap> mappings = new HashSet<>();
        for (PaperQuestionMapRequestDTO mapDTO : requestDTO.getQuestionMappings()) {
            QuestionBank question = questionBankRepository.findByUuid(mapDTO.getQuestionUuid())
                    .orElseThrow(() -> new EdusyncException("EM-404", "Question not found with UUID: " + mapDTO.getQuestionUuid(), HttpStatus.NOT_FOUND));

            PaperQuestionMap mapping = new PaperQuestionMap();
            mapping.setQuestionPaper(paper);
            mapping.setQuestion(question);
            mapping.setQuestionNumber(mapDTO.getQuestionNumber());
            mapping.setMarksForQuestion(mapDTO.getMarksForQuestion());
            mappings.add(mapping);
        }
        paper.setQuestionMappings(mappings);

        QuestionPaper savedPaper = questionPaperRepository.save(paper);
        log.info("Question paper generated successfully with UUID: {}", savedPaper.getUuid());

        return toResponseDTO(savedPaper);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionPaperResponseDTO getQuestionPaperByUuid(UUID uuid) {
        log.info("Fetching question paper UUID: {}", uuid);
        QuestionPaper paper = questionPaperRepository.findByUuid(uuid)
                .orElseThrow(() -> new EdusyncException("EM-404", "Question paper not found", HttpStatus.NOT_FOUND));
        return toResponseDTO(paper);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionPaperResponseDTO getQuestionPaperByScheduleId(Long scheduleId) {
        log.info("Fetching question paper for schedule ID: {}", scheduleId);
        QuestionPaper paper = questionPaperRepository.findByExamSchedule_Id(scheduleId)
                .orElseThrow(() -> new EdusyncException("EM-404", "No question paper found for this schedule", HttpStatus.NOT_FOUND));
        return toResponseDTO(paper);
    }

    @Override
    public void deleteQuestionPaper(UUID uuid) {
        log.info("Deleting question paper UUID: {}", uuid);
        QuestionPaper paper = questionPaperRepository.findByUuid(uuid)
                .orElseThrow(() -> new EdusyncException("EM-404", "Question paper not found", HttpStatus.NOT_FOUND));
        questionPaperRepository.delete(paper);
    }

    // --- Helper Methods ---

    private QuestionPaperResponseDTO toResponseDTO(QuestionPaper entity) {
        return QuestionPaperResponseDTO.builder()
                .uuid(entity.getUuid())
                .scheduleId(entity.getExamSchedule().getId())
                .paperName(entity.getPaperName())
                .totalMarks(entity.getTotalMarks())
                .durationMinutes(entity.getDurationMinutes())
                .instructions(entity.getInstructions())
                // Mapped audit fields
                .generatedAt(entity.getCreatedAt())
                .generatedBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .questionMappings(entity.getQuestionMappings().stream()
                        .map(this::toMapResponseDTO)
                        .collect(Collectors.toSet()))
                .build();
    }

    private PaperQuestionMapResponseDTO toMapResponseDTO(PaperQuestionMap entity) {
        return PaperQuestionMapResponseDTO.builder()
                .paperQuestionId(entity.getPaperQuestionId())
                .questionNumber(entity.getQuestionNumber())
                .marksForQuestion(entity.getMarksForQuestion())
                .questionUuid(entity.getQuestion().getUuid())
                .questionText(entity.getQuestion().getQuestionText())
                .build();
    }
}