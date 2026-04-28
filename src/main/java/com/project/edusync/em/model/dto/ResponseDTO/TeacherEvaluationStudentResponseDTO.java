package com.project.edusync.em.model.dto.ResponseDTO;

import com.project.edusync.em.model.enums.AnswerSheetStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherEvaluationStudentResponseDTO {
    private UUID studentId;
    private String studentName;
    private String enrollmentNumber;
    private Long answerSheetId;
    private AnswerSheetStatus answerSheetStatus;
    private boolean malpracticeReported;
}

