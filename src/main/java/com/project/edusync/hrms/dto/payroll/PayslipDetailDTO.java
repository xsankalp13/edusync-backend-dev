package com.project.edusync.hrms.dto.payroll;

import com.project.edusync.hrms.model.enums.PayrollRunStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PayslipDetailDTO(
        Long payslipId,
        String uuid,
        Long payrollRunId,
        Long staffId,
        String staffName,
        String employeeId,
        Integer payMonth,
        Integer payYear,
        Integer totalWorkingDays,
        Integer daysPresent,
        Integer daysAbsent,
        BigDecimal lopDays,
        BigDecimal grossPay,
        BigDecimal totalDeductions,
        BigDecimal netPay,
        PayrollRunStatus status,
        LocalDateTime generatedAt,
        List<PayslipLineItemDTO> lineItems
) {
}

