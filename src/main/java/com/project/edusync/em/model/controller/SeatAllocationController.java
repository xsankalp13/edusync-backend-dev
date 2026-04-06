package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.request.BulkSeatAllocationRequestDTO;
import com.project.edusync.em.model.dto.request.SingleSeatAllocationRequestDTO;
import com.project.edusync.em.model.dto.response.RoomAvailabilityDTO;
import com.project.edusync.em.model.dto.response.SeatAllocationResponseDTO;
import com.project.edusync.em.model.dto.response.SeatAvailabilityDTO;
import com.project.edusync.em.model.service.SeatAllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/examination/seat-allocation")
@RequiredArgsConstructor
public class SeatAllocationController {

    private final SeatAllocationService seatAllocationService;

    @GetMapping("/rooms")
    public ResponseEntity<List<RoomAvailabilityDTO>> getAvailableRooms(
            @RequestParam Long examScheduleId) {
        return ResponseEntity.ok(seatAllocationService.getAvailableRooms(examScheduleId));
    }

    @GetMapping("/rooms/{roomUuid}/seats")
    public ResponseEntity<List<SeatAvailabilityDTO>> getSeatGrid(
            @PathVariable UUID roomUuid,
            @RequestParam Long examScheduleId) {
        return ResponseEntity.ok(seatAllocationService.getAvailableSeats(examScheduleId, roomUuid));
    }

    @PostMapping("/allocate")
    public ResponseEntity<SeatAllocationResponseDTO> allocateSingleSeat(
            @Validated @RequestBody SingleSeatAllocationRequestDTO dto) {
        return ResponseEntity.ok(seatAllocationService.allocateSingleSeat(dto));
    }

    @PostMapping("/auto-allocate")
    public ResponseEntity<List<SeatAllocationResponseDTO>> autoAllocate(
            @Validated @RequestBody BulkSeatAllocationRequestDTO dto) {
        return ResponseEntity.ok(seatAllocationService.bulkAllocate(dto));
    }

    @GetMapping("/schedule/{examScheduleId}")
    public ResponseEntity<List<SeatAllocationResponseDTO>> getAllocationsForSchedule(
            @PathVariable Long examScheduleId) {
        return ResponseEntity.ok(seatAllocationService.getAllocationsForSchedule(examScheduleId));
    }

    @GetMapping("/roll/{rollNo}")
    public ResponseEntity<SeatAllocationResponseDTO> findByRollNumber(
            @PathVariable Integer rollNo,
            @RequestParam Long examScheduleId) {
        return ResponseEntity.ok(seatAllocationService.findAllocationByRollNumber(examScheduleId, rollNo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAllocation(@PathVariable Long id) {
        seatAllocationService.deleteAllocation(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<Void> bulkDeleteAllocations(@RequestBody List<Long> ids) {
        seatAllocationService.bulkDeleteAllocations(ids);
        return ResponseEntity.noContent().build();
    }

}
