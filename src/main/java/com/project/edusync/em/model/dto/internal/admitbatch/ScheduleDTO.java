package com.project.edusync.em.model.dto.internal.admitbatch;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalTime;

@Value
@Builder
public class ScheduleDTO {
    String subject;
    LocalDate date;
    LocalTime startTime;
    LocalTime endTime;
    String seat;
    String room;
}

