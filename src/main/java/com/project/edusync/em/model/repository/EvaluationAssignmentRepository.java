package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.EvaluationAssignment;
import com.project.edusync.em.model.enums.EvaluationAssignmentRole;
import com.project.edusync.em.model.enums.EvaluationAssignmentStatus;
import com.project.edusync.em.model.enums.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EvaluationAssignmentRepository extends JpaRepository<EvaluationAssignment, Long> {

    @Query("SELECT ea.examSchedule.exam.id FROM EvaluationAssignment ea WHERE ea.id = :assignmentId")
    Optional<Long> findExamIdByAssignmentId(@Param("assignmentId") Long assignmentId);

    // ── Fetching with joins (teacher dashboard & admin list) ────────────

    @Query("""
            SELECT ea FROM EvaluationAssignment ea
            JOIN FETCH ea.examSchedule es
            JOIN FETCH es.exam ex
            JOIN FETCH es.subject sub
            JOIN FETCH ea.teacher t
            WHERE t.id = :teacherId
            ORDER BY ea.assignedAt DESC
            """)
    List<EvaluationAssignment> findAllByTeacherIdWithSchedule(@Param("teacherId") Long teacherId);

    @Query("""
            SELECT ea FROM EvaluationAssignment ea
            JOIN FETCH ea.examSchedule es
            JOIN FETCH es.exam ex
            JOIN FETCH es.subject sub
            JOIN FETCH ea.teacher t
            WHERE (:teacherId IS NULL OR t.id = :teacherId)
            ORDER BY ea.assignedAt DESC
            """)
    List<EvaluationAssignment> findAllWithSchedule(@Param("teacherId") Long teacherId);

    // ── Legacy (any-role) lookups — kept for backward compat ───────────

    Optional<EvaluationAssignment> findByExamScheduleIdAndTeacherId(Long scheduleId, Long teacherId);

    boolean existsByExamScheduleIdAndTeacherId(Long scheduleId, Long teacherId);

    long countByExamScheduleIdAndTeacherIdAndStatusNot(Long scheduleId, Long teacherId, EvaluationAssignmentStatus status);

    // ── Role-specific lookups ──────────────────────────────────────────

    Optional<EvaluationAssignment> findByExamScheduleIdAndTeacherIdAndRole(
            Long scheduleId, Long teacherId, EvaluationAssignmentRole role);

    boolean existsByExamScheduleIdAndTeacherIdAndRole(
            Long scheduleId, Long teacherId, EvaluationAssignmentRole role);

    boolean existsByExamScheduleIdAndRole(Long scheduleId, EvaluationAssignmentRole role);

    boolean existsByExamScheduleIdAndTeacherIdAndRoleIn(
            Long scheduleId, Long teacherId, Collection<EvaluationAssignmentRole> roles);

    // ── Upload lifecycle queries ───────────────────────────────────────

    /** All UPLOADER assignments for a schedule */
    List<EvaluationAssignment> findByExamScheduleIdAndRole(Long scheduleId, EvaluationAssignmentRole role);

    /** Check if any uploader has NOT yet completed */
    boolean existsByExamScheduleIdAndRoleAndUploadStatusNot(
            Long scheduleId, EvaluationAssignmentRole role, UploadStatus uploadStatus);

    /** Check if evaluator has started (status != ASSIGNED) */
    boolean existsByExamScheduleIdAndRoleAndStatusNot(
            Long scheduleId, EvaluationAssignmentRole role, EvaluationAssignmentStatus status);
}
