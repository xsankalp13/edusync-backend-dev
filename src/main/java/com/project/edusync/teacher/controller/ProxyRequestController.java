package com.project.edusync.teacher.controller;

import com.project.edusync.teacher.model.entity.ProxyRequest;
import com.project.edusync.teacher.service.ProxyRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/proxy-request")
@RequiredArgsConstructor
public class ProxyRequestController {

    private final ProxyRequestService proxyRequestService;

    @GetMapping
    public ResponseEntity<List<ProxyRequest>> getProxyRequests(@RequestParam Long teacherId) {
        return ResponseEntity.ok(proxyRequestService.getProxyRequests(teacherId));
    }

    @PostMapping("/accept/{requestId}")
    public ResponseEntity<ProxyRequest> acceptProxyRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(proxyRequestService.acceptProxyRequest(requestId));
    }
}