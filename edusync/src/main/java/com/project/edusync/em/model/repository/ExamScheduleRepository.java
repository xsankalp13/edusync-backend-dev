package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.Exam;
import com.project.edusync.em.model.entity.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExamScheduleRepository extends JpaRepository<ExamSchedule , Long> {
List<ExamSchedule> findByExamUuid (UUID examUuid);
}
