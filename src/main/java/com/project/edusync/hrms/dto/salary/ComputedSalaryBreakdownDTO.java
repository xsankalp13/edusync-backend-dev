package com.project.edusync.hrms.dto.salary;

import java.math.BigDecimal;
import java.util.List;

public record ComputedSalaryBreakdownDTO(
        Long staffId,
        String staffName,
        String employeeId,
        String templateName,
        String gradeCode,
        List<ComputedComponentDTO> earnings,
        List<ComputedComponentDTO> deductions,
        BigDecimal grossPay,
        BigDecimal totalDeductions,
        BigDecimal netPay,
        BigDecimal ctc,
        boolean hasOverrides
) {
}

