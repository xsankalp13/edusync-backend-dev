package com.project.edusync.hrms.dto.calendar;

import com.project.edusync.hrms.model.enums.DayType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CalendarEventCreateDTO(
        @NotBlank String academicYear,
        @NotNull LocalDate date,
        @NotNull DayType dayType,
        @Size(max = 150) String title,
        @Size(max = 500) String description,
        Boolean appliesToStaff,
        Boolean appliesToStudents
) {
}

