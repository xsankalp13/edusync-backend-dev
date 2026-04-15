package com.project.edusync.hrms.dto.statutory;

import java.math.BigDecimal;
import java.util.List;

public record PtReportDTO(int month, int year, String state, List<PtReportRow> rows) {
    public record PtReportRow(String employeeId, String staffName, BigDecimal grossSalary, BigDecimal ptDeducted) {}
}

