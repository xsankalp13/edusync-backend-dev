package com.project.edusync.hrms.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffAttendanceSummaryDTO {
    private String periodLabel;
    private int totalDays;
    private int presentDays;
    private int absentDays;
    private int leaveDays;
    private int holidays;
    private BigDecimal attendancePercentage;
    private List<DayRecord> dailyRecords;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayRecord {
        private LocalDate date;
        private String status;
    }
}

