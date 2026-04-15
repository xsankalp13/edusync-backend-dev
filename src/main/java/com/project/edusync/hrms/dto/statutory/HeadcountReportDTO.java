package com.project.edusync.hrms.dto.statutory;

import java.time.LocalDate;
import java.util.List;

public record HeadcountReportDTO(LocalDate asOf, int totalActive, int totalTeaching,
                                   int totalNonTeachingAdmin, int totalNonTeachingSupport,
                                   List<HeadcountRow> rows) {
    public record HeadcountRow(String employeeId, String staffName, String category,
                                String designation, LocalDate hireDate) {}
}

