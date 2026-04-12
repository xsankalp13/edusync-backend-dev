package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.AdmitCard;
import com.project.edusync.em.model.enums.AdmitCardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdmitCardRepository extends JpaRepository<AdmitCard, Long> {

    @Query("""
            SELECT ac FROM AdmitCard ac
            JOIN FETCH ac.student st
            JOIN FETCH st.userProfile up
            JOIN FETCH ac.exam ex
            WHERE ex.id = :examId
            ORDER BY up.firstName ASC, up.lastName ASC
            """)
    List<AdmitCard> findByExamIdWithStudent(@Param("examId") Long examId);

    @Query("""
            SELECT ac FROM AdmitCard ac
            JOIN FETCH ac.student st
            JOIN FETCH st.userProfile up
            JOIN FETCH ac.exam ex
            WHERE ex.id = :examId
              AND st.id = :studentId
            """)
    Optional<AdmitCard> findByExamIdAndStudentIdWithContext(@Param("examId") Long examId,
                                                            @Param("studentId") Long studentId);

    @Query("""
            SELECT ac FROM AdmitCard ac
            WHERE ac.exam.id = :examId
              AND ac.status = :status
            """)
    List<AdmitCard> findByExamIdAndStatusWithStudent(@Param("examId") Long examId,
                                                     @Param("status") AdmitCardStatus status);

    @Query("""
            SELECT ac FROM AdmitCard ac
            WHERE ac.exam.id = :examId
              AND ac.student.id IN :studentIds
            """)
    List<AdmitCard> findByExamIdAndStudentIds(@Param("examId") Long examId,
                                              @Param("studentIds") List<Long> studentIds);

    @Query("""
            SELECT DISTINCT ac FROM AdmitCard ac
            JOIN ac.entries e
            WHERE ac.exam.id = :examId
              AND ac.status = 'GENERATED'
              AND e.examSchedule.id IN :scheduleIds
            """)
    List<AdmitCard> findDraftCardsByExamAndScheduleIds(@Param("examId") Long examId,
                                                       @Param("scheduleIds") List<Long> scheduleIds);

    long deleteByExam_Id(Long examId);

    @Query("""
            SELECT ac FROM AdmitCard ac
            JOIN FETCH ac.student st
            JOIN FETCH st.userProfile up
            JOIN FETCH ac.exam ex
            WHERE ac.id = :admitCardId
            """)
    Optional<AdmitCard> findByIdWithContext(@Param("admitCardId") Long admitCardId);

    Optional<AdmitCard> findByExam_IdAndStudent_Id(Long examId, Long studentId);

    @Query("""
            SELECT COUNT(ac)
            FROM AdmitCard ac
            WHERE ac.exam.id = :examId
              AND ac.status IN :statuses
            """)
    long countByExamIdAndStatuses(@Param("examId") Long examId,
                                  @Param("statuses") List<AdmitCardStatus> statuses);

    @Query("""
            SELECT COUNT(DISTINCT ac.id)
            FROM AdmitCard ac
            JOIN ac.entries e
            WHERE ac.exam.id = :examId
              AND e.examSchedule.id IN :scheduleIds
              AND ac.status IN :statuses
            """)
    long countByExamIdAndScheduleIdsAndStatuses(@Param("examId") Long examId,
                                                @Param("scheduleIds") List<Long> scheduleIds,
                                                @Param("statuses") List<AdmitCardStatus> statuses);

    @Query("""
            SELECT ac.status, COUNT(ac)
            FROM AdmitCard ac
            WHERE ac.exam.id = :examId
            GROUP BY ac.status
            """)
    List<Object[]> countByStatusForExam(@Param("examId") Long examId);

    @Query("""
            SELECT COUNT(ac)
            FROM AdmitCard ac
            WHERE ac.exam.id = :examId
              AND ac.status IN ('GENERATED', 'PUBLISHED')
            """)
    long countGeneratedOrPublishedByExamId(@Param("examId") Long examId);

    @Query("""
            SELECT COUNT(ac)
            FROM AdmitCard ac
            WHERE ac.exam.id = :examId
              AND ac.status = 'PUBLISHED'
            """)
    long countPublishedByExamId(@Param("examId") Long examId);

    @Query("""
            SELECT COUNT(ac)
            FROM AdmitCard ac
            WHERE ac.exam.id = :examId
              AND ac.status = 'FAILED'
            """)
    long countFailedByExamId(@Param("examId") Long examId);

    @Query("""
            SELECT e.examSchedule.id, COUNT(DISTINCT ac.id)
            FROM AdmitCard ac
            JOIN ac.entries e
            WHERE ac.exam.id = :examId
              AND ac.status IN ('GENERATED', 'PUBLISHED')
            GROUP BY e.examSchedule.id
            """)
    List<Object[]> countGeneratedPerSchedule(@Param("examId") Long examId);

    @Query("""
            SELECT e.examSchedule.id, COUNT(DISTINCT ac.id)
            FROM AdmitCard ac
            JOIN ac.entries e
            WHERE ac.exam.id = :examId
              AND ac.status = 'PUBLISHED'
            GROUP BY e.examSchedule.id
            """)
    List<Object[]> countPublishedPerSchedule(@Param("examId") Long examId);
}
