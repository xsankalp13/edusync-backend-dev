package com.project.edusync.hrms.dto.calendar;

import com.project.edusync.hrms.model.enums.DayType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CalendarEventResponseDTO(
        Long eventId,
        String uuid,
        String academicYear,
        LocalDate date,
        DayType dayType,
        String title,
        String description,
        boolean appliesToStaff,
        boolean appliesToStudents,
        LocalDateTime createdAt
) {
}

