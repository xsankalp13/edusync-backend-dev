package com.project.edusync.adm.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class CurriculumClassSubjectResponseDto {
    private UUID curriculumMapId;
    private UUID classId;
    private UUID subjectId;
    private String subjectName;
    private String subjectCode;
    private String color;
    private Short periodsPerWeek;
    private Long totalScheduledPeriods;
}

