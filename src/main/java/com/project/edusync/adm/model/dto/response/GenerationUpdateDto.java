package com.project.edusync.adm.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Update sent via SSE during timetable generation.
 */
@Data
@Builder
public class GenerationUpdateDto {
    private int generation;
    private int fitness;
    private boolean isComplete;
    private List<ScheduleResponseDto> schedule;
    private String message;
}
