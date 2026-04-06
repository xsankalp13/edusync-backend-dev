package com.project.edusync.hrms.dto.dashboard;

public record GradeDistributionItem(
        String gradeCode,
        String gradeName,
        int count
) {
}

