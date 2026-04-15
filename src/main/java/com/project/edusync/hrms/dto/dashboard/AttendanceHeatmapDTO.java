package com.project.edusync.hrms.dto.dashboard;

import java.time.LocalDate;
import java.util.List;

public record AttendanceHeatmapDTO(int year, int month, List<HeatmapDayEntry> days) {
    public record HeatmapDayEntry(LocalDate date, int presentCount, int absentCount, int onLeaveCount) {}
}

