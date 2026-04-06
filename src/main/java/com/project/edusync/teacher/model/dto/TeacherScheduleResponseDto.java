package com.project.edusync.teacher.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TeacherScheduleResponseDto {
    private String teacherName;
    private UUID staffUuid;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private List<Entry> entries;

    @Data
    @Builder
    public static class Entry {
        private UUID scheduleEntryUuid;
        private String dayOfWeek;
        private SlotType slotType;
        private TimeslotItem timeslot;
        private SubjectItem subject;
        private ClassItem clazz;
        private SectionItem section;
        private RoomItem room;
    }

    public enum SlotType {
        TEACHING,
        LEISURE,
        BREAK,
        MEETING,
        EVENT
    }

    @Data
    @Builder
    public static class TimeslotItem {
        private UUID uuid;
        private LocalTime startTime;
        private LocalTime endTime;
        private String slotLabel;
        private boolean isBreak;
    }

    @Data
    @Builder
    public static class SubjectItem {
        private UUID uuid;
        private String subjectName;
        private String subjectCode;
    }

    @Data
    @Builder
    public static class ClassItem {
        private UUID uuid;
        private String className;
    }

    @Data
    @Builder
    public static class SectionItem {
        private UUID uuid;
        private String sectionName;
    }

    @Data
    @Builder
    public static class RoomItem {
        private UUID uuid;
        private String roomName;
        private String roomType;
        private String floor;
    }
}

