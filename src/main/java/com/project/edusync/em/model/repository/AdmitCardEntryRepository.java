package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.AdmitCardEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AdmitCardEntryRepository extends JpaRepository<AdmitCardEntry, Long> {

    interface AdmitCardEntryKeyProjection {
        Long getScheduleId();
        Long getStudentId();
    }

    @Query("""
            SELECT ace FROM AdmitCardEntry ace
            JOIN FETCH ace.admitCard ac
            JOIN FETCH ace.examSchedule es
            WHERE ac.id = :admitCardId
            ORDER BY ace.examDate ASC, ace.startTime ASC
            """)
    List<AdmitCardEntry> findByAdmitCardIdWithSchedule(@Param("admitCardId") Long admitCardId);

    @Modifying
    @Query("DELETE FROM AdmitCardEntry ace WHERE ace.examSchedule.id = :scheduleId")
    int deleteByExamScheduleId(@Param("scheduleId") Long scheduleId);

    @Query("""
            SELECT ace.examSchedule.id AS scheduleId,
                   ace.admitCard.student.id AS studentId
            FROM AdmitCardEntry ace
            JOIN ace.admitCard ac
            WHERE ac.exam.id = :examId
              AND ace.examSchedule.id IN :scheduleIds
              AND ac.student.id IN :studentIds
            """)
    List<AdmitCardEntryKeyProjection> findEntryKeysByExamAndScheduleAndStudentIds(@Param("examId") Long examId,
                                                                                   @Param("scheduleIds") Collection<Long> scheduleIds,
                                                                                   @Param("studentIds") Collection<Long> studentIds);
}
