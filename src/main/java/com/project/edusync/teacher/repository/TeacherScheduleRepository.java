package com.project.edusync.teacher.repository;

import com.project.edusync.teacher.model.entity.TeacherSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TeacherScheduleRepository extends JpaRepository<TeacherSchedule, Long> {
    List<TeacherSchedule> findByTeacherIdAndStartTimeAfter(Long teacherId, LocalDateTime now);
}