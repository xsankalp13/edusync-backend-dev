package com.project.edusync.hrms.dto.leave;

import com.project.edusync.hrms.model.enums.HalfDayType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record LeaveApplicationCreateDTO(
        @NotBlank String leaveTypeRef,
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate,
        Boolean isHalfDay,
        HalfDayType halfDayType,
        @NotBlank @Size(max = 500) String reason,
        @Size(max = 1024) String attachmentUrl
) {
}

