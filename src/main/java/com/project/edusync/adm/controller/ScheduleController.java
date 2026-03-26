package com.project.edusync.adm.controller;

import com.project.edusync.adm.model.dto.request.ScheduleRequestDto;
import com.project.edusync.adm.model.dto.response.ScheduleResponseDto;
import com.project.edusync.adm.model.dto.response.TimetableOverviewResponseDto;
import com.project.edusync.adm.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * REST Controller for managing the Timetable Schedule.
 */
@RestController
@RequestMapping("${api.url}/auth") // Following your existing request mapping
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * Fetches all existing Schedule entries for a specific section.
     * HTTP 200 OK on success.
     */
    @GetMapping("/sections/{sectionId}/schedule")
    public ResponseEntity<List<ScheduleResponseDto>> getScheduleForSection(
            @PathVariable UUID sectionId) {

        List<ScheduleResponseDto> response = scheduleService.getScheduleForSection(sectionId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(3600, TimeUnit.SECONDS).cachePublic())
                .body(response);
    }

    @GetMapping("/schedules/overview")
    public ResponseEntity<List<TimetableOverviewResponseDto>> getScheduleOverview() {
        List<TimetableOverviewResponseDto> response = scheduleService.getScheduleOverview();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(3600, TimeUnit.SECONDS).cachePublic())
                .body(response);
    }

    /**
     * Creates a new Schedule entry.
     * HTTP 201 Created on success.
     * HTTP 409 Conflict if there is a double booking.
     */
    @PostMapping("/schedules")
    public ResponseEntity<ScheduleResponseDto> createSchedule(
            @Valid @RequestBody ScheduleRequestDto requestDto) {

        ScheduleResponseDto createdSchedule = scheduleService.addSchedule(requestDto);
        return new ResponseEntity<>(createdSchedule, HttpStatus.CREATED);
    }

    /**
     * Updates an existing Schedule entry by its UUID.
     * HTTP 200 OK on success.
     * HTTP 409 Conflict if there is a double booking.
     */
    @PutMapping("/schedules/{scheduleId}")
    public ResponseEntity<ScheduleResponseDto> updateScheduleById(
            @PathVariable UUID scheduleId,
            @Valid @RequestBody ScheduleRequestDto scheduleRequestDto) {

        ScheduleResponseDto response = scheduleService.updateSchedule(scheduleId, scheduleRequestDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Soft deletes a Schedule entry by its UUID.
     * HTTP 204 No Content on success.
     */
    @DeleteMapping("/schedules/{scheduleId}")
    public ResponseEntity<Void> deleteScheduleById(@PathVariable UUID scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/schedules/{sectionId}/status/{statusType}")
    public ResponseEntity<String> draftSchedule(@PathVariable UUID sectionId, @PathVariable String statusType) {
        scheduleService.saveAsDraft(sectionId,statusType);
        return new ResponseEntity<>("Saved as draft",HttpStatus.OK);
    }

    @PutMapping("/sections/{sectionId}/schedule/bulk")
    public ResponseEntity<List<ScheduleResponseDto>> bulkReplaceSectionSchedule(
            @PathVariable UUID sectionId,
            @Valid @RequestBody List<ScheduleRequestDto> requestDtos) {
        List<ScheduleResponseDto> response = scheduleService.replaceSectionScheduleBulk(sectionId, requestDtos);
        return ResponseEntity.ok(response);
    }
}