package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamRepository extends JpaRepository <Exam , Long> {
    Optional<Exam> findByUuid(UUID uuid);

    @Query("SELECT e.id FROM Exam e WHERE e.uuid = :uuid")
    Optional<Long> findIdByUuid(@Param("uuid") UUID uuid);
}
