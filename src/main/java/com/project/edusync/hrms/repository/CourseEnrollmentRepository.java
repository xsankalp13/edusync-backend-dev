package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {
    List<CourseEnrollment> findByStaff_Id(Long staffId);
    Optional<CourseEnrollment> findByCourse_IdAndStaff_Id(Long courseId, Long staffId);
    long countByCourse_IdAndActiveTrue(Long courseId);
    List<CourseEnrollment> findByCourse_Id(Long courseId);
}

