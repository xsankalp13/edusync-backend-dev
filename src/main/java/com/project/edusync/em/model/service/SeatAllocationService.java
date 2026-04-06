package com.project.edusync.em.model.service;

import com.project.edusync.adm.exception.ResourceNotFoundException;
import com.project.edusync.adm.model.entity.Room;
import com.project.edusync.adm.repository.RoomRepository;
import com.project.edusync.common.exception.BadRequestException;
import com.project.edusync.em.model.dto.request.BulkSeatAllocationRequestDTO;
import com.project.edusync.em.model.dto.request.SingleSeatAllocationRequestDTO;
import com.project.edusync.em.model.dto.response.RoomAvailabilityDTO;
import com.project.edusync.em.model.dto.response.SeatAllocationResponseDTO;
import com.project.edusync.em.model.dto.response.SeatAvailabilityDTO;
import com.project.edusync.em.model.entity.ExamSchedule;
import com.project.edusync.em.model.entity.Seat;
import com.project.edusync.em.model.entity.SeatAllocation;
import com.project.edusync.em.model.enums.SeatPosition;
import com.project.edusync.em.model.enums.SeatSide;
import com.project.edusync.em.model.repository.ExamScheduleRepository;
import com.project.edusync.em.model.repository.SeatAllocationRepository;
import com.project.edusync.em.model.repository.SeatRepository;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatAllocationService {

    private final SeatRepository seatRepository;
    private final SeatAllocationRepository allocationRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;

    private static final int BATCH_SIZE = 50;

    // ════════════════════════════════════════════════════════════════
    // SEAT GENERATION (called on room create/update)
    // ════════════════════════════════════════════════════════════════

    @Transactional
    public void generateSeatsForRoom(Room room) {
        if (room.getRowCount() == null || room.getColumnsPerRow() == null) {
            log.info("Skipping seat generation for room {}: dimensions not set", room.getUuid());
            return;
        }

        // Prevent deletion if allocations exist
        if (seatRepository.existsAllocationsByRoomId(room.getId())) {
            throw new BadRequestException("Cannot regenerate seats: active allocations exist for this room");
        }

        seatRepository.deleteAllByRoomId(room.getId());
        seatRepository.flush();

        List<Seat> seats = new ArrayList<>();
        for (int r = 1; r <= room.getRowCount(); r++) {
            for (int c = 1; c <= room.getColumnsPerRow(); c++) {
                Seat seat = new Seat();
                seat.setRoom(room);
                seat.setRowNumber(r);
                seat.setColumnNumber(c);
                seat.setLabel("R" + r + "-C" + c);
                seats.add(seat);
            }
        }
        // Batched insert
        log.info("Generating {} seats for room {}", seats.size(), room.getUuid());
        for (int i = 0; i < seats.size(); i += BATCH_SIZE) {
            seatRepository.saveAll(seats.subList(i, Math.min(i + BATCH_SIZE, seats.size())));
            seatRepository.flush();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // GET AVAILABLE ROOMS (with capacity-aware + mixed seating logic)
    //
    // Capacity formula:
    //   If current exam is SINGLE seating (maxPerSeat=1):
    //     availableCapacity = totalSeats - allOccupiedSeatCount
    //   If current exam is DOUBLE seating (maxPerSeat=2):
    //     blockedBySSCount = seats locked by single-seating exams
    //     effectiveSeats = totalSeats - blockedBySSCount
    //     effectiveCapacity = effectiveSeats × maxPerSeat
    //     nonBlockedOccupancy = totalAllocations - allocationsOnBlockedSeats
    //     availableCapacity = effectiveCapacity - nonBlockedOccupancy
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<RoomAvailabilityDTO> getAvailableRooms(Long examScheduleId) {
        ExamSchedule schedule = fetchSchedule(examScheduleId);
        LocalDateTime start = deriveStartTime(schedule);
        LocalDateTime end = deriveEndTime(schedule);
        int maxPerSeat = schedule.getMaxStudentsPerSeat();
        SeatPosition schedulePosition = resolveSchedulePosition(schedule);

        // 1. Count total students needing seats
        int totalStudents = countStudentsForSchedule(schedule);

        // 2. All active rooms (SINGLE query)
        List<Room> rooms = roomRepository.findAllActive();

        // 3. Seat counts per room (SINGLE query via batch)
        Map<Long, Integer> examSeatUnitsMap = rooms.stream()
            .collect(Collectors.toMap(Room::getId, r -> Optional.ofNullable(r.getExamSeatUnits()).orElse(0)));

        // 4. For double-seating exams: count seats blocked by SINGLE allocations or same side usage
        Map<Long, Long> blockedSeatCountMap = new HashMap<>();

        if (maxPerSeat > 1) {
            allocationRepository.countBlockedSeatsPerRoomForPosition(start, end, schedulePosition)
                .forEach(row -> blockedSeatCountMap.put((Long) row[0], (Long) row[1]));
        }

        // Room Occupancy details to determine mode and occupiedBy array
        List<Object[]> roomOccupancyRows = allocationRepository.findRoomOccupancyDetails(start, end);
        Map<Long, List<Object[]>> occupancyDetailsByRoom = roomOccupancyRows.stream()
            .collect(Collectors.groupingBy(row -> ((Long) row[0])));

        // 6. Build response with capacity-aware math
        return rooms.stream()
            .map(room -> {
                int totalSeats = examSeatUnitsMap.getOrDefault(room.getId(), 0);

                int totalCapacity;
                int occupiedCapacity;
                int availableCapacity;

                if (maxPerSeat == 1) {
                    totalCapacity = totalSeats;
                    Set<Long> occupiedSeatIds = allocationRepository.findOccupiedSeatIdsInRoom(room.getId(), start, end);
                    occupiedCapacity = occupiedSeatIds.size();
                    availableCapacity = totalCapacity - occupiedCapacity;
                } else {
                    long blockedSeats = blockedSeatCountMap.getOrDefault(room.getId(), 0L);
                    totalCapacity = totalSeats;
                    occupiedCapacity = (int) blockedSeats;
                    availableCapacity = totalCapacity - occupiedCapacity;
                }

                List<Object[]> occupancyRows = occupancyDetailsByRoom.getOrDefault(room.getId(), Collections.emptyList());
                boolean hasSingleExam = occupancyRows.stream().anyMatch(row -> ((Integer) row[1]) == 1);
                
                String mode;
                if (hasSingleExam) {
                    mode = "SINGLE";
                } else if (occupancyRows.isEmpty()) {
                    mode = "DOUBLE"; // Empty but supports double
                } else {
                    mode = "SHARED"; // Occupied by multiple DOUBLE exams
                }

                List<com.project.edusync.em.model.dto.response.OccupiedByDTO> occupiedBy = occupancyRows.stream()
                    .map(row -> new com.project.edusync.em.model.dto.response.OccupiedByDTO(
                            (String) row[2], // subjectName
                            (String) row[3], // className
                            ((Long) row[4]).intValue() // count
                    )).collect(Collectors.toList());

                return RoomAvailabilityDTO.builder()
                    .roomId(room.getId())
                    .roomUuid(room.getUuid())
                    .roomName(room.getName())
                    .totalSeats(totalSeats)
                    .totalCapacity(totalCapacity)
                    .occupiedCapacity(occupiedCapacity)
                    .availableCapacity(Math.max(0, availableCapacity))
                    .isFull(availableCapacity <= 0)
                    .maxStudentsPerSeat(maxPerSeat)
                    .totalStudentsToSeat(totalStudents)
                    .floorNumber(room.getFloorNumber())
                    .mode(mode)
                    .occupiedBy(occupiedBy)
                    .build();
            })
            .sorted(Comparator.comparingInt(RoomAvailabilityDTO::getAvailableCapacity).reversed())
            .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════
    // GET AVAILABLE SEATS IN A ROOM (for grid visualization)
    // Uses bulk GROUP BY query + single-seating block detection
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<SeatAvailabilityDTO> getAvailableSeats(Long examScheduleId, UUID roomUuid) {
        ExamSchedule schedule = fetchSchedule(examScheduleId);
        Room room = roomRepository.findActiveById(roomUuid)
            .orElseThrow(() -> new ResourceNotFoundException("No resource found with id: " + roomUuid));

        LocalDateTime start = deriveStartTime(schedule);
        LocalDateTime end = deriveEndTime(schedule);
        int maxPerSeat = schedule.getMaxStudentsPerSeat();
        SeatPosition schedulePosition = resolveSchedulePosition(schedule);

        // All seats for room (SINGLE query)
        List<Seat> seats = seatRepository.findByRoomIdOrderByRowNumberAscColumnNumberAsc(room.getId());

        Map<Long, Long> seatOccupancy = new HashMap<>();
        if (maxPerSeat == 1) {
            allocationRepository.countAllocationsPerSeatInRoom(room.getId(), start, end)
                .forEach(row -> seatOccupancy.put((Long) row[0], (Long) row[1]));
        } else {
            allocationRepository.countAllocationsPerSeatInRoomByPosition(room.getId(), start, end, schedulePosition)
                .forEach(row -> seatOccupancy.put((Long) row[0], (Long) row[1]));
        }

        Set<Long> blockedForPosition = maxPerSeat == 1
            ? allocationRepository.findOccupiedSeatIdsInRoom(room.getId(), start, end)
            : allocationRepository.findSeatIdsBlockedForPosition(room.getId(), start, end, schedulePosition);

        Map<Long, List<String>> occupiedPositionsMap = new HashMap<>();
        allocationRepository.findOccupiedPositionsPerSeatInRoom(room.getId(), start, end)
            .forEach(row -> occupiedPositionsMap
                .computeIfAbsent((Long) row[0], k -> new ArrayList<>())
                .add(((SeatPosition) row[1]).name()));

        return seats.stream()
            .map(s -> {
                int occupied = seatOccupancy.getOrDefault(s.getId(), 0L).intValue();
                boolean isFull = blockedForPosition.contains(s.getId());
                int effectiveCapacity = 1;

                int availableSlots = isFull ? 0 : (effectiveCapacity - occupied);

                return SeatAvailabilityDTO.builder()
                    .seatId(s.getId())
                    .label(s.getLabel())
                    .rowNumber(s.getRowNumber())
                    .columnNumber(s.getColumnNumber())
                    .capacity(effectiveCapacity)
                    .occupiedCount(occupied)
                    .availableSlots(Math.max(0, availableSlots))
                    .isFull(isFull)
                    .available(!isFull) // backward compat
                    .occupiedPositions(occupiedPositionsMap.getOrDefault(s.getId(), Collections.emptyList()))
                    .build();
            })
            .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════
    // SINGLE STUDENT ALLOCATION (manual assignment)
    // Uses GROUP BY occupancy check + single-seating block validation
    // ════════════════════════════════════════════════════════════════

    @Transactional
    public SeatAllocationResponseDTO allocateSingleSeat(SingleSeatAllocationRequestDTO dto) {
        ExamSchedule schedule = fetchSchedule(dto.getExamScheduleId());
        LocalDateTime start = deriveStartTime(schedule);
        LocalDateTime end = deriveEndTime(schedule);
        int maxPerSeat = schedule.getMaxStudentsPerSeat();
        SeatPosition schedulePosition = resolveSchedulePosition(schedule);

        Student student = studentRepository.findByUuid(dto.getStudentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + dto.getStudentId()));
        Room room = roomRepository.findActiveById(dto.getRoomId())
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + dto.getRoomId()));
        Seat seat = seatRepository.findById(dto.getSeatId())
            .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + dto.getSeatId()));

        // Validate seat belongs to room
        if (!seat.getRoom().getId().equals(room.getId())) {
            throw new BadRequestException("Seat does not belong to selected room");
        }

        // Check student not already allocated in this time window
        if (allocationRepository.isStudentAllocatedInTimeWindow(student.getId(), start, end)) {
            throw new BadRequestException("Student already has a seat allocation in this time window");
        }

        if (maxPerSeat == 1) {
            Set<Long> occupiedSeatIds = allocationRepository.findOccupiedSeatIdsInRoom(room.getId(), start, end);
            if (occupiedSeatIds.contains(seat.getId())) {
                throw new BadRequestException("Seat is already occupied in this time window. Cannot assign.");
            }
        } else {
            Set<Long> blockedSeats = allocationRepository
                .findSeatIdsBlockedForPosition(room.getId(), start, end, schedulePosition);
            if (blockedSeats.contains(seat.getId())) {
                throw new BadRequestException(
                    "Seat side is already occupied for this time window. Cannot assign.");
            }
        }

        SeatAllocation allocation = new SeatAllocation();
        allocation.setSeat(seat);
        allocation.setStudent(student);
        allocation.setExamSchedule(schedule);
        allocation.setStartTime(start);
        allocation.setEndTime(end);
        allocation.setPosition(schedulePosition);

        return toResponse(allocationRepository.save(allocation));
    }

    // ════════════════════════════════════════════════════════════════
    // BULK AUTO-ALLOCATION (concurrency-safe with pessimistic lock)
    //
    // Algorithm:
    //   1. Lock ALL seats in room (unfiltered pessimistic write lock)
    //   2. Get occupancy map via GROUP BY (no subquery)
    //   3. Get single-seating blocked seat IDs
    //   4. Filter out blocked seats from candidate pool
    //   5. Compute totalAvailableCapacity, validate >= studentsToAllocate
    //   6. Sort: partially filled first (desc occupancy), then empty
    //   7. Iterate & fill up to maxPerSeat per seat
    //   8. Batched insert
    // ════════════════════════════════════════════════════════════════

    @Transactional
    public List<SeatAllocationResponseDTO> bulkAllocate(BulkSeatAllocationRequestDTO dto) {
        ExamSchedule schedule = fetchSchedule(dto.getExamScheduleId());
        Room room = roomRepository.findActiveById(dto.getRoomId())
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + dto.getRoomId()));

        LocalDateTime start = deriveStartTime(schedule);
        LocalDateTime end = deriveEndTime(schedule);
        int maxPerSeat = schedule.getMaxStudentsPerSeat();
        SeatPosition schedulePosition = resolveSchedulePosition(schedule);

        // 1. Resolve all students for this schedule's class/section
        List<Student> allStudents = resolveStudents(schedule);
        if (allStudents.isEmpty()) {
            throw new BadRequestException("No students found for this schedule");
        }

        // 2. BULK check: which students already have allocations (SINGLE query)
        Set<Long> allStudentIds = allStudents.stream().map(Student::getId).collect(Collectors.toSet());
        Set<Long> alreadyAllocated = allocationRepository.findAlreadyAllocatedStudentIds(allStudentIds, start, end);

        List<Student> unallocated = allStudents.stream()
            .filter(s -> !alreadyAllocated.contains(s.getId()))
            .collect(Collectors.toList());

        if (unallocated.isEmpty()) {
            throw new BadRequestException("All students already have seat allocations");
        }

        // 3. PESSIMISTIC LOCK: lock candidate seats in room
        List<Seat> allSeats = maxPerSeat == 1
            ? allocationRepository.lockAllSeatsInRoom(room.getId())
            : allocationRepository.lockAvailableSeatsInRoom(room.getId(), start, end, schedulePosition);

        if (allSeats.isEmpty()) {
            throw new BadRequestException("No configured seats available for sharing in this room");
        }

        // 4. Get per-seat occupancy via GROUP BY (SINGLE query, no subquery)
        Map<Long, Long> seatOccupancy = new HashMap<>();
        if (maxPerSeat == 1) {
            allocationRepository.countAllocationsPerSeatInRoom(room.getId(), start, end)
                .forEach(row -> seatOccupancy.put((Long) row[0], (Long) row[1]));
        } else {
            allocationRepository.countAllocationsPerSeatInRoomByPosition(room.getId(), start, end, schedulePosition)
                .forEach(row -> seatOccupancy.put((Long) row[0], (Long) row[1]));
        }

        List<Seat> availableSeats = allSeats.stream()
            .filter(s -> seatOccupancy.getOrDefault(s.getId(), 0L) < 1)
            .collect(Collectors.toList());

        // 6. CAPACITY VALIDATION: compute total available capacity
        //    Seats returned by lockAvailableSeatsInRoom are inherently free of single-seating/same-subject conflicts
        long totalAvailableCapacity = availableSeats.size();

        if (totalAvailableCapacity <= 0) {
            throw new BadRequestException("No available capacity in this room");
        }

        int toAllocate = Math.min(unallocated.size(), (int) totalAvailableCapacity);
        log.info("Allocating {} students to room {} (capacity: {}, maxPerSeat: {}) for Schedule ID {}",
            toAllocate, room.getUuid(), totalAvailableCapacity, maxPerSeat, schedule.getId());

        // 7. SORT: partially filled seats first (desc occupancy), then empty seats
        List<Seat> sortedSeats = availableSeats.stream()
            .sorted(Comparator
                // Partially filled first (higher occupancy = higher priority)
                .comparingLong((Seat s) -> seatOccupancy.getOrDefault(s.getId(), 0L)).reversed()
                // Then by position for consistent ordering
                .thenComparingInt(Seat::getRowNumber)
                .thenComparingInt(Seat::getColumnNumber))
            .collect(Collectors.toList());

        // 8. Build allocations — one allocation per seat for this schedule
        List<SeatAllocation> newAllocations = new ArrayList<>(toAllocate);
        int studentIdx = 0;

        for (Seat seat : sortedSeats) {
            if (studentIdx >= toAllocate) break;

            SeatAllocation sa = new SeatAllocation();
            sa.setSeat(seat);
            sa.setStudent(unallocated.get(studentIdx));
            sa.setExamSchedule(schedule);
            sa.setStartTime(start);
            sa.setEndTime(end);
            sa.setPosition(schedulePosition);

            newAllocations.add(sa);
            studentIdx++;
        }

        // 9. BATCHED insert
        List<SeatAllocation> saved = new ArrayList<>(newAllocations.size());
        for (int i = 0; i < newAllocations.size(); i += BATCH_SIZE) {
            List<SeatAllocation> batch = newAllocations.subList(i, Math.min(i + BATCH_SIZE, newAllocations.size()));
            saved.addAll(allocationRepository.saveAll(batch));
            allocationRepository.flush();
        }

        return saved.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════
    // GET ALLOCATIONS FOR A SCHEDULE
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<SeatAllocationResponseDTO> getAllocationsForSchedule(Long examScheduleId) {
        return allocationRepository.findByExamScheduleWithDetails(examScheduleId)
            .stream()
            .map(this::toResponse)
            .sorted(Comparator.comparing(SeatAllocationResponseDTO::getRollNo, Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SeatAllocationResponseDTO findAllocationByRollNumber(Long examScheduleId, Integer rollNo) {
        Student student = studentRepository.findByRollNo(rollNo)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with roll number: " + rollNo));
            
        SeatAllocation allocation = allocationRepository
            .findByExamScheduleIdAndStudentId(examScheduleId, student.getId())
            .orElseThrow(() -> new ResourceNotFoundException("No seat allocation found for roll number " + rollNo + " in schedule " + examScheduleId));
            
        return toResponse(allocation);
    }

    // ════════════════════════════════════════════════════════════════
    // DELETION (Singular & Bulk)
    // ════════════════════════════════════════════════════════════════

    @Transactional
    public void deleteAllocation(Long allocationId) {
        if (!allocationRepository.existsById(allocationId)) {
            throw new ResourceNotFoundException("SeatAllocation not found with id: " + allocationId);
        }
        allocationRepository.deleteById(allocationId);
    }

    @Transactional
    public void bulkDeleteAllocations(List<Long> allocationIds) {
        if (allocationIds == null || allocationIds.isEmpty()) return;
        allocationRepository.deleteAllByIdInBatch(allocationIds);
    }

    // ── Private helpers ──────────────────────────────────────────

    private SeatPosition resolveSchedulePosition(ExamSchedule schedule) {
        if (schedule.getMaxStudentsPerSeat() == 1) {
            return SeatPosition.SINGLE;
        }
        SeatSide side = schedule.getSeatSide();
        if (side == null) {
            throw new BadRequestException("seatSide is required for DOUBLE seating schedules");
        }
        return side == SeatSide.LEFT ? SeatPosition.LEFT : SeatPosition.RIGHT;
    }

    private ExamSchedule fetchSchedule(Long id) {
        return examScheduleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ExamSchedule not found with id: " + id));
    }

    private LocalDateTime deriveStartTime(ExamSchedule s) {
        log.debug("[deriveStartTime] ExamSchedule ID: {} examDate: {} timeslot.startTime: {} -> startTime: {}",
            s.getId(), s.getExamDate(), s.getTimeslot().getStartTime(),
            s.getExamDate().atTime(s.getTimeslot().getStartTime()));
        return s.getExamDate().atTime(s.getTimeslot().getStartTime());
    }

    private LocalDateTime deriveEndTime(ExamSchedule s) {
        log.debug("[deriveEndTime] ExamSchedule ID: {} examDate: {} timeslot.endTime: {} -> endTime: {}",
            s.getId(), s.getExamDate(), s.getTimeslot().getEndTime(),
            s.getExamDate().atTime(s.getTimeslot().getEndTime()));
        return s.getExamDate().atTime(s.getTimeslot().getEndTime());
    }

    private int countStudentsForSchedule(ExamSchedule s) {
        if (s.getSection() != null) {
            return (int) studentRepository.countBySectionId(s.getSection().getId());
        } else if (s.getAcademicClass() != null) {
            return (int) studentRepository.countBySection_AcademicClass_Id(s.getAcademicClass().getId());
        }
        return 0;
    }

    private List<Student> resolveStudents(ExamSchedule s) {
        if (s.getSection() != null) {
            return studentRepository.findBySectionIdOrderByRollNoAsc(s.getSection().getId());
        } else if (s.getAcademicClass() != null) {
            return studentRepository.findBySection_AcademicClass_IdOrderByRollNoAsc(s.getAcademicClass().getId());
        }
        return Collections.emptyList();
    }

    private SeatAllocationResponseDTO toResponse(SeatAllocation sa) {
        String firstName = sa.getStudent().getUserProfile().getFirstName();
        String lastName = sa.getStudent().getUserProfile().getLastName();
        String positionLabel = sa.getPosition() == SeatPosition.SINGLE
            ? "" : " - " + sa.getPosition().name();
        return SeatAllocationResponseDTO.builder()
            .allocationId(sa.getId())
            .studentName((firstName + " " + (lastName != null ? lastName : "")).trim())
            .enrollmentNumber(sa.getStudent().getEnrollmentNumber())
            .rollNo(sa.getStudent().getRollNo())
            .seatLabel(sa.getSeat().getLabel() + positionLabel)
            .position(sa.getPosition().name())
            .seatId(sa.getSeat().getId())
            .studentId(sa.getStudent().getUuid())
            .roomName(sa.getSeat().getRoom().getName())
            .rowNumber(sa.getSeat().getRowNumber())
            .columnNumber(sa.getSeat().getColumnNumber())
            .startTime(sa.getStartTime())
            .endTime(sa.getEndTime())
            .build();
    }
}
