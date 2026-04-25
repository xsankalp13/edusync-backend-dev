package com.project.edusync.em.model.dto.response;

import com.project.edusync.em.model.enums.RoomAttendanceProgressStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class ExamControllerDashboardResponseDTO {
    Long examId;
    List<RoomSummary> rooms;
    TimerInfo timer;

    @Value
    @Builder
    public static class RoomSummary {
        Long roomId;
        String roomName;
        long allocatedStudents;
        long markedStudents;
        RoomAttendanceProgressStatus attendanceStatus;
    }

    @Value
    @Builder
    public static class TimerInfo {
        LocalDateTime startTime;
        LocalDateTime endTime;
        long remainingSeconds;
    }
}

