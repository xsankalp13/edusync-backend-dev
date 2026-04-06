package com.project.edusync.teacher.model.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherHomeroomResponseDto {
    private boolean isClassTeacher;
    private UUID classUuid;
    private String className;
    private UUID sectionUuid;
    private String sectionName;
    private DefaultRoom defaultRoom;
    private long studentCount;
    private TodayAttendance todayAttendance;
    private List<AtRiskStudent> atRiskStudents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefaultRoom {
        private UUID uuid;
        private String roomName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayAttendance {
        private long present;
        private long absent;
        private long late;
        private long notMarked;
        private BigDecimal percentage;
        private boolean attendanceMarkedToday;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AtRiskStudent {
        private UUID studentUuid;
        private String name;
        private BigDecimal attendancePercentage;
        private long consecutiveAbsences;
    }
}


