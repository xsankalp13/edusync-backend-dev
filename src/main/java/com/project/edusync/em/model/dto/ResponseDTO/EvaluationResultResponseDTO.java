package com.project.edusync.em.model.dto.ResponseDTO;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class EvaluationResultResponseDTO {
    private Long resultId;
    private Long answerSheetId;
    private BigDecimal totalMarks;
    private String status;
    private LocalDateTime evaluatedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime publishedAt;
    private String approvedBy;
    private Map<String, BigDecimal> sectionTotals;
    private Map<String, List<String>> selectedQuestions;
}

