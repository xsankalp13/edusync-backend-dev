package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.RequestDTO.AdmitCardGenerateRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.AdmitCardGenerationProgressDTO;
import com.project.edusync.em.model.dto.ResponseDTO.AdmitCardGenerationResponseDTO;
import com.project.edusync.em.model.dto.ResponseDTO.ScheduleAdmitCardStatusDTO;
import com.project.edusync.em.model.service.AdmitCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/admit-cards")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SCHOOL_ADMIN','SUPER_ADMIN')")
public class AdmitCardAdminController {

    private final AdmitCardService admitCardService;

    @PostMapping("/generate")
    public ResponseEntity<AdmitCardGenerationResponseDTO> generate(@Valid @RequestBody AdmitCardGenerateRequestDTO requestDTO) {
        if (requestDTO.getScheduleId() != null) {
            return ResponseEntity.ok(admitCardService.generateAdmitCardsForSchedule(requestDTO.getExamId(), requestDTO.getScheduleId()));
        }
        return ResponseEntity.ok(admitCardService.generateAdmitCards(requestDTO.getExamId()));
    }

    @PostMapping("/generate/schedule/{scheduleId}")
    public ResponseEntity<AdmitCardGenerationResponseDTO> generateForSchedule(
            @Valid @RequestBody AdmitCardGenerateRequestDTO requestDTO,
            @PathVariable Long scheduleId) {
        return ResponseEntity.ok(admitCardService.generateAdmitCardsForSchedule(requestDTO.getExamId(), scheduleId));
    }

    @PostMapping("/generate-batch")
    public ResponseEntity<byte[]> generateBatch(@Valid @RequestBody AdmitCardGenerateRequestDTO requestDTO) {
        byte[] pdf = admitCardService.generateBatchAdmitCardsPdf(requestDTO.getExamId(), requestDTO.getScheduleId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=admit-cards-batch-" + requestDTO.getExamId() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/status/{examUuid}")
    public ResponseEntity<List<ScheduleAdmitCardStatusDTO>> getStatus(@PathVariable UUID examUuid) {
        return ResponseEntity.ok(admitCardService.getAdmitCardStatusByExam(examUuid));
    }

    @GetMapping("/status/{examUuid}/progress")
    public ResponseEntity<AdmitCardGenerationProgressDTO> getProgress(@PathVariable UUID examUuid) {
        return ResponseEntity.ok(admitCardService.getGenerationProgress(examUuid));
    }
}
