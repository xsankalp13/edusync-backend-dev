package com.project.edusync.adm.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for responding with Timeslot information.
 */
@Data
@Builder
public class TimeslotResponseDto {

    private UUID uuid;
    private Short dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String slotLabel;
    private Boolean isBreak;

}