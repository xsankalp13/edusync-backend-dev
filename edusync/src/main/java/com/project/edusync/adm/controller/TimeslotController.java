package com.project.edusync.adm.controller;

import com.project.edusync.adm.model.dto.request.TimeslotRequestDto;
import com.project.edusync.adm.model.dto.response.TimeslotResponseDto;
import com.project.edusync.adm.service.TimeslotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing Timeslots.
 * All responses are wrapped in ResponseEntity for full control over the HTTP response.
 */
@RestController
@RequestMapping("${api.url}/auth") // Following your existing request mapping
@RequiredArgsConstructor
public class TimeslotController {

    private final TimeslotService timeslotService;

    /**
     * Creates a new timeslot (e.g., "Monday, 09:00-09:40").
     * HTTP 201 Created on success.
     */
    @PostMapping("/timeslots")
    public ResponseEntity<TimeslotResponseDto> createTimeslot(
            @Valid @RequestBody TimeslotRequestDto requestDto) {

        TimeslotResponseDto createdTimeslot = timeslotService.addTimeslot(requestDto);
        return new ResponseEntity<>(createdTimeslot, HttpStatus.CREATED);
    }

    /**
     * Retrieves all active timeslots.
     * Supports filtering by day of the week.
     * HTTP 200 OK on success.
     */
    @GetMapping("/timeslots")
    public ResponseEntity<List<TimeslotResponseDto>> getAllTimeslots(
            @RequestParam(value = "dayOfWeek", required = false) Short dayOfWeek) {

        List<TimeslotResponseDto> response = timeslotService.getAllTimeslots(dayOfWeek);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Retrieves details for a single timeslot by its UUID.
     * HTTP 200 OK on success.
     */
    @GetMapping("/timeslots/{timeslotId}")
    public ResponseEntity<TimeslotResponseDto> getTimeslotById(
            @PathVariable UUID timeslotId) {

        TimeslotResponseDto response = timeslotService.getTimeslotById(timeslotId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Updates a timeslot's details by its UUID.
     * HTTP 200 OK on success.
     */
    @PutMapping("/timeslots/{timeslotId}")
    public ResponseEntity<TimeslotResponseDto> updateTimeslotById(
            @PathVariable UUID timeslotId,
            @Valid @RequestBody TimeslotRequestDto timeslotRequestDto) {

        TimeslotResponseDto response = timeslotService.updateTimeslot(timeslotId, timeslotRequestDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Soft deletes a timeslot by its UUID.
     * HTTP 204 No Content on success.
     */
    @DeleteMapping("/timeslots/{timeslotId}")
    public ResponseEntity<Void> deleteTimeslotById(@PathVariable UUID timeslotId) {
        timeslotService.deleteTimeslot(timeslotId);
        return ResponseEntity.noContent().build();
    }
}