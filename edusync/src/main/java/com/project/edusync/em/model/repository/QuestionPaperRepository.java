package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.QuestionPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionPaperRepository extends JpaRepository<QuestionPaper, Long> {
    Optional<QuestionPaper> findByUuid(UUID uuid);
    Optional<QuestionPaper> findByExamSchedule_ScheduleId(Long scheduleId);
}