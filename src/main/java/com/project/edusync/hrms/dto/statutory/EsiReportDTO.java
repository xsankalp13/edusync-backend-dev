package com.project.edusync.hrms.dto.statutory;

import java.math.BigDecimal;
import java.util.List;

public record EsiReportDTO(int month, int year, List<EsiReportRow> rows) {
    public record EsiReportRow(String employeeId, String staffName, BigDecimal grossWages,
                                BigDecimal employeeContribution, BigDecimal employerContribution) {}
}

