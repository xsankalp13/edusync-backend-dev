package com.project.edusync.hrms.dto.statutory;

import java.math.BigDecimal;
import java.util.List;

public record PfReportDTO(int month, int year, List<PfReportRow> rows) {
    public record PfReportRow(String employeeId, String staffName, BigDecimal grossSalary,
                               BigDecimal pfWages, BigDecimal employeeContribution,
                               BigDecimal employerContribution) {}
}

