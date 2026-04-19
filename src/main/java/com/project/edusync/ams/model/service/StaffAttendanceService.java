package com.project.edusync.ams.model.service;

import com.project.edusync.ams.model.dto.request.StaffAttendanceRequestDTO;
import com.project.edusync.ams.model.dto.response.AttendanceCompletionDTO;
import com.project.edusync.ams.model.dto.response.StaffDailyStatsResponseDTO;
import com.project.edusync.ams.model.dto.response.StaffAttendanceResponseDTO;
import com.project.edusync.uis.model.dto.admin.StaffSummaryDTO;
import com.project.edusync.uis.model.enums.StaffCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffAttendanceService {

    StaffAttendanceResponseDTO createAttendance(StaffAttendanceRequestDTO request, Long performedBy);

    List<StaffAttendanceResponseDTO> bulkCreate(List<StaffAttendanceRequestDTO> requests, Long performedBy);

    Page<StaffAttendanceResponseDTO> listAttendances(Pageable pageable,
                                                     Optional<UUID> staffUuid,
                                                     Optional<LocalDate> date,
                                                     Optional<LocalDate> fromDate,
                                                     Optional<LocalDate> toDate,
                                                     Optional<String> status,
                                                     Optional<String> search);

    StaffDailyStatsResponseDTO getDailyStats(Optional<LocalDate> date);

    StaffAttendanceResponseDTO getAttendance(UUID recordUuid);

    StaffAttendanceResponseDTO updateAttendance(UUID recordUuid, StaffAttendanceRequestDTO request, Long performedBy);

    void deleteAttendance(UUID recordUuid, Long performedBy);

    AttendanceCompletionDTO getAttendanceCompletion(int month, int year);

    List<StaffSummaryDTO> getUnmarkedStaff(LocalDate date, Optional<StaffCategory> category);

    /**
     * Mark all scheduled (and not-yet-marked) staff as Present for a given date.
     * Skips staff on approved leave.
     * @param date       the target date
     * @param testMode   if true, allows future dates (for testing purposes)
     * @return number of records created
     */
    int markAllPresent(LocalDate date, boolean testMode);

    /**
     * Mark all scheduled (and not-yet-marked) staff as Absent for a given date.
     * Skips staff on approved leave.
     * @param date       the target date
     * @param testMode   if true, allows future dates (for testing purposes)
     * @return number of records created
     */
    int markAllAbsent(LocalDate date, boolean testMode);
}
