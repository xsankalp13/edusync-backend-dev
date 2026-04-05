package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.LeaveTypeConfig;
import com.project.edusync.uis.model.enums.StaffCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaveTypeConfigRepository extends JpaRepository<LeaveTypeConfig, Long> {

    List<LeaveTypeConfig> findByActiveTrueOrderBySortOrderAscLeaveCodeAsc();

    Optional<LeaveTypeConfig> findByLeaveCodeIgnoreCaseAndActiveTrue(String leaveCode);

    Optional<LeaveTypeConfig> findByLeaveCodeIgnoreCase(String leaveCode);

    @Query("""
            SELECT DISTINCT lt FROM LeaveTypeConfig lt
            LEFT JOIN lt.applicableCategories c
            WHERE lt.active = true
              AND (c IS NULL OR c = :category)
            ORDER BY lt.sortOrder ASC, lt.leaveCode ASC
            """)
    List<LeaveTypeConfig> findApplicableForCategory(@Param("category") StaffCategory category);

    boolean existsByLeaveCodeIgnoreCaseAndActiveTrue(String leaveCode);

    boolean existsByLeaveCodeIgnoreCaseAndActiveTrueAndIdNot(String leaveCode, Long id);

    Optional<LeaveTypeConfig> findByUuid(java.util.UUID uuid);
}

