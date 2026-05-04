package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.request.BulkSeatAllocationRequestDTO;
import com.project.edusync.em.model.dto.request.SingleSeatAllocationRequestDTO;
import com.project.edusync.em.model.dto.request.GlobalSeatAllocationRequestDTO;
import com.project.edusync.em.model.dto.response.GlobalSeatAllocationResultDTO;
import com.project.edusync.em.model.dto.response.RoomAvailabilityDTO;
import com.project.edusync.em.model.dto.response.SeatAllocationResponseDTO;
import com.project.edusync.em.model.dto.response.SeatAvailabilityDTO;
import com.project.edusync.em.model.service.SeatAllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/examination/seat-allocation")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SCHOOL_ADMIN','SUPER_ADMIN','EXAM_CONTROLLER')")
public class SeatAllocationController {

    private final SeatAllocationService seatAllocationService;

    @GetMapping("/rooms")
    @PreAuthorize("@examControllerAccess.canAccessSchedule(#examScheduleId)")
    public ResponseEntity<List<RoomAvailabilityDTO>> getAvailableRooms(
            @RequestParam Long examScheduleId) {
        return ResponseEntity.ok(seatAllocationService.getAvailableRooms(examScheduleId));
    }

    @GetMapping("/rooms/{roomUuid}/seats")
    @PreAuthorize("@examControllerAccess.canAccessSchedule(#examScheduleId)")
    public ResponseEntity<List<SeatAvailabilityDTO>> getSeatGrid(
            @PathVariable UUID roomUuid,
            @RequestParam Long examScheduleId) {
        return ResponseEntity.ok(seatAllocationService.getAvailableSeats(examScheduleId, roomUuid));
    }

    @PostMapping("/rooms/seats/bulk")
    @PreAuthorize("@examControllerAccess.canAccessSchedule(#examScheduleId)")
    public ResponseEntity<java.util.Map<UUID, List<SeatAvailabilityDTO>>> getBulkSeatGrids(
            @RequestParam Long examScheduleId,
            @RequestBody List<UUID> roomUuids) {
        return ResponseEntity.ok(seatAllocationService.getBulkAvailableSeats(examScheduleId, roomUuids));
    }

    @PostMapping("/allocate")
    @PreAuthorize("@examControllerAccess.canAccessSchedule(#dto.examScheduleId)")
    public ResponseEntity<SeatAllocationResponseDTO> allocateSingleSeat(
            @Validated @RequestBody SingleSeatAllocationRequestDTO dto) {
        return ResponseEntity.ok(seatAllocationService.allocateSingleSeat(dto));
    }

    @PostMapping("/auto-allocate")
    @PreAuthorize("@examControllerAccess.canAccessSchedule(#dto.examScheduleId)")
    public ResponseEntity<List<SeatAllocationResponseDTO>> autoAllocate(
            @Validated @RequestBody BulkSeatAllocationRequestDTO dto) {
        return ResponseEntity.ok(seatAllocationService.bulkAllocate(dto));
    }

    @GetMapping("/global-capacity-info")
    @PreAuthorize("@examControllerAccess.canAccessExamUuid(#examUuid)")
    public ResponseEntity<com.project.edusync.em.model.dto.response.GlobalCapacityInfoDTO> getGlobalCapacityInfo(
            @RequestParam UUID examUuid) {
        return ResponseEntity.ok(seatAllocationService.getGlobalCapacityInfo(examUuid));
    }

    @PostMapping("/global-allocate")
    @PreAuthorize("@examControllerAccess.canAccessExamUuid(#dto.examUuid)")
    public ResponseEntity<GlobalSeatAllocationResultDTO> globalAllocate(
            @Validated @RequestBody GlobalSeatAllocationRequestDTO dto) {
        return ResponseEntity.ok(seatAllocationService.globalAllocateForExam(dto));
    }

    @GetMapping("/schedule/{examScheduleId}")
    @PreAuthorize("@examControllerAccess.canAccessSchedule(#examScheduleId)")
    public ResponseEntity<List<SeatAllocationResponseDTO>> getAllocationsForSchedule(
            @PathVariable Long examScheduleId) {
        return ResponseEntity.ok(seatAllocationService.getAllocationsForSchedule(examScheduleId));
    }


    @GetMapping("/roll/{rollNo}")
    @PreAuthorize("@examControllerAccess.canAccessSchedule(#examScheduleId)")
    public ResponseEntity<SeatAllocationResponseDTO> findByRollNumber(
            @PathVariable Integer rollNo,
            @RequestParam Long examScheduleId) {
        return ResponseEntity.ok(seatAllocationService.findAllocationByRollNumber(examScheduleId, rollNo));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@examControllerAccess.canAccessSeatAllocation(#id)")
    public ResponseEntity<Void> deleteAllocation(@PathVariable Long id) {
        seatAllocationService.deleteAllocation(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/exam/{examUuid}")
    @PreAuthorize("@examControllerAccess.canAccessExamUuid(#examUuid)")
    public ResponseEntity<Void> clearAllocationsByExam(@PathVariable UUID examUuid) {
        seatAllocationService.clearAllocationsByExamUuid(examUuid);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/schedule/{examScheduleId}")
    @PreAuthorize("@examControllerAccess.canAccessSchedule(#examScheduleId)")
    public ResponseEntity<Void> clearAllocationsBySchedule(@PathVariable Long examScheduleId) {
        seatAllocationService.clearAllocationsByScheduleId(examScheduleId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    @PreAuthorize("@examControllerAccess.canAccessSeatAllocations(#ids)")
    public ResponseEntity<Void> bulkDeleteAllocations(@RequestBody List<Long> ids) {
        seatAllocationService.bulkDeleteAllocations(ids);
        return ResponseEntity.noContent().build();
    }

}
