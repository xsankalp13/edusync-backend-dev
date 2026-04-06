package com.project.edusync.em.model.dto.request;

import com.project.edusync.em.model.enums.InvigilationRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class InvigilationRequestDTO {
    @NotNull
    private Long examScheduleId;
    @NotNull
    private UUID staffId;
    @NotNull
    private InvigilationRole role;
    @NotNull
    private UUID roomId;
    
    // getters and setters
    public Long getExamScheduleId() { return examScheduleId; }
    public void setExamScheduleId(Long examScheduleId) { this.examScheduleId = examScheduleId; }
    public UUID getStaffId() { return staffId; }
    public void setStaffId(UUID staffId) { this.staffId = staffId; }
    public InvigilationRole getRole() { return role; }
    public void setRole(InvigilationRole role) { this.role = role; }
    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID roomId) { this.roomId = roomId; }
}

