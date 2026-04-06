package com.project.edusync.hrms.dto.leave;

import com.project.edusync.hrms.model.enums.HalfDayType;
import com.project.edusync.hrms.model.enums.LeaveApplicationStatus;
import com.project.edusync.uis.model.enums.StaffCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveApplicationResponseDTO(
        Long applicationId,
        String uuid,
        Long staffId,
        String staffName,
        String employeeId,
        StaffCategory staffCategory,
        String designationName,
        Long leaveTypeId,
        String leaveTypeCode,
        String leaveTypeName,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal totalDays,
        boolean isHalfDay,
        HalfDayType halfDayType,
        String reason,
        String attachmentUrl,
        LeaveApplicationStatus status,
        LocalDateTime appliedOn,
        Long reviewedByUserId,
        String reviewedByName,
        String reviewRemarks,
        LocalDateTime reviewedAt
) {
}

