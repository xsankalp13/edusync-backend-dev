package com.project.edusync.teacher.service;

import java.time.LocalDate;
import java.util.UUID;

public interface AttendanceExportService {
    byte[] exportDailyAttendanceSheet(Long currentUserId, UUID sectionUuid, LocalDate date);
}

