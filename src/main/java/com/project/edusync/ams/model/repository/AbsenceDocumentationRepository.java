package com.project.edusync.ams.model.repository;

import com.project.edusync.ams.model.entity.AbsenceDocumentation;
import com.project.edusync.ams.model.enums.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repository for managing AbsenceDocumentation persistence.
 *
 * Notes:
 * - Methods here are additive and non-breaking.
 * - Consider adding a DB unique constraint on (attendance_id, submitted_by_user_id)
 *   if you want to enforce "one submission per parent per attendance" at the DB level.
 */
@Repository
public interface AbsenceDocumentationRepository extends JpaRepository<AbsenceDocumentation, Long> {

    /**
     * Retrieves absence documentation records by approval status.
     * Use the pageable sort to control ordering; the service currently uses createdAt desc.
     */
    Page<AbsenceDocumentation> findByApprovalStatus(ApprovalStatus approvalStatus, Pageable pageable);

    /**
     * Retrieves pending docs ordered by createdAt ascending (queue style).
     * Maintained for convenience; caller can also pass Pageable with sort.
     */
    Page<AbsenceDocumentation> findByApprovalStatusOrderByCreatedAtAsc(ApprovalStatus approvalStatus, Pageable pageable);

    /**
     * Finds documentation submitted by a specific user (e.g., a parent/guardian).
     */
    Page<AbsenceDocumentation> findBySubmittedByUserId(Long submittedByUserId, Pageable pageable);

    /**
     * Finds documentation approved by a specific staff member.
     */
    Page<AbsenceDocumentation> findByApprovedByStaffId(Long approvedByStaffId, Pageable pageable);

    /**
     * Find documentation for a specific attendance record (by attendance PK).
     * Returns Optional to indicate presence/absence.
     */
    Optional<AbsenceDocumentation> findByAttendanceId(Long attendanceId);

    /**
     * Helper that checks if a documentation exists for the given attendance id (fast boolean check).
     */
    boolean existsByAttendanceId(Long attendanceId);

    /**
     * Helper to check whether the same user has already submitted documentation for the same attendance.
     * Useful for preventing duplicate submissions by the same parent.
     */
    boolean existsByAttendanceIdAndSubmittedByUserId(Long attendanceId, Long submittedByUserId);

    /**
     * Get the most recent documentation for an attendance (if multiple allowed).
     * Useful if you preserve history and want the latest state.
     */
    Optional<AbsenceDocumentation> findFirstByAttendanceIdOrderByCreatedAtDesc(Long attendanceId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM AbsenceDocumentation a " +
            "WHERE a.attendance.studentId = :studentId " +
            "AND a.attendance.attendanceDate = :date " + // <--- Updated to match your entity
            "AND a.approvalStatus = :status")
    boolean existsByStudentIdAndDateAndApprovalStatus(
            @Param("studentId") Long studentId,
            @Param("date") LocalDate date,
            @Param("status") ApprovalStatus status
    );
}
