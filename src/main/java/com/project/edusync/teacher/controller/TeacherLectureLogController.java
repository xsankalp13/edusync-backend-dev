package com.project.edusync.teacher.controller;

import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.teacher.model.dto.LectureLogRequestDto;
import com.project.edusync.teacher.model.dto.LectureLogResponseDto;
import com.project.edusync.teacher.model.dto.LectureLogUploadInitRequestDto;
import com.project.edusync.teacher.model.dto.LectureLogUploadInitResponseDto;
import com.project.edusync.teacher.service.LectureLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/lecture-logs")
@RequiredArgsConstructor
public class TeacherLectureLogController {

    private final LectureLogService lectureLogService;
    private final AuthUtil authUtil;

    @GetMapping("/schedule/{scheduleUuid}")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<LectureLogResponseDto> getLectureLog(@PathVariable UUID scheduleUuid) {
        return ResponseEntity.ok(lectureLogService.getLectureLog(authUtil.getCurrentUserId(), scheduleUuid));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<LectureLogResponseDto> saveLectureLog(@Valid @RequestBody LectureLogRequestDto requestDto) {
        return ResponseEntity.ok(lectureLogService.saveLectureLog(authUtil.getCurrentUserId(), requestDto));
    }

    @PostMapping("/upload-init")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<LectureLogUploadInitResponseDto> initUpload(@Valid @RequestBody LectureLogUploadInitRequestDto request) {
        return ResponseEntity.ok(lectureLogService.initUpload(authUtil.getCurrentUserId(), request));
    }
}
