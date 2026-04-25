package com.project.edusync.em.model.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ExamControllerClassViewResponseDTO {
    Long examId;
    List<ClassNode> classes;

    @Value
    @Builder
    public static class ClassNode {
        String className;
        List<StudentNode> students;
    }

    @Value
    @Builder
    public static class StudentNode {
        Long studentId;
        String studentName;
        Integer rollNo;
        List<RoomAssignmentNode> rooms;
    }

    @Value
    @Builder
    public static class RoomAssignmentNode {
        Long roomId;
        String roomName;
        Long examScheduleId;
        String subjectName;
        String seatNumber;
        String attendanceStatus;
        boolean entryAllowed;
    }
}

