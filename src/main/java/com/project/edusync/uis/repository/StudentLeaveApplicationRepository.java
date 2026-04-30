package com.project.edusync.uis.repository;

import com.project.edusync.uis.model.entity.StudentLeaveApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentLeaveApplicationRepository extends JpaRepository<StudentLeaveApplication, Long> {
    List<StudentLeaveApplication> findByStudent_Id(Long studentId);
}

