package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.TrainingCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingCourseRepository extends JpaRepository<TrainingCourse, Long> {
    Optional<TrainingCourse> findByUuid(UUID uuid);
    List<TrainingCourse> findAllByActiveTrue();
}

