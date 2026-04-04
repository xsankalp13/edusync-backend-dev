package com.project.edusync.em.model.dto.response;

public class SittingPlanResponseDTO {
    private Long id;
    private String studentName;
    private String roomName;
    private String seatNumber;
    private Long examScheduleId;
    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    public Long getExamScheduleId() { return examScheduleId; }
    public void setExamScheduleId(Long examScheduleId) { this.examScheduleId = examScheduleId; }
}

