package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.GradingScale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradingScaleRepository extends JpaRepository<GradingScale, Long> {
    boolean existsByGrade(String grade);
}

