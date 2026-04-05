package com.project.edusync.hrms.dto.calendar;

import java.util.List;

public record CalendarSummaryDTO(
        String academicYear,
        int totalWorkingDays,
        int totalHolidays,
        int totalHalfDays,
        List<MonthSummaryDTO> months
) {
}

