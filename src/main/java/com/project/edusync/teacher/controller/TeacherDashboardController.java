package com.project.edusync.teacher.controller;

import com.project.edusync.teacher.service.TeacherDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/teacher/dashboard")
@RequiredArgsConstructor
public class TeacherDashboardController {

    private final TeacherDashboardService teacherDashboardService;

    @GetMapping("/pulse-chart/{classId}")
    public ResponseEntity<?> getPulseChart(@PathVariable Long classId) {
        return ResponseEntity.ok(teacherDashboardService.getPulseChart(classId));
    }

    @GetMapping("/at-risk/{classId}")
    public ResponseEntity<?> getAtRiskStudents(@PathVariable Long classId) {
        return ResponseEntity.ok(teacherDashboardService.getAtRiskStudents(classId));
    }

    @GetMapping("/heatmap/{classId}")
    public ResponseEntity<?> getHeatmap(@PathVariable Long classId) {
        return ResponseEntity.ok(teacherDashboardService.getHeatmap(classId));
    }
}