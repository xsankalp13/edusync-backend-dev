package com.project.edusync.adm.controller;

import com.project.edusync.adm.model.dto.PickupRequestGenerateDto;
import com.project.edusync.adm.model.dto.PickupRequestResponseDto;
import com.project.edusync.adm.model.dto.PickupVerifyDto;
import com.project.edusync.adm.service.PickupRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/adm/pickups")
@RequiredArgsConstructor
public class PickupRequestController {

    private final PickupRequestService pickupRequestService;

    @PostMapping("/generate")
    public ResponseEntity<PickupRequestResponseDto> generatePickup(@RequestBody PickupRequestGenerateDto dto) {
        return ResponseEntity.ok(pickupRequestService.generatePickup(dto));
    }

    @PostMapping("/verify")
    public ResponseEntity<PickupRequestResponseDto> verifyPickup(@RequestBody PickupVerifyDto dto) {
        return ResponseEntity.ok(pickupRequestService.verifyPickup(dto));
    }

    @GetMapping("/history")
    public ResponseEntity<List<PickupRequestResponseDto>> getMyHistory() {
        return ResponseEntity.ok(pickupRequestService.getMyGenerateHistory());
    }

    @GetMapping
    public ResponseEntity<List<PickupRequestResponseDto>> getAllPickups() {
        return ResponseEntity.ok(pickupRequestService.getAllPickups());
    }
}
