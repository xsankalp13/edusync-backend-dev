package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.ExamControllerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamControllerAssignmentRepository extends JpaRepository<ExamControllerAssignment, Long> {

    Optional<ExamControllerAssignment> findByExamIdAndActiveTrue(Long examId);

    boolean existsByExamIdAndStaffIdAndActiveTrue(Long examId, Long staffId);

    long countByStaffIdAndActiveTrueAndExamIdNot(Long staffId, Long examId);

    @Query("""
        SELECT eca.exam.id
        FROM ExamControllerAssignment eca
        WHERE eca.staff.id = :staffId
          AND eca.active = true
        """)
    List<Long> findActiveExamIdsByStaffId(@Param("staffId") Long staffId);
}

