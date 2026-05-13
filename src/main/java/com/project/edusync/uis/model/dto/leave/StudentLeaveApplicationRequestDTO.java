package com.project.edusync.uis.model.dto.leave;

import com.project.edusync.uis.model.enums.StudentLeaveType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentLeaveApplicationRequestDTO {
    @NotNull
    private StudentLeaveType leaveType;

    @NotNull
    private LocalDate fromDate;

    @NotNull
    private LocalDate toDate;

    @NotBlank
    private String reason;

    private boolean halfDay;
}

