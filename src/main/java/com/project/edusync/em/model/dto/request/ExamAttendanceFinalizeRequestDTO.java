package com.project.edusync.em.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExamAttendanceFinalizeRequestDTO {

    @NotNull
    private Long examScheduleId;

    @NotNull
    private Long roomId;
}

