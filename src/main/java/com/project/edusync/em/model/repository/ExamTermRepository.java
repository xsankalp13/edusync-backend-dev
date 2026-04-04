package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.ExamTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamTermRepository extends JpaRepository<ExamTerm, Long> {
    boolean existsByName(String name);
}

