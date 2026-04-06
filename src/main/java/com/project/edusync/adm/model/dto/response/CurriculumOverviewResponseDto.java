package com.project.edusync.adm.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class CurriculumOverviewResponseDto {
    private UUID classId;
    private String className;
    private Long totalSubjects;
    private Long totalPeriodsPerWeek;
    private Long scheduledPeriods;
    private BigDecimal coveragePercent;
}

