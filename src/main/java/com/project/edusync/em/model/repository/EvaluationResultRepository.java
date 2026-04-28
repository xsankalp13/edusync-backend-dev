package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.EvaluationResult;
import com.project.edusync.em.model.enums.EvaluationResultStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, Long> {

    @Query("SELECT er.answerSheet.examSchedule.exam.id FROM EvaluationResult er WHERE er.id = :resultId")
    Optional<Long> findExamIdByResultId(@Param("resultId") Long resultId);

    @Query("SELECT er.id, er.answerSheet.examSchedule.exam.id FROM EvaluationResult er WHERE er.id IN :resultIds")
    List<Object[]> findResultIdAndExamIdByResultIds(@Param("resultIds") List<Long> resultIds);

    Optional<EvaluationResult> findByAnswerSheetId(Long answerSheetId);

    @Query("""
            SELECT er FROM EvaluationResult er
            JOIN FETCH er.answerSheet a
            WHERE a.id = :answerSheetId
            """)
    Optional<EvaluationResult> findByAnswerSheetIdWithAnswerSheet(@Param("answerSheetId") Long answerSheetId);

    @Query("""
            SELECT er FROM EvaluationResult er
            JOIN FETCH er.answerSheet a
            JOIN FETCH a.student st
            JOIN FETCH st.userProfile up
            JOIN FETCH a.examSchedule es
            JOIN FETCH es.exam ex
            JOIN FETCH es.subject sub
            JOIN FETCH es.academicClass ac
            LEFT JOIN FETCH er.approvedBy ab
            WHERE er.status = :status
            ORDER BY COALESCE(er.submittedAt, er.createdAt) ASC
            """)
    List<EvaluationResult> findAllByStatusWithContext(@Param("status") EvaluationResultStatus status);

    @Query("""
            SELECT er FROM EvaluationResult er
            JOIN FETCH er.answerSheet a
            JOIN FETCH a.student st
            JOIN FETCH st.userProfile up
            JOIN FETCH a.examSchedule es
            JOIN FETCH es.exam ex
            JOIN FETCH es.subject sub
            JOIN FETCH es.academicClass ac
            LEFT JOIN FETCH er.approvedBy ab
            ORDER BY er.createdAt DESC
            """)
    List<EvaluationResult> findAllWithContext();

    @Query("""
            SELECT er FROM EvaluationResult er
            JOIN FETCH er.answerSheet a
            JOIN FETCH a.student st
            JOIN FETCH st.userProfile up
            JOIN FETCH a.examSchedule es
            JOIN FETCH es.exam ex
            JOIN FETCH es.subject sub
            JOIN FETCH es.academicClass ac
            LEFT JOIN FETCH er.approvedBy ab
            WHERE er.id = :resultId
            """)
    Optional<EvaluationResult> findByIdWithContext(@Param("resultId") Long resultId);

    @Query("""
            SELECT er FROM EvaluationResult er
            JOIN FETCH er.answerSheet a
            JOIN FETCH a.student st
            JOIN FETCH st.userProfile up
            JOIN FETCH a.examSchedule es
            JOIN FETCH es.exam ex
            JOIN FETCH es.subject sub
            JOIN FETCH es.academicClass ac
            LEFT JOIN FETCH er.approvedBy ab
            WHERE st.id = :studentId
              AND er.status = :status
            ORDER BY er.publishedAt DESC, er.createdAt DESC
            """)
    List<EvaluationResult> findByStudentIdAndStatusWithContext(@Param("studentId") Long studentId,
                                                               @Param("status") EvaluationResultStatus status);

    @Query("""
            SELECT er FROM EvaluationResult er
            JOIN FETCH er.answerSheet a
            JOIN FETCH a.student st
            JOIN FETCH st.userProfile up
            JOIN FETCH a.examSchedule es
            JOIN FETCH es.exam ex
            JOIN FETCH es.subject sub
            JOIN FETCH es.academicClass ac
            LEFT JOIN FETCH er.approvedBy ab
            WHERE er.id = :resultId
              AND st.id = :studentId
              AND er.status = :status
            """)
    Optional<EvaluationResult> findByIdAndStudentIdAndStatusWithContext(@Param("resultId") Long resultId,
                                                                         @Param("studentId") Long studentId,
                                                                         @Param("status") EvaluationResultStatus status);

    @Query("""
            SELECT er FROM EvaluationResult er
            JOIN FETCH er.answerSheet a
            JOIN FETCH a.examSchedule es
            WHERE er.id IN :resultIds
              AND er.status = :status
            """)
    List<EvaluationResult> findAllByIdInAndStatusWithContext(@Param("resultIds") List<Long> resultIds,
                                                             @Param("status") EvaluationResultStatus status);
}

