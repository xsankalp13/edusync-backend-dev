package com.project.edusync.teacher.controller;

import com.project.edusync.adm.model.dto.response.ScheduleResponseDto;
import com.project.edusync.teacher.service.TeacherScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/schedule")
@RequiredArgsConstructor
public class TeacherScheduleController {

    private final TeacherScheduleService teacherScheduleService;

    @GetMapping("/section/{sectionId}")
    public ResponseEntity<List<ScheduleResponseDto>> getScheduleForSection(@PathVariable UUID sectionId) {
        return ResponseEntity.ok(teacherScheduleService.getScheduleForSection(sectionId));
    }

    @GetMapping("/{staffId}")
    public ResponseEntity<List<ScheduleResponseDto>> getScheduleForTeacher(@PathVariable Long staffId) {
        return ResponseEntity.ok(teacherScheduleService.getScheduleForTeacher(staffId));
    }
}