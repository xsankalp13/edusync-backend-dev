package com.project.edusync.teacher.controller;

import com.project.edusync.teacher.model.entity.Incident;
import com.project.edusync.teacher.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/incident")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping("/log")
    public ResponseEntity<Incident> logIncident(@RequestBody Incident incident) {
        return ResponseEntity.ok(incidentService.logIncident(incident));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Incident>> getIncidentsByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(incidentService.getIncidentsByStudent(studentId));
    }
}