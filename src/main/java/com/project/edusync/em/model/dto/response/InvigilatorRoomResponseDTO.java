package com.project.edusync.em.model.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalTime;

@Value
@Builder
public class InvigilatorRoomResponseDTO {
    Long examScheduleId;
    Long roomId;
    String roomName;
    String subjectName;
    String className;
    LocalDate examDate;
    LocalTime startTime;
    LocalTime endTime;
}

