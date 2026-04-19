package com.project.edusync.hrms.dto.statutory;

import java.math.BigDecimal;
import java.util.List;

public record Form16DataDTO(String financialYear, List<Form16Row> rows) {
    public record Form16Row(String employeeId, String staffName, BigDecimal totalGross,
                             BigDecimal totalTdsDeducted, BigDecimal standardDeduction,
                             BigDecimal taxableIncome) {}
}

