package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.calendar.BulkOperationResultDTO;
import com.project.edusync.hrms.dto.calendar.CalendarEventCreateDTO;
import com.project.edusync.hrms.dto.calendar.CalendarEventResponseDTO;
import com.project.edusync.hrms.dto.calendar.CalendarEventUpdateDTO;
import com.project.edusync.hrms.dto.calendar.CalendarSummaryDTO;
import com.project.edusync.hrms.service.AcademicCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/hrms/calendar")
@RequiredArgsConstructor
@Tag(name = "HRMS Calendar", description = "Academic and leave calendar management APIs")
public class AcademicCalendarController {

    private final AcademicCalendarService academicCalendarService;

    @GetMapping("/events")
    @Operation(summary = "List calendar events", description = "Returns all active calendar events, optionally filtered by academicYear and month")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<List<CalendarEventResponseDTO>> listEvents(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer month
    ) {
        return ResponseEntity.ok(academicCalendarService.listEvents(academicYear, month));
    }

    @PostMapping("/events")
    @Operation(summary = "Create calendar event")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<CalendarEventResponseDTO> createEvent(@Valid @RequestBody CalendarEventCreateDTO dto) {
        return new ResponseEntity<>(academicCalendarService.createEvent(dto), HttpStatus.CREATED);
    }

    @PostMapping("/events/bulk")
    @Operation(summary = "Bulk create calendar events")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<BulkOperationResultDTO> bulkCreateEvents(@Valid @RequestBody @NotEmpty List<CalendarEventCreateDTO> dtos) {
        return new ResponseEntity<>(academicCalendarService.bulkCreateEvents(dtos), HttpStatus.CREATED);
    }

    @PutMapping("/events/{identifier}")
    @Operation(summary = "Update calendar event")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<CalendarEventResponseDTO> updateEvent(
            @PathVariable String identifier,
            @Valid @RequestBody CalendarEventUpdateDTO dto
    ) {
        return ResponseEntity.ok(academicCalendarService.updateEventByIdentifier(identifier, dto));
    }

    @DeleteMapping("/events/{identifier}")
    @Operation(summary = "Delete calendar event")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<Void> deleteEvent(@PathVariable String identifier) {
        academicCalendarService.deleteEventByIdentifier(identifier);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    @Operation(summary = "Get academic year summary", description = "Returns monthly working day/holiday/half-day aggregates")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<CalendarSummaryDTO> summary(@RequestParam String academicYear) {
        return ResponseEntity.ok(academicCalendarService.getSummary(academicYear));
    }
}
