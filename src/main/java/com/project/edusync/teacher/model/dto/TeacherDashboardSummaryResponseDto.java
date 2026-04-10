package com.project.edusync.teacher.model.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDashboardSummaryResponseDto {
    private LocalDate date;
    private long totalStudents;
    private long classesToday;
    private TeacherHomeroomResponseDto.TodayAttendance attendance;
    private Alerts alerts;
    private NextClass nextClass;
    private boolean isOnLeaveToday;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alerts {
        private long atRiskStudentCount;
        private long pendingLeaveRequests;
        private long belowThresholdCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NextClass {
        private String subject;
        private String className;
        private String sectionName;
        private String room;
        private LocalTime startTime;
        private LocalTime endTime;
    }
}



