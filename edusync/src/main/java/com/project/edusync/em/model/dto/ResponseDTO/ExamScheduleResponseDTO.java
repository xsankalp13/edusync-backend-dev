package com.project.edusync.em.model.dto.ResponseDTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for sending ExamSchedule data to the client.
 * This entity is not auditable, so it returns its Long PK.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamScheduleResponseDTO {

    private Long scheduleId;
    private UUID examUuid; // The public UUID of the parent Exam
    private Long classId;
    private Long sectionId;
    private Long subjectId;
    private LocalDate examDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal maxMarks;
    private BigDecimal passingMarks;
    private String roomNumber;

}
