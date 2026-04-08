package com.project.edusync.adm.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class EditorContextResponseDto {
    private SectionSummaryDto section;
    private List<TimeslotItemDto> timeslots;
    private List<AvailableSubjectDto> availableSubjects;
    private List<TeacherItemDto> teachers;
    private List<ExistingScheduleItemDto> existingSchedule;

    @Data
    @Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SectionSummaryDto {
        private UUID uuid;
        private String sectionName;
        private String className;
        /** Full name of the class teacher, or null if not assigned. */
        private String classTeacherName;
        /** TeacherDetails ID of the class teacher, used to match against teacher list. */
        private String classTeacherId;
        /** Staff UUID of the class teacher, used to pre-populate the section settings picker. */
        private UUID classTeacherStaffUuid;
    }

    @Data
    @Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TimeslotItemDto {
        private UUID uuid;
        private Short dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;
        private String slotLabel;
        private Boolean isBreak;
    }

    @Data
    @Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TeacherItemDto {
        private String id;
        private String name;
        private List<UUID> teachableSubjectIds;
    }

    @Data
    @Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ExistingScheduleItemDto {
        private UUID uuid;
        private UUID subjectId;
        private String teacherId;
        private UUID roomId;
        private UUID timeslotId;
        private String slotLabel;
    }
}

