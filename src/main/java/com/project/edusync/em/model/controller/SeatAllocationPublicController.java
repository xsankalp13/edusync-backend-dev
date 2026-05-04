package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.service.SeatAllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${api.url}/public/examination/seat-allocation")
@RequiredArgsConstructor
public class SeatAllocationPublicController {

    private final SeatAllocationService seatAllocationService;

    @GetMapping("/schedule/{examScheduleId}/print")
    public ResponseEntity<byte[]> printAllocationsForSchedule(
            @PathVariable Long examScheduleId,
            @RequestParam(name = "format", required = false, defaultValue = "ROOM_WISE") String format) {
        byte[] pdf = seatAllocationService.generateSeatingPlanPdf(examScheduleId, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=seating-plan-" + examScheduleId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/global-print/{examUuid}")
    public ResponseEntity<byte[]> globalPrintAllocations(@PathVariable UUID examUuid) {
        byte[] pdf = seatAllocationService.generateGlobalSeatingPlanPdf(examUuid);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=global-seating-plan-" + examUuid + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
