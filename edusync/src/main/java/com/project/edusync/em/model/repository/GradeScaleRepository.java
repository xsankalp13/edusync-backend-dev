package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.GradeScale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeScaleRepository extends JpaRepository<GradeScale , Long> {
}
