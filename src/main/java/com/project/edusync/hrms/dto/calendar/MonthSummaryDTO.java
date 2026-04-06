package com.project.edusync.hrms.dto.calendar;

public record MonthSummaryDTO(
        int month,
        String monthName,
        int workingDays,
        int holidays,
        int halfDays
) {
}

