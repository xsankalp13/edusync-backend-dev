package com.project.edusync.teacher.service;

import java.util.Map;

public interface TeacherDashboardService {
    Map<String, Object> getPulseChart(Long classId);
    Map<String, Object> getAtRiskStudents(Long classId);
    Object getHeatmap(Long classId);
}