package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.SalaryTemplateComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalaryTemplateComponentRepository extends JpaRepository<SalaryTemplateComponent, Long> {

    List<SalaryTemplateComponent> findByTemplate_IdAndActiveTrueOrderByComponent_SortOrderAscComponent_ComponentCodeAsc(Long templateId);

    boolean existsByTemplate_IdAndComponent_IdAndActiveTrue(Long templateId, Long componentId);

    void deleteByTemplate_Id(Long templateId);
}

