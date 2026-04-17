package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.TemplateSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemplateSectionRepository extends JpaRepository<TemplateSection, Long> {

    @Modifying
    @Query("DELETE FROM TemplateSection s WHERE s.template.id = :templateId")
    int deleteByTemplateId(@Param("templateId") Long templateId);
}

