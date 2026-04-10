package com.project.edusync.ams.model.repository;

import com.project.edusync.ams.model.entity.StudentDailyAttendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentDailyAttendanceRepository extends JpaRepository<StudentDailyAttendance, Long>, JpaSpecificationExecutor<StudentDailyAttendance> {

    Optional<StudentDailyAttendance> findByUuid(UUID uuid);

    interface StudentAttendanceAggregateProjection {
        Long getStudentId();
        Long getPresentCount();
        Long getAbsentCount();
        Long getLateCount();
        Long getTotalCount();
    }

    /**
     * Finds the record for a specific student on a specific date, enforcing the unique constraint.
     * @param studentId The logical ID of the student (UIS FK).
     * @param attendanceDate The date.
     * @return The attendance record, if it exists.
     */
    Optional<StudentDailyAttendance> findByStudentIdAndAttendanceDate(
            Long studentId,
            LocalDate attendanceDate);

    /**
     * Retrieves all attendance records for a student, paginated and sorted by date descending.
     * Crucial for a student/parent attendance history view.
     */
    Page<StudentDailyAttendance> findByStudentIdOrderByAttendanceDateDesc(
            Long studentId,
            Pageable pageable);

    /**
     * Retrieves all attendance records taken by a staff member within a date range.
     * Useful for auditing and reporting on staff activity.
     */
    List<StudentDailyAttendance> findByTakenByStaffIdAndAttendanceDateBetween(
            Long takenByStaffId,
            LocalDate startDate,
            LocalDate endDate);

    /**
     * Retrieves attendance for a group of students on a given date (e.g., a specific section/class).
     */
    @Query("SELECT sda FROM StudentDailyAttendance sda WHERE sda.studentId IN :studentIds AND sda.attendanceDate = :attendanceDate")
    List<StudentDailyAttendance> findAttendanceByStudentIdsAndDate(
            @Param("studentIds") List<Long> studentIds,
            @Param("attendanceDate") LocalDate attendanceDate);

    @Query("""
            SELECT DISTINCT sda.attendanceDate FROM StudentDailyAttendance sda
            WHERE sda.studentId IN :studentIds
              AND sda.attendanceDate BETWEEN :fromDate AND :toDate
            ORDER BY sda.attendanceDate ASC
            """)
    List<LocalDate> findDistinctDatesWithRecords(
            @Param("studentIds") List<Long> studentIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    /**
     * Counts the number of attendance records for a student.
     * @param studentId The logical ID of the student (UIS FK).
     * @return The count of attendance records.
     */
    long countByStudentId(Long studentId);

    /**
     * Counts the number of present marks for a student.
     * @param studentId The logical ID of the student (UIS FK).
     * @return The count of present marks.
     */
    @Query("""
            SELECT COUNT(sda)
            FROM StudentDailyAttendance sda
            WHERE sda.studentId = :studentId
              AND sda.attendanceType.isPresentMark = true
            """)
    long countPresentByStudentId(@Param("studentId") Long studentId);

    // 1. At-Risk Students (Grouped by Student ID)
    @Query(value = "SELECT student_id FROM student_daily_attendance WHERE student_id IN :studentIds GROUP BY student_id HAVING COUNT(*) > :threshold", nativeQuery = true)
    List<Long> findAtRiskStudents(@Param("studentIds") List<Long> studentIds, @Param("threshold") long threshold);

    // 2. Heatmap Density (Grouped by Date, since Subject-level isn't tracked here)
    @Query(value = "SELECT attendance_date, COUNT(*) FROM student_daily_attendance WHERE student_id IN :studentIds GROUP BY attendance_date", nativeQuery = true)
    List<Object[]> findAbsenceDensityByDate(@Param("studentIds") List<Long> studentIds);

    @Query("""
            SELECT sda.studentId as studentId,
                   SUM(CASE WHEN sda.attendanceType.isPresentMark = true THEN 1 ELSE 0 END) as presentCount,
                   SUM(CASE WHEN sda.attendanceType.isAbsenceMark = true THEN 1 ELSE 0 END) as absentCount,
                   SUM(CASE WHEN sda.attendanceType.isLateMark = true THEN 1 ELSE 0 END) as lateCount,
                   COUNT(sda.id) as totalCount
            FROM StudentDailyAttendance sda
            WHERE sda.studentId IN :studentIds
              AND sda.attendanceDate BETWEEN :startDate AND :endDate
            GROUP BY sda.studentId
            """)
    List<StudentAttendanceAggregateProjection> summarizeAttendanceForStudents(
            @Param("studentIds") List<Long> studentIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT COUNT(DISTINCT sda.studentId)
            FROM StudentDailyAttendance sda
            WHERE sda.attendanceDate = :date
              AND sda.attendanceType.isPresentMark = true
            """)
    long countDistinctPresentStudentsByDate(@Param("date") LocalDate date);
}