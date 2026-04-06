package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.StaffGradeAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

public interface StaffGradeAssignmentRepository extends JpaRepository<StaffGradeAssignment, Long> {

    Optional<StaffGradeAssignment> findByStaff_IdAndActiveTrueAndEffectiveToIsNull(Long staffId);

    List<StaffGradeAssignment> findByStaff_IdAndActiveTrueOrderByEffectiveFromDesc(Long staffId);

    Page<StaffGradeAssignment> findByActiveTrueAndEffectiveToIsNull(Pageable pageable);

    boolean existsByStaff_IdAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
            Long staffId,
            LocalDate fromDate,
            LocalDate toDate
    );

    @Query("""
            SELECT a.grade.gradeCode, a.grade.gradeName, COUNT(a)
            FROM StaffGradeAssignment a
            WHERE a.active = true
              AND a.effectiveTo IS NULL
            GROUP BY a.grade.gradeCode, a.grade.gradeName
            ORDER BY a.grade.gradeCode
            """)
    List<Object[]> gradeDistribution();
}


