package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.StudentMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentMarkRepository extends JpaRepository<StudentMark, Long> {
    Optional<StudentMark> findByUuid(UUID uuid);
    List<StudentMark> findByExamSchedule_ScheduleId(Long scheduleId);
}