package com.project.edusync.adm.model.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for responding with a fully populated Schedule entry.
 * Contains nested objects for easy frontend rendering.
 */
@Data
@Builder
public class ScheduleResponseDto {

    private UUID uuid;
    private NestedSectionResponseDto section;
    private NestedSubjectResponseDto subject;
    private NestedTeacherResponseDto teacher;
    private NestedRoomResponseDto room;
    private NestedTimeslotResponseDto timeslot;

    // --- Nested DTOs for clean responses ---

    @Data
    @Builder
    public static class NestedSectionResponseDto {
        private UUID uuid;
        private String sectionName;
        private String className;
        private RoomBasicResponseDto defaultRoom;
    }

    @Data
    @Builder
    public static class NestedSubjectResponseDto {
        private UUID uuid;
        private String name;
        private String subjectCode;
    }

    @Data
    @Builder
    public static class NestedTeacherResponseDto {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String name; // Assuming TeacherDetails has a name
    }

    @Data
    @Builder
    public static class NestedRoomResponseDto {
        private UUID uuid;
        private String name;
        private String roomType;
        private Integer totalCapacity;
    }

    @Data
    @Builder
    public static class NestedTimeslotResponseDto {
        private UUID uuid;
        private Short dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;
        private String slotLabel;
    }
}