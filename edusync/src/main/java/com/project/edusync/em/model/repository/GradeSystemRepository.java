package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.GradeSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GradeSystemRepository extends JpaRepository<GradeSystem , Long> {
    Optional<GradeSystem> findByUuid(UUID uuid);
}
