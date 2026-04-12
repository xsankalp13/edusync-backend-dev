package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.ResponseDTO.AdmitCardResponseDTO;
import com.project.edusync.em.model.service.AdmitCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student/admit-card")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_STUDENT')")
public class AdmitCardStudentController {

    private final AdmitCardService admitCardService;

    @GetMapping("/{examUuid}")
    public ResponseEntity<AdmitCardResponseDTO> getStudentAdmitCard(@PathVariable UUID examUuid) {
        return ResponseEntity.ok(admitCardService.getStudentAdmitCard(examUuid));
    }
}
