package com.project.edusync.em.model.dto.ResponseDTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScheduleAdmitCardStatusDTO {
    private Long scheduleId;
    private String className;
    private String sectionName;
    private String subjectName;
    private String examDate;
    private int totalStudents;
    private int generatedCount;
    private boolean allGenerated;
    private int publishedCount;
    private boolean allPublished;
    private LocalDateTime lastGeneratedAt;
}
