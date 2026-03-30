package com.project.edusync.adm.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

/**
 * DTO for creating or updating a Schedule entry.
 */
@Data
public class ScheduleRequestDto {

    @NotNull(message = "Section ID cannot be null")
    private UUID sectionId;

    @NotNull(message = "Subject ID cannot be null")
    private UUID subjectId;

    @NotNull(message = "Teacher ID cannot be null")
    private Long teacherId;

    // @NotNull(message = "Room ID cannot be null")
    private UUID roomId;

    @NotNull(message = "Timeslot ID cannot be null")
    private UUID timeslotId;
}