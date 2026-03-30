package com.project.edusync.adm.controller;

import com.project.edusync.adm.model.dto.request.ScheduleRequestDto;
import com.project.edusync.adm.model.dto.response.EditorContextResponseDto;
import com.project.edusync.adm.model.dto.response.ScheduleResponseDto;
import com.project.edusync.adm.model.dto.response.TimetableOverviewResponseDto;
import com.project.edusync.adm.service.EditorContextService;
import com.project.edusync.adm.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Timetable Management", description = "Manage section schedules and timetable editor data")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final EditorContextService editorContextService;

    /**
     * Fetches all existing Schedule entries for a specific section.
     * HTTP 200 OK on success.
     */
    @GetMapping("/sections/{sectionId}/schedule")
    @Operation(
            summary = "Get section schedule",
            description = "Returns all timetable entries for a section.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Schedule fetched successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Section not found")
    })
    public ResponseEntity<List<ScheduleResponseDto>> getScheduleForSection(
            @PathVariable UUID sectionId) {

        List<ScheduleResponseDto> response = scheduleService.getScheduleForSection(sectionId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    @GetMapping("/sections/{sectionId}/editor-context")
    @Operation(
            summary = "Get timetable editor context",
            description = "Returns section metadata, timeslots, curriculum-backed subjects, available teachers, and existing schedule in one call.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Editor context fetched successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Section not found")
    })
    public ResponseEntity<EditorContextResponseDto> getEditorContext(@PathVariable UUID sectionId) {
        EditorContextResponseDto response = editorContextService.getEditorContext(sectionId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    @GetMapping("/schedules/overview")
    @Operation(
            summary = "Get timetable overview",
            description = "Returns an institution-wide timetable health overview including coverage and status per section.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Timetable overview fetched successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT")
    })
    public ResponseEntity<List<TimetableOverviewResponseDto>> getScheduleOverview() {
        List<TimetableOverviewResponseDto> response = scheduleService.getScheduleOverview();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    /**
     * Creates a new Schedule entry.
     * HTTP 201 Created on success.
     * HTTP 409 Conflict if there is a double booking.
     */
    @PostMapping("/schedules")
    @Operation(
            summary = "Create schedule entry",
            description = "Creates a single timetable entry for a section, subject, teacher, and timeslot.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Schedule entry created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "409", description = "Scheduling conflict detected")
    })
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
    @Operation(
            summary = "Update schedule entry",
            description = "Updates a timetable entry by schedule UUID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Schedule entry updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Schedule entry not found"),
            @ApiResponse(responseCode = "409", description = "Scheduling conflict detected")
    })
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
    @Operation(
            summary = "Delete schedule entry",
            description = "Soft deletes a timetable entry by schedule UUID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Schedule entry deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Schedule entry not found")
    })
    public ResponseEntity<Void> deleteScheduleById(@PathVariable UUID scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.noContent().build();
    }
 
    @DeleteMapping("/sections/{sectionId}/schedule")
    @Operation(
            summary = "Delete complete section schedule",
            description = "Soft deletes all active timetable entries for the specified section ID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Complete section schedule deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Section not found")
    })
    public ResponseEntity<Void> deleteScheduleBySection(@PathVariable UUID sectionId) {
        scheduleService.deleteScheduleBySection(sectionId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/schedules/{sectionId}/status/{statusType}")
    @Operation(
            summary = "Update section timetable status",
            description = "Marks a section timetable as draft or published based on status type.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Timetable status updated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Section not found")
    })
    public ResponseEntity<String> draftSchedule(@PathVariable UUID sectionId, @PathVariable String statusType) {
        scheduleService.saveAsDraft(sectionId,statusType);
        return new ResponseEntity<>("Saved as draft",HttpStatus.OK);
    }

    @PutMapping("/sections/{sectionId}/schedule/bulk")
    @Operation(
            summary = "Bulk replace section schedule",
            description = "Atomically replaces the entire schedule for a section in one transaction.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Section schedule replaced successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Section not found"),
            @ApiResponse(responseCode = "409", description = "Scheduling conflict detected")
    })
    public ResponseEntity<List<ScheduleResponseDto>> bulkReplaceSectionSchedule(
            @PathVariable UUID sectionId,
            @Valid @RequestBody List<ScheduleRequestDto> requestDtos) {
        List<ScheduleResponseDto> response = scheduleService.replaceSectionScheduleBulk(sectionId, requestDtos);
        return ResponseEntity.ok(response);
    }
}