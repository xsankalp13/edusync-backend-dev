package com.project.edusync.teacher.repository;

import com.project.edusync.teacher.model.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByDateAndRecordedBy(LocalDate date, String recordedBy);
}