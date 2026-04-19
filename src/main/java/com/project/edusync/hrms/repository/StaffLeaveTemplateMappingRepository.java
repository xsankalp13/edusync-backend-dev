package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.StaffLeaveTemplateMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffLeaveTemplateMappingRepository extends JpaRepository<StaffLeaveTemplateMapping, Long> {
    Optional<StaffLeaveTemplateMapping> findByUuid(UUID uuid);
    List<StaffLeaveTemplateMapping> findByStaffIdAndActiveTrue(Long staffId);
    Optional<StaffLeaveTemplateMapping> findByStaffIdAndAcademicYearAndActiveTrue(Long staffId, String academicYear);
}
