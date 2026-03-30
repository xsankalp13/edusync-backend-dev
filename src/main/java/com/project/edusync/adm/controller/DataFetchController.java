package com.project.edusync.adm.controller;

import com.project.edusync.adm.model.dto.response.AvailableRoomDto;
import com.project.edusync.adm.model.dto.response.AvailableSubjectDto;
import com.project.edusync.adm.model.dto.response.AvailableTeacherDto;
import com.project.edusync.adm.service.DataFetchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for fetching supporting data for UI dropdowns.
 */
@RestController
@RequestMapping("${api.url}/auth/data-fetch") // Grouped under a new path
@RequiredArgsConstructor
public class DataFetchController {

    private final DataFetchService dataFetchService;

    /**
     * Finds teachers who are qualified for a subject.
     */
    @GetMapping("/teachers/available")
    public ResponseEntity<List<AvailableTeacherDto>> getAvailableTeachers(
            @RequestParam UUID subjectId,
            @RequestParam(required = false) UUID timeslotId) {

        List<AvailableTeacherDto> response = dataFetchService.getAvailableTeachers(subjectId, timeslotId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Finds rooms not scheduled at a timeslot, with optional type filtering.
     */
    @GetMapping("/rooms/available")
    public ResponseEntity<List<AvailableRoomDto>> getAvailableRooms(
            @RequestParam UUID timeslotId,
            @RequestParam(required = false) String roomType) {

        List<AvailableRoomDto> response = dataFetchService.getAvailableRooms(timeslotId, roomType);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Finds the subjects that are assigned to a specific section's class.
     */
    @GetMapping("/subjects/available")
    public ResponseEntity<List<AvailableSubjectDto>> getAvailableSubjects(
            @RequestParam UUID sectionId) {

        List<AvailableSubjectDto> response = dataFetchService.getAvailableSubjects(sectionId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}