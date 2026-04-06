package com.project.edusync.teacher.service;

import com.project.edusync.teacher.model.entity.Attendance;
import com.project.edusync.teacher.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    public List<Attendance> markAttendance(List<Attendance> attendanceList) {
        return attendanceRepository.saveAll(attendanceList);
    }

    public List<Attendance> getAttendanceByDate(LocalDate date, String teacherUsername) {
        return attendanceRepository.findByDateAndRecordedBy(date, teacherUsername);
    }
}