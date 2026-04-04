package com.project.edusync.em.model.dto.request;

import com.project.edusync.em.model.enums.InvigilationRole;
import jakarta.validation.constraints.NotNull;

public class InvigilationRequestDTO {
    @NotNull
    private Long examScheduleId;
    @NotNull
    private Long staffId;
    @NotNull
    private InvigilationRole role;
    // getters and setters
    public Long getExamScheduleId() { return examScheduleId; }
    public void setExamScheduleId(Long examScheduleId) { this.examScheduleId = examScheduleId; }
    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public InvigilationRole getRole() { return role; }
    public void setRole(InvigilationRole role) { this.role = role; }
}

