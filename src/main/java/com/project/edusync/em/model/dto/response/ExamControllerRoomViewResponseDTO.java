package com.project.edusync.em.model.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ExamControllerRoomViewResponseDTO {
    Long examId;
    List<RoomNode> rooms;

    @Value
    @Builder
    public static class RoomNode {
        Long roomId;
        String roomName;
        List<StudentNode> students;
    }

    @Value
    @Builder
    public static class StudentNode {
        Long studentId;
        String studentName;
        Integer rollNo;
        String className;
        Long examScheduleId;
        String subjectName;
        String seatNumber;
        String attendanceStatus;
        boolean entryAllowed;
    }
}

