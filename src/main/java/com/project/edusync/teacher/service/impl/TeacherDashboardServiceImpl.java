package com.project.edusync.teacher.service.impl;

import com.project.edusync.teacher.service.TeacherDashboardService;
import com.project.edusync.ams.model.repository.StudentDailyAttendanceRepository;
import com.project.edusync.uis.repository.StudentRepository;
import com.project.edusync.uis.model.entity.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherDashboardServiceImpl implements TeacherDashboardService {

    private final StudentDailyAttendanceRepository studentDailyAttendanceRepository;

    // We inject the StudentRepository to find which students belong to the class
    private final StudentRepository studentRepository;

    @Override
    public Map<String, Object> getPulseChart(Long classId) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Pulse chart data endpoint active.");
        return response;
    }

    @Override
    public Map<String, Object> getAtRiskStudents(Long classId) {
        long threshold = 3L; // Define at-risk as more than 3 absences

        // 1. Get all student IDs belonging to this class
        List<Long> studentIds = studentRepository.findByAcademicClassId(classId)
                .stream()
                .map(Student::getId)
                .collect(Collectors.toList());

        // 2. Safely query the database
        List<Long> atRiskStudentIds = java.util.Collections.emptyList();
        if (!studentIds.isEmpty()) {
            atRiskStudentIds = studentDailyAttendanceRepository.findAtRiskStudents(studentIds, threshold);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("threshold", threshold);
        response.put("atRiskStudentIds", atRiskStudentIds);
        response.put("count", atRiskStudentIds.size());

        return response;
    }

    @Override
    public Object getHeatmap(Long classId) {
        // 1. Get all student IDs belonging to this class
        List<Long> studentIds = studentRepository.findByAcademicClassId(classId)
                .stream()
                .map(Student::getId)
                .collect(Collectors.toList());

        // 2. Fetch absence density by DATE
        List<Object[]> densityData = java.util.Collections.emptyList();
        if (!studentIds.isEmpty()) {
            densityData = studentDailyAttendanceRepository.findAbsenceDensityByDate(studentIds);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("classId", classId);
        response.put("heatmapData", densityData);

        return response;
    }
}