package com.project.edusync.teacher.controller;

import com.project.edusync.teacher.model.entity.LessonPlan;
import com.project.edusync.teacher.service.LessonPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/lesson-plan")
@RequiredArgsConstructor
public class LessonPlanController {

    private final LessonPlanService lessonPlanService;

    @GetMapping
    public ResponseEntity<List<LessonPlan>> getLessonPlans(@RequestParam Long teacherId) {
        return ResponseEntity.ok(lessonPlanService.getLessonPlans(teacherId));
    }

    @PostMapping
    public ResponseEntity<LessonPlan> saveLessonPlan(@RequestBody LessonPlan lessonPlan) {
        return ResponseEntity.ok(lessonPlanService.saveLessonPlan(lessonPlan));
    }
}