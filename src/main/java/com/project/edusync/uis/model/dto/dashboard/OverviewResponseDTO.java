package com.project.edusync.uis.model.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Consolidated student dashboard overview payload")
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public record OverviewResponseDTO(
        ProfileDTO profile,
        KpisDTO kpis,
        List<ScheduleItemDTO> todaySchedule,
        List<PendingAssignmentDTO> pendingAssignments,
        List<PerformanceTrendDTO> performanceTrend,
        List<AnnouncementDTO> recentAnnouncements
) {

    public enum ScheduleStatus {
        COMPLETED,
        LIVE,
        UPCOMING
    }

    public enum AssignmentPriority {
        HIGH,
        MEDIUM,
        LOW
    }

    public enum AnnouncementType {
        EVENT,
        ACADEMIC,
        ALERT
    }

    public record ProfileDTO(
            Long studentId,
            String fullName,
            String enrollmentNumber,
            String courseOrClass,
            String profileUrl
    ) {}

    public record KpisDTO(
            BigDecimal attendancePercentage,
            BigDecimal currentCgpa,
            Integer pendingAssignmentsCount,
            BigDecimal totalOverdueFees
    ) {}

    public record ScheduleItemDTO(
            Long id,
            String subject,
            String teacher,
            String room,
            Instant startTime,
            Instant endTime,
            ScheduleStatus status
    ) {}

    public record PendingAssignmentDTO(
            Long id,
            String subject,
            String title,
            Instant dueDate,
            AssignmentPriority priority
    ) {}

    public record PerformanceTrendDTO(
            String term,
            BigDecimal score
    ) {}

    public record AnnouncementDTO(
            Long id,
            String title,
            Instant date,
            AnnouncementType type
    ) {}
}

