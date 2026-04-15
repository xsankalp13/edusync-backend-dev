package com.project.edusync.ams.model.controller;

import com.project.edusync.ams.model.dto.request.StaffAttendanceRequestDTO;
import com.project.edusync.ams.model.dto.response.AttendanceCompletionDTO;
import com.project.edusync.ams.model.dto.response.StaffDailyStatsResponseDTO;
import com.project.edusync.ams.model.dto.response.StaffAttendanceResponseDTO;
import com.project.edusync.ams.model.service.StaffAttendanceService;
import com.project.edusync.uis.model.dto.admin.StaffSummaryDTO;
import com.project.edusync.uis.model.enums.StaffCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(value = "${api.url:/api/v1}/auth/ams/staff", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AMS Staff Attendance", description = "UUID-first staff attendance APIs")
public class StaffAttendanceController {

    private final StaffAttendanceService service;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create staff attendance")
    public ResponseEntity<StaffAttendanceResponseDTO> create(
            @Valid @RequestBody StaffAttendanceRequestDTO request) {

        Long callerUserId = resolveCallerUserId();
        log.debug("POST staff attendance request: staffUuid={}, date={}, callerUserId={}",
                request.getStaffUuid(), request.getAttendanceDate(), callerUserId);
        StaffAttendanceResponseDTO dto = service.createAttendance(request, callerUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping(path = "/bulk", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Bulk create staff attendance")
    public ResponseEntity<List<StaffAttendanceResponseDTO>> bulkCreate(
            @Valid @RequestBody List<StaffAttendanceRequestDTO> requests) {

        Long callerUserId = resolveCallerUserId();
        log.debug("POST bulk staff attendance request, count={}, callerUserId={}",
                requests == null ? 0 : requests.size(), callerUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.bulkCreate(requests, callerUserId));
    }

    @GetMapping
    @Operation(summary = "List staff attendance")
    public ResponseEntity<Page<StaffAttendanceResponseDTO>> list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort,
            @Parameter(description = "Filter by staff UUID", schema = @Schema(format = "uuid"))
            @RequestParam(value = "staffUuid", required = false) UUID staffUuid,
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "fromDate", required = false) String fromDateStr,
            @RequestParam(value = "toDate", required = false) String toDateStr,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search) {

        String[] sortParts = sort.split(",");
        Sort s = sortParts.length >= 2
                ? Sort.by(Sort.Direction.fromString(sortParts[1]), sortParts[0])
                : Sort.by(Sort.Direction.DESC, sortParts[0]);

        Pageable pageable = PageRequest.of(page, size, s);
        Optional<LocalDate> date = Optional.ofNullable(dateStr).filter(s1 -> !s1.isBlank()).map(LocalDate::parse);
        Optional<LocalDate> fromDate = Optional.ofNullable(fromDateStr).filter(s1 -> !s1.isBlank()).map(LocalDate::parse);
        Optional<LocalDate> toDate = Optional.ofNullable(toDateStr).filter(s1 -> !s1.isBlank()).map(LocalDate::parse);

        Optional<String> statusFilter = Optional.ofNullable(status).map(String::trim).filter(s1 -> !s1.isBlank());
        Optional<String> searchFilter = Optional.ofNullable(search).map(String::trim).filter(s1 -> !s1.isBlank());

        return ResponseEntity.ok(service.listAttendances(pageable, Optional.ofNullable(staffUuid), date, fromDate, toDate, statusFilter, searchFilter));
    }

    @GetMapping("/stats/daily")
    @Operation(summary = "Get organization-wide daily staff attendance stats")
    public ResponseEntity<StaffDailyStatsResponseDTO> getDailyStats(
            @RequestParam(value = "date", required = false) String dateStr) {
        Optional<LocalDate> date = Optional.ofNullable(dateStr).filter(s1 -> !s1.isBlank()).map(LocalDate::parse);
        return ResponseEntity.ok(service.getDailyStats(date));
    }

    @GetMapping("/attendance-completion")
    @Operation(summary = "Get monthly staff attendance completion stats")
    public ResponseEntity<AttendanceCompletionDTO> getAttendanceCompletion(
            @RequestParam("month") int month,
            @RequestParam("year") int year) {
        return ResponseEntity.ok(service.getAttendanceCompletion(month, year));
    }

    @GetMapping("/unmarked")
    @Operation(summary = "List staff with unmarked attendance for a date")
    public ResponseEntity<List<StaffSummaryDTO>> getUnmarkedStaff(
            @RequestParam("date") String date,
            @RequestParam(value = "category", required = false) StaffCategory category) {
        return ResponseEntity.ok(service.getUnmarkedStaff(LocalDate.parse(date), Optional.ofNullable(category)));
    }

    @GetMapping("/{recordUuid}")
    @Operation(summary = "Get staff attendance record by UUID")
    public ResponseEntity<StaffAttendanceResponseDTO> getById(
            @Parameter(description = "Staff attendance record UUID", schema = @Schema(format = "uuid"))
            @PathVariable UUID recordUuid) {
        return ResponseEntity.ok(service.getAttendance(recordUuid));
    }

    @PutMapping(path = "/{recordUuid}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update staff attendance record by UUID")
    public ResponseEntity<StaffAttendanceResponseDTO> update(
            @Parameter(description = "Staff attendance record UUID", schema = @Schema(format = "uuid"))
            @PathVariable UUID recordUuid,
            @Valid @RequestBody StaffAttendanceRequestDTO request) {

        Long callerUserId = resolveCallerUserId();
        return ResponseEntity.ok(service.updateAttendance(recordUuid, request, callerUserId));
    }

    @DeleteMapping("/{recordUuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete staff attendance record by UUID")
    public void delete(
                       @Parameter(description = "Staff attendance record UUID", schema = @Schema(format = "uuid"))
                       @PathVariable UUID recordUuid) {
        Long callerUserId = resolveCallerUserId();
        service.deleteAttendance(recordUuid, callerUserId);
    }

    /**
     * Resolves the authenticated caller's user ID from the JWT claims injected by JWTFilter
     * into {@code Authentication.getDetails()}. This is always present for authenticated
     * requests and cannot be spoofed via a client-controlled request header.
     *
     * @return the Long user ID of the caller, or {@code null} if the token does not carry the claim.
     */
    private Long resolveCallerUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getDetails() == null) {
            return null;
        }
        if (authentication.getDetails() instanceof Map<?, ?> details) {
            Object userId = details.get("user_id");
            if (userId instanceof Number number) {
                return number.longValue();
            }
            if (userId instanceof String raw && !raw.isBlank()) {
                try {
                    return Long.parseLong(raw);
                } catch (NumberFormatException ignored) {
                    log.warn("Invalid user_id claim in JWT details: {}", raw);
                }
            }
        }
        return null;
    }
}
