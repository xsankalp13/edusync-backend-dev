package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.ExamEntryDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamEntryDecisionRepository extends JpaRepository<ExamEntryDecision, Long> {

    Optional<ExamEntryDecision> findByExamScheduleIdAndStudentId(Long examScheduleId, Long studentId);

    @Query("""
        SELECT eed FROM ExamEntryDecision eed
        WHERE eed.examSchedule.id IN :scheduleIds
          AND eed.student.id IN :studentIds
        """)
    List<ExamEntryDecision> findByExamScheduleIdsAndStudentIds(@Param("scheduleIds") Collection<Long> scheduleIds,
                                                               @Param("studentIds") Collection<Long> studentIds);
}

