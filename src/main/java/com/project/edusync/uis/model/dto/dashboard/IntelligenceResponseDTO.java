package com.project.edusync.uis.model.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Student dashboard intelligence payload.
 *
 * This DTO is intentionally nested and strictly typed so the API contract
 * remains stable even when entity models evolve.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public record IntelligenceResponseDTO(
        @Schema(description = "Student profile details")
        ProfileDTO profile,
        @Schema(description = "Live academic pulse and attendance signals")
        AcademicPulseDTO academicPulse,
        @Schema(description = "Financial health of unpaid invoices")
        FinanceHealthDTO financeHealth,
        @Schema(description = "Recent student activity feed")
        ActivityFeedDTO activityFeed
) {

    public record ProfileDTO(
            @Schema(description = "Internal student id", example = "125")
            Long studentId,
            @Schema(description = "Internal user id", example = "88")
            Long userId,
            @Schema(description = "Student display name", example = "Aarav Kumar")
            String fullName,
            @Schema(description = "Enrollment number", example = "ENR-2026-0045")
            String enrollmentNumber,
            @Schema(description = "Section name", example = "A")
            String sectionName,
            @Schema(description = "Class name", example = "Grade 10")
            String className
    ) {
    }

    public record AcademicPulseDTO(
            @Schema(description = "Live timetable context")
            LiveAcademicContextDTO liveAcademicContext,
            @Schema(description = "Attendance prediction and risk state")
            PredictiveAttendanceDTO predictiveAttendance
    ) {
    }

    public record LiveAcademicContextDTO(
            @Schema(description = "Currently running class label", example = "Mathematics | Room 302 | 09:00-09:45")
            String currentClass,
            @Schema(description = "Upcoming class label", example = "Science | Lab 2 | 10:00-10:45")
            String nextClass
    ) {
    }

    public record PredictiveAttendanceDTO(
            @Schema(description = "Total class records considered", example = "120")
            long totalClasses,
            @Schema(description = "Classes marked present", example = "98")
            long attendedClasses,
            @Schema(description = "Attendance percentage", example = "81.67")
            BigDecimal percentage,
            @Schema(description = "Risk status derived from threshold")
            AttendanceStatus status,
            @Schema(description = "Configured minimum attendance threshold", example = "75")
            int thresholdPercentage
    ) {
    }

    @Schema(description = "Attendance status category")
    public enum AttendanceStatus {
        HEALTHY,
        WARNING,
        CRITICAL
    }

    public record FinanceHealthDTO(
            @Schema(description = "Total unpaid amount", example = "4500.00")
            BigDecimal totalDue,
            @Schema(description = "Earliest due date among unpaid invoices", example = "2026-04-05")
            LocalDate earliestDueDate,
            @Schema(description = "True when finance data falls back due to circuit breaker")
            boolean temporarilyUnavailable,
            @Schema(description = "Fallback message when finance module is unavailable", example = "Finance data temporarily unavailable")
            String message
    ) {
    }

    public record ActivityFeedDTO(
            @Schema(description = "Latest activity items (max 5)")
            java.util.List<RecentActivityDTO> recentActivities
    ) {
    }

    public record RecentActivityDTO(
            @Schema(description = "Activity identifier", example = "9021")
            Long activityId,
            @Schema(description = "Action performed", example = "UPDATE")
            String actionType,
            @Schema(description = "Field affected", example = "attendance_type")
            String fieldName,
            @Schema(description = "Previous value", example = "A")
            String oldValue,
            @Schema(description = "New value", example = "P")
            String newValue,
            @Schema(description = "Activity timestamp", example = "2026-03-25T08:15:30")
            LocalDateTime createdAt
    ) {
    }
}


