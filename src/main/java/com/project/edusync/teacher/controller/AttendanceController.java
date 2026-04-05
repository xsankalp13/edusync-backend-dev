package com.project.edusync.teacher.controller;

import com.project.edusync.teacher.model.entity.Attendance;
import com.project.edusync.teacher.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/teacher/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/mark")
    public ResponseEntity<List<Attendance>> markAttendance(@RequestBody List<Attendance> attendanceList) {
        return ResponseEntity.ok(attendanceService.markAttendance(attendanceList));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<Attendance>> getAttendanceByDate(
            @PathVariable LocalDate date,
            @RequestParam String teacherUsername) {
        return ResponseEntity.ok(attendanceService.getAttendanceByDate(date, teacherUsername));
    }
}