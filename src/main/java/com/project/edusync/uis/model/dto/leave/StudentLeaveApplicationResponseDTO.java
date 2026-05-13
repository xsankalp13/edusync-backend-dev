package com.project.edusync.uis.model.dto.leave;

import com.project.edusync.uis.model.enums.StudentLeaveStatus;
import com.project.edusync.uis.model.enums.StudentLeaveType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class StudentLeaveApplicationResponseDTO {
    private Long id;
    private Long studentId;
    private Long appliedByGuardianId;
    private StudentLeaveType leaveType;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String reason;
    private StudentLeaveStatus status;
    private boolean halfDay;
}

