package com.project.edusync.uis.model.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StaffKpiMetricsDTO {
    private Long staffId;
    private BigDecimal performanceRating;
    private Long totalClassesAssigned;
    private Integer weeklyHoursAssigned;
    private BigDecimal attendanceRatePercentage;
}

