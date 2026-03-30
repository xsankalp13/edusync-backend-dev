package com.project.edusync.uis.model.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentKpiMetricsDTO {
    private Long studentId;
    private String academicStanding;
    private BigDecimal gpa;
    private String currentGrade;
    private String currentSection;
    private BigDecimal attendanceRatePercentage;
    private Long openDisciplinaryIncidents;
}

