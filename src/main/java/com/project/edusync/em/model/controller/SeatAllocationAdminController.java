package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.service.SeatAllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/examination/seat-allocation")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SCHOOL_ADMIN','SUPER_ADMIN','EXAM_CONTROLLER')")
public class SeatAllocationAdminController {

    private final SeatAllocationService seatAllocationService;

}

