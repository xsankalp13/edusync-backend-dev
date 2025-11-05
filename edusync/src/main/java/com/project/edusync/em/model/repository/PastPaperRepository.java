package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.PastPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PastPaperRepository extends JpaRepository<PastPaper, Long> {
    Optional<PastPaper> findByUuid(UUID uuid);
}