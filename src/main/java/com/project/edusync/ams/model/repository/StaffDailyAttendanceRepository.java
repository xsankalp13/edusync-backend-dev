package com.project.edusync.ams.model.repository;


import com.project.edusync.ams.model.entity.StaffDailyAttendance;
import com.project.edusync.uis.model.enums.StaffCategory;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffDailyAttendanceRepository extends JpaRepository<StaffDailyAttendance, Long>, JpaSpecificationExecutor<StaffDailyAttendance> {

    /**
     * Projection for a single date's attendance
     */
    interface DailyAttendanceCountProjection {
        java.time.LocalDate getAttendanceDate();
        Long getPresentCount();
        Long getAbsentCount();
    }

    /**
     * Projection for category-level attendance on a specific date
     */
    interface CategoryAttendanceCountProjection {
        StaffCategory getCategory();
        Long getPresentCount();
        Long getAbsentCount();
    }

    interface StaffDailyStatusCountProjection {
        String getShortCode();
        Long getCount();
    }

    Optional<StaffDailyAttendance> findByUuid(UUID uuid);

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

    @Query("""
            SELECT sda.attendanceType.shortCode as shortCode, COUNT(sda.id) as count
            FROM StaffDailyAttendance sda, Staff st
            WHERE st.id = sda.staffId
              AND st.isActive = true
              AND sda.attendanceDate = :date
            GROUP BY sda.attendanceType.shortCode
            """)
    List<StaffDailyStatusCountProjection> countByDateGroupedByShortCode(@Param("date") LocalDate date);

    @Query("SELECT DISTINCT sda.staffId FROM StaffDailyAttendance sda WHERE sda.attendanceDate = :date")
    List<Long> findDistinctStaffIdsByDate(@Param("date") LocalDate date);

    @Query("""
            SELECT sda FROM StaffDailyAttendance sda
            WHERE sda.attendanceDate BETWEEN :fromDate AND :toDate
            """)
    List<StaffDailyAttendance> findAllByDateRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

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

    /**
     * Returns (attendanceDate, presentCount, absentCount) for each date in the range.
     * Replaces the N+1 day loop in buildHeatmap() — 1 query instead of 2×N.
     */
    @Query("""
            SELECT sda.attendanceDate as attendanceDate,
                   SUM(CASE WHEN sda.attendanceType.isPresentMark = true THEN 1 ELSE 0 END) as presentCount,
                   SUM(CASE WHEN sda.attendanceType.isAbsenceMark = true THEN 1 ELSE 0 END) as absentCount
            FROM StaffDailyAttendance sda
            WHERE sda.attendanceDate BETWEEN :startDate AND :endDate
            GROUP BY sda.attendanceDate
            ORDER BY sda.attendanceDate
            """)
    List<DailyAttendanceCountProjection> heatmapPresentAbsentByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Returns (category, presentCount, absentCount) for a specific date, grouped by StaffCategory.
     * Replaces 6 individual category-level count queries in buildCategoryAttendance().
     */
    @Query("""
            SELECT st.category as category,
                   SUM(CASE WHEN sda.attendanceType.isPresentMark = true THEN 1 ELSE 0 END) as presentCount,
                   SUM(CASE WHEN sda.attendanceType.isAbsenceMark = true THEN 1 ELSE 0 END) as absentCount
            FROM StaffDailyAttendance sda
            JOIN Staff st ON st.id = sda.staffId
            WHERE st.isActive = true
              AND sda.attendanceDate = :date
            GROUP BY st.category
            """)
    List<CategoryAttendanceCountProjection> countByDateGroupedByCategoryAndAttendanceType(
            @Param("date") LocalDate date
    );

    /**
     * Returns (attendanceDate, presentCount) for each date in the range.
     * Shared by MasterDashboardAnalyticsServiceImpl.buildAttendanceTrend() — 1 query instead of 14.
     */
    @Query("""
            SELECT sda.attendanceDate as attendanceDate,
                   COUNT(DISTINCT sda.staffId) as presentCount,
                   0L as absentCount
            FROM StaffDailyAttendance sda
            WHERE sda.attendanceDate BETWEEN :startDate AND :endDate
              AND sda.attendanceType.isPresentMark = true
            GROUP BY sda.attendanceDate
            ORDER BY sda.attendanceDate
            """)
    List<DailyAttendanceCountProjection> countPresentStaffByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}