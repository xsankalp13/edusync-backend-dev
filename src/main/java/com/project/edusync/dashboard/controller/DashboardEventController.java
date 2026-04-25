package com.project.edusync.dashboard.controller;

import com.project.edusync.dashboard.dto.DashboardEventDTO;
import com.project.edusync.dashboard.service.DashboardEventService;
import com.project.edusync.iam.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/dashboard")
@RequiredArgsConstructor
public class DashboardEventController {

    private final DashboardEventService service;

    @GetMapping(value = "/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@AuthenticationPrincipal User user, HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache, no-transform");
        return service.register();
    }

    @GetMapping("/events")
    public ResponseEntity<Page<DashboardEventDTO>> getEvents(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Instant since,
            @RequestParam(required = false) String type) {
            
        return ResponseEntity.ok(service.getEvents(page, size, since, type));
    }

    @PatchMapping("/events/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal User user,
            @RequestBody List<UUID> eventIds) {
            
        service.markAsRead(eventIds);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/events/unread-count")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(@AuthenticationPrincipal User user) {
        int count = service.getUnreadCount();
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
}
