package com.project.edusync.em.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.edusync.em.model.enums.ExamAttendanceStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExamRoomStudentResponseDTO {
    Long studentId;
    String studentName;
    Integer rollNo;
    String className;
    String subjectName;
    String seatNumber;
    ExamAttendanceStatus attendanceStatus;
    boolean malpractice;
    boolean finalized;
    boolean entryAllowed;

    // Backward-compatible aliases for existing clients.
    @JsonProperty("name")
    public String getName() {
        return studentName;
    }

    @JsonProperty("seatLabel")
    public String getSeatLabel() {
        return seatNumber;
    }

    @JsonProperty("status")
    public ExamAttendanceStatus getStatus() {
        return attendanceStatus;
    }

    @JsonProperty("malpracticeReported")
    public boolean isMalpracticeReported() {
        return malpractice;
    }
}

