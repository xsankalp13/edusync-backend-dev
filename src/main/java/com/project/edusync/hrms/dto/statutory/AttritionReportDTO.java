package com.project.edusync.hrms.dto.statutory;

import java.time.LocalDate;
import java.util.List;

public record AttritionReportDTO(LocalDate fromDate, LocalDate toDate,
                                   int hired, int separated, double attritionRate,
                                   List<AttritionRow> separations) {
    public record AttritionRow(String employeeId, String staffName, LocalDate separationDate, String reason) {}
}

