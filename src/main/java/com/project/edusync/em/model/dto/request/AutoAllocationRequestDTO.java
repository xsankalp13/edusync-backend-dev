package com.project.edusync.em.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class AutoAllocationRequestDTO {
    @NotNull
    private Long examScheduleId;
    @NotNull
    private UUID roomId;
    private String seatPrefix; // e.g. "Row-A-"
    private Integer startNumber; // e.g. 1
}
