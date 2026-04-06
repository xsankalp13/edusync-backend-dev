package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.StaffGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffGradeRepository extends JpaRepository<StaffGrade, Long> {

    List<StaffGrade> findByActiveTrueOrderBySortOrderAsc();

    Optional<StaffGrade> findByGradeCodeIgnoreCase(String gradeCode);

    boolean existsByGradeCodeIgnoreCaseAndActiveTrue(String gradeCode);

    boolean existsByGradeCodeIgnoreCaseAndActiveTrueAndIdNot(String gradeCode, Long id);

    Optional<StaffGrade> findByUuid(java.util.UUID uuid);
}

