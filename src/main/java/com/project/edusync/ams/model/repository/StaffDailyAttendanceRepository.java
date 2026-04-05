package com.project.edusync.ams.model.repository;

import com.project.edusync.ams.model.entity.StaffDailyAttendance;
import com.project.edusync.uis.model.enums.StaffCategory;
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

    long countByStaffIdAndAttendanceDateBetween(Long staffId, LocalDate startDate, LocalDate endDate);

    @Query("""
            SELECT COUNT(sda)
            FROM StaffDailyAttendance sda
            WHERE sda.staffId = :staffId
              AND sda.attendanceDate BETWEEN :startDate AND :endDate
              AND sda.attendanceType.isPresentMark = true
            """)
    long countPresentByStaffIdAndDateBetween(
            @Param("staffId") Long staffId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT COUNT(sda)
            FROM StaffDailyAttendance sda
            WHERE sda.staffId = :staffId
              AND sda.attendanceDate BETWEEN :startDate AND :endDate
              AND sda.attendanceType.isAbsenceMark = true
            """)
    long countAbsentByStaffIdAndDateBetween(
            @Param("staffId") Long staffId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT COUNT(DISTINCT sda.staffId)
            FROM StaffDailyAttendance sda
            WHERE sda.attendanceDate = :date
              AND sda.attendanceType.isPresentMark = true
            """)
    long countDistinctPresentStaffByDate(@Param("date") LocalDate date);

    @Query("""
            SELECT COUNT(DISTINCT sda.staffId)
            FROM StaffDailyAttendance sda
            WHERE sda.attendanceDate = :date
              AND sda.attendanceType.isAbsenceMark = true
            """)
    long countDistinctAbsentStaffByDate(@Param("date") LocalDate date);

    @Query("""
            SELECT COUNT(DISTINCT sda.staffId)
            FROM StaffDailyAttendance sda, Staff st
            WHERE st.id = sda.staffId
              AND st.isActive = true
              AND st.category = :category
              AND sda.attendanceDate = :date
              AND sda.attendanceType.isPresentMark = true
            """)
    long countDistinctPresentStaffByDateAndCategory(@Param("date") LocalDate date, @Param("category") StaffCategory category);

    @Query("""
            SELECT COUNT(DISTINCT sda.staffId)
            FROM StaffDailyAttendance sda, Staff st
            WHERE st.id = sda.staffId
              AND st.isActive = true
              AND st.category = :category
              AND sda.attendanceDate = :date
              AND sda.attendanceType.isAbsenceMark = true
            """)
    long countDistinctAbsentStaffByDateAndCategory(@Param("date") LocalDate date, @Param("category") StaffCategory category);
}