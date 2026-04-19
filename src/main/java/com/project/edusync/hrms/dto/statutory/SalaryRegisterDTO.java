package com.project.edusync.hrms.dto.statutory;

import java.math.BigDecimal;
import java.util.List;

public record SalaryRegisterDTO(int month, int year, List<SalaryRegisterRow> rows) {
    public record SalaryRegisterRow(String employeeId, String staffName, String designation,
                                     BigDecimal grossPay, BigDecimal totalDeductions, BigDecimal netPay) {}
}

