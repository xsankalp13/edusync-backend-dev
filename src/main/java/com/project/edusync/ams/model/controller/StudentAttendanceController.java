package com.project.edusync.ams.model.controller;

import com.project.edusync.ams.model.dto.request.StudentAttendanceRequestDTO;
import com.project.edusync.ams.model.dto.response.StudentAttendanceCompletionDTO;
import com.project.edusync.ams.model.dto.response.StudentAttendanceResponseDTO;
import com.project.edusync.ams.model.exception.AttendanceProcessingException;
import com.project.edusync.ams.model.service.StudentAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.time.LocalDate;

/**
 * Controller for Student Attendance endpoints using attendance short code.
 * Base path is configured in application properties and includes /auth/ so JWT is not required for testing
 * (WebSecurityConfig already permits /{apiVersion}/auth/**).
 *
 * NOTE: This controller intentionally **does not throw** when staff id is missing.
 * It passes the nullable performedByStaffId to the service which chooses between the header/principal
 * and the per-row takenByStaffId in the DTO (service preference: performedByStaffId -> dto.takenByStaffId).
 */
@RestController
@RequestMapping("${api.url}/auth/ams/records")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AMS Student Attendance", description = "UUID-first student attendance record APIs")
public class StudentAttendanceController {

    private final StudentAttendanceService service;

    /**
     * POST - mark a batch of student attendance.
     *
     * The controller no longer rejects when X-User-Id is missing — it forwards whatever it can
     * to the service. For testing you can either supply a header "X-User-Id" or include
     * "takenByStaffId" in each request object.
     */
    @PostMapping
    @Operation(summary = "Create student attendance records", description = "Accepts UUID-first attendance payloads")
    public ResponseEntity<List<StudentAttendanceResponseDTO>> createBatch(
            @RequestBody @Valid List<StudentAttendanceRequestDTO> requests,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            Authentication authentication) {

        // extract staff id if present in principal; otherwise headerUserId may be used (can be null)
        Long staffId = extractStaffId(authentication).orElse(headerUserId);

        // do NOT throw here — service will use request.takenByStaffId per-row when performedByStaffId is null
        List<StudentAttendanceResponseDTO> resp = service.markAttendanceBatch(requests, staffId);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    /**
     * GET list with optional filters.
     */
    @GetMapping
    @Operation(summary = "List student attendance", description = "Filters by UUID fields and supports stable sorting")
    public ResponseEntity<Page<StudentAttendanceResponseDTO>> list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort,
            @Parameter(description = "Filter by student UUID", schema = @Schema(format = "uuid"))
            @RequestParam(value = "studentUuid", required = false) UUID studentUuid,
            @Parameter(description = "Filter by attendance taker staff UUID", schema = @Schema(format = "uuid"))
            @RequestParam(value = "takenByStaffUuid", required = false) UUID takenByStaffUuid,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate,
            @RequestParam(value = "attendanceTypeShortCode", required = false) String attendanceTypeShortCode,
            @RequestParam(value = "classUuid", required = false) UUID classUuid,
            @RequestParam(value = "sectionUuid", required = false) UUID sectionUuid,
            @RequestParam(value = "search", required = false) String search
    ) {
        String[] sortParts = sort.split(",");
        Sort s;
        if (sortParts.length >= 2) {
            s = Sort.by(Sort.Direction.fromString(sortParts[1]), sortParts[0]);
        } else {
            s = Sort.by(Sort.Direction.DESC, sortParts[0]);
        }
        Pageable pageable = PageRequest.of(page, size, s);
        Page<StudentAttendanceResponseDTO> resp = service.listAttendances(pageable,
                Optional.ofNullable(studentUuid),
                Optional.ofNullable(takenByStaffUuid),
                Optional.ofNullable(fromDate),
                Optional.ofNullable(toDate),
                Optional.ofNullable(attendanceTypeShortCode),
                Optional.ofNullable(classUuid),
                Optional.ofNullable(sectionUuid),
                Optional.ofNullable(search));
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/completion")
    @Operation(summary = "Get student attendance completion by class/section and date range")
    public ResponseEntity<StudentAttendanceCompletionDTO> completion(
            @RequestParam("classUuid") UUID classUuid,
            @RequestParam(value = "sectionUuid", required = false) UUID sectionUuid,
            @RequestParam("fromDate") String fromDate,
            @RequestParam("toDate") String toDate
    ) {
        StudentAttendanceCompletionDTO resp = service.getAttendanceCompletion(
                classUuid,
                sectionUuid,
                LocalDate.parse(fromDate),
                LocalDate.parse(toDate)
        );
        return ResponseEntity.ok(resp);
    }

    /**
     * GET single record
     */
    @GetMapping("/{recordUuid}")
    @Operation(summary = "Get student attendance record by UUID")
    public ResponseEntity<StudentAttendanceResponseDTO> getById(
            @Parameter(description = "Attendance record UUID", schema = @Schema(format = "uuid"))
            @PathVariable UUID recordUuid) {
        StudentAttendanceResponseDTO resp = service.getAttendance(recordUuid);
        return ResponseEntity.ok(resp);
    }

    /**
     * PUT update
     */
    @PutMapping("/{recordUuid}")
    @Operation(summary = "Update student attendance record by UUID")
    public ResponseEntity<StudentAttendanceResponseDTO> update(
            @Parameter(description = "Attendance record UUID", schema = @Schema(format = "uuid"))
            @PathVariable UUID recordUuid,
            @RequestBody @Valid StudentAttendanceRequestDTO request,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            Authentication authentication) {

        Long staffId = extractStaffId(authentication).orElse(headerUserId);
        // do NOT throw here; pass nullable staffId to service
        StudentAttendanceResponseDTO resp = service.updateAttendance(recordUuid, request, staffId);
        return ResponseEntity.ok(resp);
    }

    /**
     * DELETE soft-delete
     */
    @DeleteMapping("/{recordUuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete student attendance record by UUID")
    public void delete(
            @Parameter(description = "Attendance record UUID", schema = @Schema(format = "uuid"))
            @PathVariable UUID recordUuid,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            Authentication authentication) {

        Long staffId = extractStaffId(authentication).orElse(headerUserId);
        service.deleteAttendance(recordUuid, staffId);
    }

    // Helper: extract staff ID from Authentication principal (if your principal stores it)
    private Optional<Long> extractStaffId(Authentication authentication) {
        if (authentication == null) return Optional.empty();
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.User user) {
            try {
                return Optional.of(Long.parseLong(user.getUsername()));
            } catch (NumberFormatException ignored) {}
        }
        // Add additional principal types if you use a custom JWT principal that stores the ID in claims
        return Optional.empty();
    }
}
