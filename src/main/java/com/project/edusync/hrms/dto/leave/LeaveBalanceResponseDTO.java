package com.project.edusync.hrms.dto.leave;

import java.math.BigDecimal;

public record LeaveBalanceResponseDTO(
        Long balanceId,
        Long staffId,
        String staffName,
        Long leaveTypeId,
        String leaveTypeCode,
        String leaveTypeName,
        String academicYear,
        BigDecimal totalQuota,
        BigDecimal used,
        BigDecimal carriedForward,
        BigDecimal remaining
) {
}

