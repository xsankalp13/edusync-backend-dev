package com.project.edusync.em.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SittingPlanRequestDTO {
    @NotNull
    private Long examScheduleId;
    @NotNull
    private Long studentId;
    @NotNull
    private Long roomId;
    @NotBlank
    private String seatNumber;
    // getters and setters
    public Long getExamScheduleId() { return examScheduleId; }
    public void setExamScheduleId(Long examScheduleId) { this.examScheduleId = examScheduleId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
}

