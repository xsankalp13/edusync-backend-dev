package com.project.edusync.discipline.controller;

import com.project.edusync.discipline.model.dto.AdminRemarkDTO;
import com.project.edusync.discipline.model.dto.CreateRemarkRequest;
import com.project.edusync.discipline.model.dto.StudentRemarkDTO;
import com.project.edusync.discipline.model.enums.RemarkTag;
import com.project.edusync.discipline.service.DisciplineRemarkService;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DisciplineRemarkController {

    private final DisciplineRemarkService remarkService;
    private final UserRepository userRepository;

    // ─── Teacher Endpoints ───────────────────────────────────────────────

    /**
     * POST /api/v1/teacher/discipline/remarks
     * Teacher creates a remark about a student.
     */
    @PostMapping("${api.url}/teacher/discipline/remarks")
    public ResponseEntity<StudentRemarkDTO> createRemark(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateRemarkRequest request) {

        User user = getUser(userDetails);
        StudentRemarkDTO dto = remarkService.createRemark(request, user);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    /**
     * GET /api/v1/teacher/discipline/remarks
     * Teacher views their own previous remarks.
     */
    @GetMapping("${api.url}/teacher/discipline/remarks")
    public ResponseEntity<List<StudentRemarkDTO>> getTeacherRemarks(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        return ResponseEntity.ok(remarkService.getTeacherRemarks(user));
    }

    // ─── Student Endpoint ────────────────────────────────────────────────

    /**
     * GET /api/v1/student/discipline/remarks
     * Student views remarks about themselves.
     */
    @GetMapping("${api.url}/student/discipline/remarks")
    public ResponseEntity<List<StudentRemarkDTO>> getMyRemarks(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        return ResponseEntity.ok(remarkService.getMyRemarks(user));
    }

    // ─── Admin Endpoint ──────────────────────────────────────────────────

    /**
     * GET /api/v1/auth/discipline/remarks
     * Admin views all remarks, with optional filters and pagination/sorting.
     *
     * Query params:
     * - fromDate, toDate (yyyy-MM-dd)
     * - classUuid, sectionUuid, teacherUuid, studentUuid
     * - tag (EXCELLENT, GOOD, OKAY, BAD, NEEDS_IMPROVEMENT)
     * - page, size, sort (Spring Pageable)
     */
    @GetMapping("${api.url}/auth/discipline/remarks")
    public ResponseEntity<Page<AdminRemarkDTO>> getAllRemarks(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) UUID classUuid,
            @RequestParam(required = false) UUID sectionUuid,
            @RequestParam(required = false) RemarkTag tag,
            @RequestParam(required = false) UUID teacherUuid,
            @RequestParam(required = false) UUID studentUuid,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "remarkDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AdminRemarkDTO> page = remarkService.getAllRemarks(
                fromDate, toDate, classUuid, sectionUuid, tag, teacherUuid, studentUuid, search, pageable
        );
        return ResponseEntity.ok(page);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
    }
}
