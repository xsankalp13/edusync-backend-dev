package com.project.edusync.ams.model.repository;

import com.project.edusync.ams.model.entity.StaffDailyAttendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffDailyAttendanceRepository extends JpaRepository<StaffDailyAttendance, Long> {

    /**
     * Finds the attendance record for a specific staff member on a specific date.
     * Enforces the unique constraint.
     */
    Optional<StaffDailyAttendance> findByStaffIdAndAttendanceDate(
            Long staffId,
            LocalDate attendanceDate);

    /**
     * Retrieves the attendance history for a specific staff member within a date range,
     * ordered descending by date.
     * Useful for payroll and HR reporting on staff presence.
     */
    List<StaffDailyAttendance> findByStaffIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
            Long staffId,
            LocalDate startDate,
            LocalDate endDate);

    /**
     * Retrieves all staff attendance records for a given date.
     */
    List<StaffDailyAttendance> findByAttendanceDate(LocalDate attendanceDate);

    long countByStaffId(Long staffId);

    @Query("""
            SELECT COUNT(sda)
            FROM StaffDailyAttendance sda
            WHERE sda.staffId = :staffId
              AND sda.attendanceType.isPresentMark = true
            """)
    long countPresentByStaffId(@Param("staffId") Long staffId);
}