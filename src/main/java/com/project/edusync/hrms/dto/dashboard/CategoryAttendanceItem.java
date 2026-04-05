package com.project.edusync.hrms.dto.dashboard;

import com.project.edusync.uis.model.enums.StaffCategory;

public record CategoryAttendanceItem(
        StaffCategory category,
        int present,
        int absent,
        int onLeave
) {
}

