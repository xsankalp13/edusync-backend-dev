package com.project.edusync.hrms.dto.payroll;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record PayrollPreflightDTO(
        int month,
        int year,
        boolean canProcess,
        boolean alreadyProcessed,          // run already exists for this period
        List<PayrollBlockerDTO> blockers,
        List<PayrollWarningDTO> warnings,
        PayrollPreflightSummaryDTO summary
) {

    @Builder
    public record PayrollBlockerDTO(
            String type,
            String message,
            Map<String, Object> details
    ) {}

    @Builder
    public record PayrollWarningDTO(
            String type,
            String message,
            int count
    ) {}

    @Builder
    public record PayrollPreflightSummaryDTO(
            long totalStaff,
            long staffWithSalaryMapping,
            long staffWithoutSalaryMapping,
            long totalApprovedLeaves,
            long totalLopDays,
            double attendanceCompletionPercent
    ) {}
}

