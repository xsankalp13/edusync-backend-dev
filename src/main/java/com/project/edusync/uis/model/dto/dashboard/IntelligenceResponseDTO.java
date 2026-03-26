package com.project.edusync.uis.model.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Student dashboard intelligence payload.
 *
 * This DTO is intentionally nested and strictly typed so the API contract
 * remains stable even when entity models evolve.
 */
public record IntelligenceResponseDTO(
        ProfileDTO profile,
        AcademicPulseDTO academicPulse,
        FinanceHealthDTO financeHealth,
        ActivityFeedDTO activityFeed
) {

    public record ProfileDTO(
            Long studentId,
            Long userId,
            String fullName,
            String enrollmentNumber,
            String sectionName,
            String className
    ) {
    }

    public record AcademicPulseDTO(
            LiveAcademicContextDTO liveAcademicContext,
            PredictiveAttendanceDTO predictiveAttendance
    ) {
    }

    public record LiveAcademicContextDTO(
            String currentClass,
            String nextClass
    ) {
    }

    public record PredictiveAttendanceDTO(
            long totalClasses,
            long attendedClasses,
            BigDecimal percentage,
            AttendanceStatus status,
            int thresholdPercentage
    ) {
    }

    public enum AttendanceStatus {
        HEALTHY,
        WARNING,
        CRITICAL
    }

    public record FinanceHealthDTO(
            BigDecimal totalDue,
            LocalDate earliestDueDate,
            boolean temporarilyUnavailable,
            String message
    ) {
    }

    public record ActivityFeedDTO(
            java.util.List<RecentActivityDTO> recentActivities
    ) {
    }

    public record RecentActivityDTO(
            Long activityId,
            String actionType,
            String fieldName,
            String oldValue,
            String newValue,
            LocalDateTime createdAt
    ) {
    }
}

