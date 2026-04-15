package com.project.edusync.hrms.dto.statutory;

import java.math.BigDecimal;
import java.util.List;

public record TdsReportDTO(int month, int year, List<TdsReportRow> rows) {
    public record TdsReportRow(String employeeId, String staffName, BigDecimal grossSalary, BigDecimal tdsDeducted) {}
}

