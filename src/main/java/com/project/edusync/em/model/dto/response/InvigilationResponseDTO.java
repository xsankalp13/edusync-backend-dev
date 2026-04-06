package com.project.edusync.em.model.dto.response;

import com.project.edusync.em.model.enums.InvigilationRole;

public class InvigilationResponseDTO {
    private Long id;
    private String staffName;
    private InvigilationRole role;
    private Long examScheduleId;
    private String roomUuid;
    private String roomName;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
    public InvigilationRole getRole() { return role; }
    public void setRole(InvigilationRole role) { this.role = role; }
    public Long getExamScheduleId() { return examScheduleId; }
    public void setExamScheduleId(Long examScheduleId) { this.examScheduleId = examScheduleId; }
    public String getRoomUuid() { return roomUuid; }
    public void setRoomUuid(String roomUuid) { this.roomUuid = roomUuid; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
}

