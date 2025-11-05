package com.project.edusync.uis.repository;

import com.project.edusync.uis.model.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student,Long> {
    boolean existsByEnrollmentNumber(String enrollmentNumber);
}
