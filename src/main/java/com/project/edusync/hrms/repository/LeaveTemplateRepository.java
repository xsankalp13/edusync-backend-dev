package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.LeaveTemplate;
import com.project.edusync.uis.model.enums.StaffCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveTemplateRepository extends JpaRepository<LeaveTemplate, Long> {
    Optional<LeaveTemplate> findByUuid(UUID uuid);
    List<LeaveTemplate> findByAcademicYearAndActiveTrue(String academicYear);
    List<LeaveTemplate> findByAcademicYearAndApplicableCategoryAndActiveTrue(String academicYear, StaffCategory category);
    List<LeaveTemplate> findByActiveTrue();
    List<LeaveTemplate> findByApplicableCategoryAndActiveTrue(StaffCategory category);
}
