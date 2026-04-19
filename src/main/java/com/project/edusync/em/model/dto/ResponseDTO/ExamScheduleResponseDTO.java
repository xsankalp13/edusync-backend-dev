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
 * Includes rich data (names) to reduce frontend API chatter.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamScheduleResponseDTO {

    private Long scheduleId;
    private UUID examUuid; // Public ID of the parent Exam
    private UUID templateId;
    private TemplateSnapshotResponseDTO templateSnapshot;

    // Rich data for UI display
    private UUID classId;
    private String className;
    private UUID sectionId;
    private String sectionName;
    private UUID subjectId;
    private String subjectName;

    private LocalDate examDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal maxMarks;
    private BigDecimal passingMarks;
    private String roomNumber;
    private Long totalStudents;
    private Integer maxStudentsPerSeat;
}