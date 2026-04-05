package com.project.edusync.teacher.repository;

import com.project.edusync.teacher.model.entity.LessonPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonPlanRepository extends JpaRepository<LessonPlan, Long> {
    List<LessonPlan> findByTeacherId(Long teacherId);
}