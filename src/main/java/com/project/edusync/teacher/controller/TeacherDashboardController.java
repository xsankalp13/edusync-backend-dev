package com.project.edusync.teacher.controller;

import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.teacher.model.dto.TeacherDashboardSummaryResponseDto;
import com.project.edusync.teacher.model.dto.TeacherHomeroomResponseDto;
import com.project.edusync.teacher.model.dto.TeacherMyClassesResponseDto;
import com.project.edusync.teacher.model.dto.TeacherScheduleResponseDto;
import com.project.edusync.teacher.model.dto.TeacherStudentResponseDto;
import com.project.edusync.teacher.service.AttendanceExportService;
import com.project.edusync.teacher.service.TeacherDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherDashboardController {

    private final TeacherDashboardService teacherDashboardService;
    private final AttendanceExportService attendanceExportService;
    private final AuthUtil authUtil;

    @GetMapping("/my-classes")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<TeacherMyClassesResponseDto>> getMyClasses() {
        return ResponseEntity.ok(teacherDashboardService.getMyClasses(authUtil.getCurrentUserId()));
    }

    @GetMapping("/class-teacher/class")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<TeacherMyClassesResponseDto>> getMyClassTeacherSections() {
        return ResponseEntity.ok(teacherDashboardService.getMyClassTeacherSections(authUtil.getCurrentUserId()));
    }

    @GetMapping("/my-students")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<Page<TeacherStudentResponseDto>> getMyStudents(
            @RequestParam(required = false) UUID classUuid,
            @RequestParam(required = false) UUID sectionUuid,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(Math.min(size, 200), 1);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return ResponseEntity.ok(teacherDashboardService.getMyStudents(
                authUtil.getCurrentUserId(),
                classUuid,
                sectionUuid,
                search,
                pageable
        ));
    }

    @GetMapping("/class-teacher/students")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<Page<TeacherStudentResponseDto>> getClassTeacherStudents(
            @RequestParam UUID sectionUuid,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(Math.min(size, 200), 1);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return ResponseEntity.ok(teacherDashboardService.getClassTeacherStudents(
                authUtil.getCurrentUserId(),
                sectionUuid,
                search,
                pageable
        ));
    }

    @GetMapping("/my-schedule")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<TeacherScheduleResponseDto> getMySchedule(@RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(teacherDashboardService.getMySchedule(authUtil.getCurrentUserId(), date));
    }

    @GetMapping("/dashboard-summary")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<TeacherDashboardSummaryResponseDto> getDashboardSummary(@RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(teacherDashboardService.getDashboardSummary(authUtil.getCurrentUserId(), date));
    }

    @GetMapping("/my-homeroom")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<TeacherHomeroomResponseDto> getMyHomeroom(@RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(teacherDashboardService.getMyHomeroom(authUtil.getCurrentUserId(), date));
    }

    @GetMapping("/attendance/export")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportAttendanceSheet(
            @RequestParam UUID sectionUuid,
            @RequestParam(required = false) LocalDate date
    ) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        byte[] pdf = attendanceExportService.exportDailyAttendanceSheet(authUtil.getCurrentUserId(), sectionUuid, targetDate);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"attendance-" + targetDate + ".pdf\"")
                .body(pdf);
    }
}