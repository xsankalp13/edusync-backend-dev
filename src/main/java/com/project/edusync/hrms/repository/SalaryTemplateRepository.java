package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.SalaryTemplate;
import com.project.edusync.uis.model.enums.StaffCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalaryTemplateRepository extends JpaRepository<SalaryTemplate, Long> {

    List<SalaryTemplate> findByActiveTrueOrderByTemplateNameAsc();

    @Query("""
            SELECT st FROM SalaryTemplate st
            WHERE st.active = true
              AND (st.applicableCategory IS NULL OR st.applicableCategory = :category)
            ORDER BY st.templateName ASC
            """)
    List<SalaryTemplate> findApplicableForCategory(@Param("category") StaffCategory category);

    Optional<SalaryTemplate> findByUuid(java.util.UUID uuid);
}

