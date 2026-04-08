package com.project.edusync.uis.controller;

import com.project.edusync.adm.model.dto.response.EditorContextResponseDto;
import com.project.edusync.adm.service.EditorContextService;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.teacher.model.dto.LectureLogResponseDto;
import com.project.edusync.teacher.service.LectureLogService;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.repository.StudentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Student-accessible timetable endpoints.
 * Resolves the student's own section from auth context and returns the published schedule
 * plus read-only access to teacher lecture logs.
 */
@RestController
@RequestMapping("${api.url}/student/timetable")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Student Timetable", description = "Published timetable and lecture log access for students")
public class StudentTimetableController {

    private final AuthUtil authUtil;
    private final StudentRepository studentRepository;
    private final EditorContextService editorContextService;
    private final LectureLogService lectureLogService;

    /**
     * Returns the full timetable context (timeslots, subjects, teachers, schedule entries)
     * for the authenticated student's section.
     * Reuses the same EditorContextResponseDto already consumed by the Admin TimetableReader.
     */
    @GetMapping("/context")
    @PreAuthorize("hasAnyAuthority('dashboard:read:own', 'ROLE_STUDENT', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(
            summary = "Get student timetable context",
            description = "Returns the published timetable for the authenticated student's class section.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<EditorContextResponseDto> getTimetableContext() {
        Long userId = authUtil.getCurrentUserId();
        Student student = studentRepository.findByUserProfile_User_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("No student record found for userId: " + userId));

        UUID sectionUuid = student.getSection().getUuid();
        log.info("Student timetable context requested: userId={} sectionUuid={}", userId, sectionUuid);

        EditorContextResponseDto context = editorContextService.getEditorContext(sectionUuid);
        return ResponseEntity.ok(context);
    }

    /**
     * Returns the lecture log saved by the teacher for a given schedule entry UUID.
     * Returns 204 No Content when the teacher has not yet logged this lecture.
     * Students can only read — not modify — logs.
     */
    @GetMapping("/lecture-log/{scheduleUuid}")
    @PreAuthorize("hasAnyAuthority('dashboard:read:own', 'ROLE_STUDENT', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(
            summary = "Get lecture log for a schedule entry",
            description = "Returns the teacher's lecture log for the given schedule UUID, or 204 if not yet logged.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<LectureLogResponseDto> getLectureLog(@PathVariable UUID scheduleUuid) {
        log.debug("Student lecture log requested for scheduleUuid={}", scheduleUuid);
        return lectureLogService.getLectureLogByScheduleUuid(scheduleUuid)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
