package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.StudentMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentMarkRepository extends JpaRepository<StudentMark, Long> {

    interface PerformanceTrendView {
        String getTerm();

        BigDecimal getScore();
    }

    Optional<StudentMark> findByUuid(UUID uuid);

    List<StudentMark> findByExamSchedule_Id(Long id);

    @Query("""
            SELECT e.name as term,
                   AVG(CASE
                       WHEN es.maxMarks > 0 AND sm.marksObtained IS NOT NULL
                       THEN (sm.marksObtained * 10.0 / es.maxMarks)
                       ELSE NULL
                   END) as score
            FROM StudentMark sm
            JOIN sm.examSchedule es
            JOIN es.exam e
            WHERE sm.student.id = :studentId
            GROUP BY e.id, e.name, e.startDate
            ORDER BY e.startDate ASC
            """)
    List<PerformanceTrendView> findPerformanceTrendByStudentId(@Param("studentId") Long studentId);
}