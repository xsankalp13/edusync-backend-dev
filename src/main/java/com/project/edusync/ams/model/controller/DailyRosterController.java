package com.project.edusync.ams.model.controller;

import com.project.edusync.ams.model.service.DailyRosterService;
import com.project.edusync.ams.model.dto.response.DailyRosterResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v2/ams/roster")
@RequiredArgsConstructor
public class DailyRosterController {

    private final DailyRosterService dailyRosterService;

    @GetMapping("/{classId}")
    public ResponseEntity<List<DailyRosterResponseDTO>> getDailyRoster(@PathVariable Long classId, @RequestParam LocalDate date) {
        List<DailyRosterResponseDTO> roster = dailyRosterService.getDailyRoster(classId, date);
        return ResponseEntity.ok(roster);
    }
}