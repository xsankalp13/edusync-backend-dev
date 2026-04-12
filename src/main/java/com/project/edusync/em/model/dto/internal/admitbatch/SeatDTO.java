package com.project.edusync.em.model.dto.internal.admitbatch;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SeatDTO {
    Long studentId;
    String seat;
    String room;
}

