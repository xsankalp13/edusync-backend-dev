package com.project.edusync.teacher.repository;

import com.project.edusync.teacher.model.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByStudentId(Long studentId);
}