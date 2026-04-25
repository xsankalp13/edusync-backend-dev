package com.project.edusync.adm.controller;

import com.project.edusync.adm.service.AutoScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/sections")
@RequiredArgsConstructor
@Tag(name = "Timetable Auto-Generation", description = "Real-time timetable generation using Genetic Algorithm")
public class ScheduleSSEController {

    private final AutoScheduleService autoScheduleService;

    @GetMapping(value = "/{sectionId}/auto-generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Stream automated timetable generation",
            description = "Starts the Genetic Algorithm and streams the best candidate schedules via SSE.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public SseEmitter streamGeneration(@PathVariable UUID sectionId, HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache, no-transform");
        return autoScheduleService.generateTimetableStream(sectionId);
    }
}
