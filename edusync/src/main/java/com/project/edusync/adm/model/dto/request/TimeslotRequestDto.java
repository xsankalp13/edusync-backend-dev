package com.project.edusync.adm.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalTime;

/**
 * DTO for creating or updating a Timeslot.
 */
@Data
public class TimeslotRequestDto {

    @NotNull(message = "Day of week cannot be null")
    @Min(value = 1, message = "Day of week must be between 1 (Monday) and 7 (Sunday)")
    @Max(value = 7, message = "Day of week must be between 1 (Monday) and 7 (Sunday)")
    private Short dayOfWeek;

    @NotNull(message = "Start time cannot be null")
    private LocalTime startTime;

    @NotNull(message = "End time cannot be null")
    private LocalTime endTime;

    @NotBlank(message = "Slot label cannot be blank")
    private String slotLabel; // e.g., "Period 1", "Lunch Break"

    @NotNull(message = "isBreak flag cannot be null")
    private Boolean isBreak = false;
}