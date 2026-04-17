package com.project.edusync.em.model.service;

import com.project.edusync.adm.exception.ResourceNotFoundException;
import com.project.edusync.adm.model.entity.Room;
import com.project.edusync.adm.repository.RoomRepository;
import com.project.edusync.common.config.CacheNames;
import com.project.edusync.common.exception.BadRequestException;
import com.project.edusync.common.settings.service.AppSettingService;
import com.project.edusync.em.model.dto.request.BulkSeatAllocationRequestDTO;
import com.project.edusync.em.model.dto.request.SingleSeatAllocationRequestDTO;
import com.project.edusync.em.model.dto.response.*;
import com.project.edusync.em.model.entity.ExamSchedule;
import com.project.edusync.em.model.entity.Seat;
import com.project.edusync.em.model.entity.SeatAllocation;
import com.project.edusync.em.model.repository.ExamScheduleRepository;
import com.project.edusync.em.model.repository.SeatAllocationRepository;
import com.project.edusync.em.model.repository.SeatRepository;
import com.project.edusync.finance.service.PdfGenerationService;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatAllocationService {

    private final SeatRepository seatRepository;
    private final SeatAllocationRepository allocationRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final PdfGenerationService pdfGenerationService;
    private final AppSettingService appSettingService;

    private static final int BATCH_SIZE = 50;

    private enum SeatingPlanPdfFormat {
        ROOM_WISE,
        ADMIN_TABLE;

        static SeatingPlanPdfFormat from(String value) {
            if (value == null || value.isBlank()) {
                return ROOM_WISE;
            }
            String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
            try {
                return SeatingPlanPdfFormat.valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid seating plan format: " + value + ". Supported values: ROOM_WISE, ADMIN_TABLE");
            }
        }
    }

    /** Position labels: 0→LEFT, 1→MIDDLE, 2→RIGHT */
    private static final String[] POSITION_LABELS = {"LEFT", "MIDDLE", "RIGHT"};

    private static String positionLabel(int index) {
        return index >= 0 && index < POSITION_LABELS.length ? POSITION_LABELS[index] : "POS_" + index;
    }

    private static String modeLabel(int maxPerSeat) {
        return switch (maxPerSeat) {
            case 1 -> "SINGLE";
            case 2 -> "DOUBLE";
            case 3 -> "TRIPLE";
            default -> "MULTI_" + maxPerSeat;
        };
    }

    // ════════════════════════════════════════════════════════════════
    // SEAT GENERATION (called on room create/update)
    // ════════════════════════════════════════════════════════════════

    @Transactional
    @CacheEvict(value = CacheNames.ROOM_AVAILABILITY, allEntries = true)
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
    // GET AVAILABLE ROOMS (capacity-aware)
    //
    // Capacity formula:
    //   totalCapacity = totalSeats × maxStudentsPerSeat
    //   availableCapacity = totalCapacity - currentAllocations
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.ROOM_AVAILABILITY, key = "#examScheduleId")
    public List<RoomAvailabilityDTO> getAvailableRooms(Long examScheduleId) {
        ExamSchedule schedule = fetchSchedule(examScheduleId);
        LocalDateTime start = deriveStartTime(schedule);
        LocalDateTime end = deriveEndTime(schedule);
        int maxPerSeat = schedule.getMaxStudentsPerSeat();

        // 1. Count total students needing seats
        int totalStudents = countStudentsForSchedule(schedule);

        // 2. All active rooms (SINGLE query)
        List<Room> rooms = roomRepository.findAllActive();

        // 3. Seat counts per room
        Map<Long, Integer> examSeatUnitsMap = rooms.stream()
            .collect(Collectors.toMap(Room::getId, r -> Optional.ofNullable(r.getExamSeatUnits()).orElse(0)));

        // 4. Total allocations per room in this time window (SINGLE query)
        Map<Long, Long> allocationsPerRoom = new HashMap<>();
        allocationRepository.countOccupiedAllocationsPerRoom(start, end)
            .forEach(row -> allocationsPerRoom.put((Long) row[0], (Long) row[1]));

        // 5. Room Occupancy details for mode and occupiedBy
        List<Object[]> roomOccupancyRows = allocationRepository.findRoomOccupancyDetails(start, end);
        Map<Long, List<Object[]>> occupancyDetailsByRoom = roomOccupancyRows.stream()
            .collect(Collectors.groupingBy(row -> ((Long) row[0])));

        // 6. Build response
        return rooms.stream()
            .map(room -> {
                int totalSeats = examSeatUnitsMap.getOrDefault(room.getId(), 0);
                int totalCapacity = totalSeats * maxPerSeat;
                int occupiedCapacity = allocationsPerRoom.getOrDefault(room.getId(), 0L).intValue();
                int availableCapacity = totalCapacity - occupiedCapacity;

                List<Object[]> occupancyRows = occupancyDetailsByRoom.getOrDefault(room.getId(), Collections.emptyList());

                String mode = modeLabel(maxPerSeat);

                List<OccupiedByDTO> occupiedBy = occupancyRows.stream()
                    .map(row -> new OccupiedByDTO(
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
    // Returns per-seat data with rich occupied slot info
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<SeatAvailabilityDTO> getAvailableSeats(Long examScheduleId, UUID roomUuid) {
        ExamSchedule schedule = fetchSchedule(examScheduleId);
        Room room = roomRepository.findActiveById(roomUuid)
            .orElseThrow(() -> new ResourceNotFoundException("No resource found with id: " + roomUuid));

        LocalDateTime start = deriveStartTime(schedule);
        LocalDateTime end = deriveEndTime(schedule);
        int maxPerSeat = schedule.getMaxStudentsPerSeat();

        // All seats for room (SINGLE query)
        List<Seat> seats = seatRepository.findByRoomIdOrderByRowNumberAscColumnNumberAsc(room.getId());

        // Per-seat occupancy count (SINGLE query)
        Map<Long, Long> seatOccupancy = new HashMap<>();
        allocationRepository.countAllocationsPerSeatInRoom(room.getId(), start, end)
            .forEach(row -> seatOccupancy.put((Long) row[0], (Long) row[1]));

        // Rich slot details: [seatId, positionIndex, subjectName, className, studentName]
        Map<Long, List<OccupiedSlotDTO>> slotsMap = new HashMap<>();
        allocationRepository.findOccupiedSlotDetailsInRoom(room.getId(), start, end)
            .forEach(row -> {
                Long seatId = (Long) row[0];
                int posIdx = (Integer) row[1];
                OccupiedSlotDTO slot = OccupiedSlotDTO.builder()
                    .positionIndex(posIdx)
                    .positionLabel(positionLabel(posIdx))
                    .subjectName((String) row[2])
                    .className((String) row[3])
                    .studentName(((String) row[4]).trim())
                    .build();
                slotsMap.computeIfAbsent(seatId, k -> new ArrayList<>()).add(slot);
            });

        return seats.stream()
            .map(s -> {
                int occupied = seatOccupancy.getOrDefault(s.getId(), 0L).intValue();
                boolean isFull = occupied >= maxPerSeat;
                int availableSlots = Math.max(0, maxPerSeat - occupied);

                return SeatAvailabilityDTO.builder()
                    .seatId(s.getId())
                    .label(s.getLabel())
                    .rowNumber(s.getRowNumber())
                    .columnNumber(s.getColumnNumber())
                    .capacity(maxPerSeat)
                    .occupiedCount(occupied)
                    .availableSlots(availableSlots)
                    .isFull(isFull)
                    .available(!isFull)
                    .occupiedSlots(slotsMap.getOrDefault(s.getId(), Collections.emptyList()))
                    .build();
            })
            .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════
    // GET BULK AVAILABLE SEATS IN MULTIPLE ROOMS (for grid visualization)
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Map<UUID, List<SeatAvailabilityDTO>> getBulkAvailableSeats(Long examScheduleId, List<UUID> roomUuids) {
        if (roomUuids == null || roomUuids.isEmpty()) {
            return Collections.emptyMap();
        }

        ExamSchedule schedule = fetchSchedule(examScheduleId);
        List<Room> rooms = roomRepository.findAllActive().stream()
            .filter(r -> roomUuids.contains(r.getUuid()))
            .collect(Collectors.toList());

        if (rooms.isEmpty()) {
            return Collections.emptyMap();
        }

        LocalDateTime start = deriveStartTime(schedule);
        LocalDateTime end = deriveEndTime(schedule);
        int maxPerSeat = schedule.getMaxStudentsPerSeat();

        List<Long> roomIds = rooms.stream().map(Room::getId).collect(Collectors.toList());

        // All seats for requested rooms (SINGLE query)
        List<Seat> allSeats = seatRepository.findByRoomIdInOrderByRowNumberAscColumnNumberAsc(roomIds);

        // Per-seat occupancy count (SINGLE bulk query)
        Map<Long, Long> seatOccupancy = new HashMap<>();
        allocationRepository.countAllocationsPerSeatInRooms(roomIds, start, end)
            .forEach(row -> seatOccupancy.put((Long) row[0], (Long) row[1]));

        // Rich slot details: [roomUuid, seatId, positionIndex, subjectName, className, studentName]
        Map<Long, List<OccupiedSlotDTO>> slotsMap = new HashMap<>();
        allocationRepository.findOccupiedSlotDetailsInRooms(roomIds, start, end)
            .forEach(row -> {
                Long seatId = (Long) row[1];
                int posIdx = (Integer) row[2];
                OccupiedSlotDTO slot = OccupiedSlotDTO.builder()
                    .positionIndex(posIdx)
                    .positionLabel(positionLabel(posIdx))
                    .subjectName((String) row[3])
                    .className((String) row[4])
                    .studentName(((String) row[5]).trim())
                    .build();
                slotsMap.computeIfAbsent(seatId, k -> new ArrayList<>()).add(slot);
            });

        // Group seats by Room UUID
        Map<UUID, List<Seat>> seatsByRoomUuid = allSeats.stream()
            .collect(Collectors.groupingBy(s -> s.getRoom().getUuid()));

        Map<UUID, List<SeatAvailabilityDTO>> result = new HashMap<>();

        for (Room room : rooms) {
            List<Seat> roomSeats = seatsByRoomUuid.getOrDefault(room.getUuid(), Collections.emptyList());
            
            List<SeatAvailabilityDTO> dtoList = roomSeats.stream()
                .map(s -> {
                    int occupied = seatOccupancy.getOrDefault(s.getId(), 0L).intValue();
                    boolean isFull = occupied >= maxPerSeat;
                    int availableSlots = Math.max(0, maxPerSeat - occupied);

                    return SeatAvailabilityDTO.builder()
                        .seatId(s.getId())
                        .label(s.getLabel())
                        .rowNumber(s.getRowNumber())
                        .columnNumber(s.getColumnNumber())
                        .capacity(maxPerSeat)
                        .occupiedCount(occupied)
                        .availableSlots(availableSlots)
                        .isFull(isFull)
                        .available(!isFull)
                        .occupiedSlots(slotsMap.getOrDefault(s.getId(), Collections.emptyList()))
                        .build();
                })
                .collect(Collectors.toList());
                
            result.put(room.getUuid(), dtoList);
        }

        return result;
    }

    // ════════════════════════════════════════════════════════════════
    // SINGLE STUDENT ALLOCATION (manual assignment)
    // Finds next available positionIndex, validates conflicts
    // ════════════════════════════════════════════════════════════════

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheNames.ROOM_AVAILABILITY, key = "#dto.examScheduleId"),
        @CacheEvict(value = CacheNames.SEATING_PLAN_PDF, allEntries = true)
    })
    public SeatAllocationResponseDTO allocateSingleSeat(SingleSeatAllocationRequestDTO dto) {
        ExamSchedule schedule = fetchSchedule(dto.getExamScheduleId());
        LocalDateTime start = deriveStartTime(schedule);
        LocalDateTime end = deriveEndTime(schedule);
        int maxPerSeat = schedule.getMaxStudentsPerSeat();

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

        // Check seat capacity
        Set<Integer> occupiedPositions = allocationRepository.findOccupiedPositionIndices(seat.getId(), start, end);
        if (occupiedPositions.size() >= maxPerSeat) {
            throw new BadRequestException("Seat is at full capacity (" + maxPerSeat + "/" + maxPerSeat + "). Cannot assign.");
        }

        // A seat can host only one student for the same schedule in the overlapping slot.
        if (allocationRepository.existsScheduleConflictOnSeat(
                seat.getId(), start, end, schedule.getId())) {
            throw new BadRequestException(
                "Conflict: this seat already has a student for the selected exam schedule.");
        }

        // Find next available positionIndex
        int positionIndex = findNextAvailablePosition(occupiedPositions, maxPerSeat);

        SeatAllocation allocation = new SeatAllocation();
        allocation.setSeat(seat);
        allocation.setStudent(student);
        allocation.setExamSchedule(schedule);
        allocation.setStartTime(start);
        allocation.setEndTime(end);
        allocation.setPositionIndex(positionIndex);

        return toResponse(allocationRepository.save(allocation));
    }

    // ════════════════════════════════════════════════════════════════
    // BULK AUTO-ALLOCATION (concurrency-safe with pessimistic lock)
    //
    // Algorithm:
    //   1. Lock ALL seats in room
    //   2. Get occupancy map via GROUP BY
    //   3. Compute available seats (occupancy < maxPerSeat)
    //   4. Sort: partially filled first (fill existing benches)
    //   5. For each student, find seat with capacity + no conflict
    //   6. Assign next available positionIndex
    //   7. Batched insert
    // ════════════════════════════════════════════════════════════════

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheNames.ROOM_AVAILABILITY, key = "#dto.examScheduleId"),
        @CacheEvict(value = CacheNames.SEATING_PLAN_PDF, allEntries = true)
    })
    public List<SeatAllocationResponseDTO> bulkAllocate(BulkSeatAllocationRequestDTO dto) {
        ExamSchedule schedule = fetchSchedule(dto.getExamScheduleId());
        Room room = roomRepository.findActiveById(dto.getRoomId())
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + dto.getRoomId()));

        LocalDateTime start = deriveStartTime(schedule);
        LocalDateTime end = deriveEndTime(schedule);
        int maxPerSeat = schedule.getMaxStudentsPerSeat();

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

        // 3. PESSIMISTIC LOCK: lock all seats in room
        List<Seat> allSeats = allocationRepository.lockAllSeatsInRoom(room.getId());

        if (allSeats.isEmpty()) {
            throw new BadRequestException("No configured seats available in this room");
        }

        // 4. Get per-seat occupancy via GROUP BY (SINGLE query)
        Map<Long, Long> seatOccupancy = new HashMap<>();
        allocationRepository.countAllocationsPerSeatInRoom(room.getId(), start, end)
            .forEach(row -> seatOccupancy.put((Long) row[0], (Long) row[1]));

        // 5. Get occupied position indices per seat (for positionIndex assignment)
        Map<Long, Set<Integer>> occupiedPositionsPerSeat = new HashMap<>();
        allocationRepository.findOccupiedSlotDetailsInRoom(room.getId(), start, end)
            .forEach(row -> {
                Long seatId = (Long) row[0];
                int posIdx = (Integer) row[1];
                occupiedPositionsPerSeat.computeIfAbsent(seatId, k -> new HashSet<>()).add(posIdx);
            });

        // Seats already used by this schedule cannot be reused.
        Set<Long> scheduleUsedSeatIds = allocationRepository.findSeatIdsAlreadyUsedByScheduleInRoom(
            room.getId(), start, end, schedule.getId());

        // 6. Filter to seats with available capacity and no same-schedule occupancy
        List<Seat> availableSeats = allSeats.stream()
            .filter(s -> seatOccupancy.getOrDefault(s.getId(), 0L) < maxPerSeat)
            .filter(s -> !scheduleUsedSeatIds.contains(s.getId()))
            .collect(Collectors.toList());

        long totalAvailableSlots = availableSeats.stream()
            .mapToLong(s -> maxPerSeat - seatOccupancy.getOrDefault(s.getId(), 0L))
            .sum();

        if (totalAvailableSlots <= 0) {
            throw new BadRequestException("No available capacity in this room");
        }

        int scheduleEligibleSeats = availableSeats.size();
        int toAllocate = Math.min(unallocated.size(), scheduleEligibleSeats);
        log.info("Allocating {} students to room {} (eligibleSeats: {}, openSlots: {}, maxPerSeat: {}) for Schedule ID {}",
            toAllocate, room.getUuid(), scheduleEligibleSeats, totalAvailableSlots, maxPerSeat, schedule.getId());

        // 7. SORT: partially filled seats first (higher occupancy = higher priority), then by position
        List<Seat> sortedSeats = availableSeats.stream()
            .sorted(Comparator
                .comparingLong((Seat s) -> seatOccupancy.getOrDefault(s.getId(), 0L)).reversed()
                .thenComparingInt(Seat::getRowNumber)
                .thenComparingInt(Seat::getColumnNumber))
            .collect(Collectors.toList());

        // 8. Build allocations — at most one student per seat for this schedule
        List<SeatAllocation> newAllocations = new ArrayList<>(toAllocate);
        int studentIdx = 0;

        for (Seat seat : sortedSeats) {
            if (studentIdx >= toAllocate) break;

            Set<Integer> occupiedPos = occupiedPositionsPerSeat.getOrDefault(seat.getId(), new HashSet<>());
            if (occupiedPos.size() >= maxPerSeat) {
                continue;
            }

            int positionIndex = findNextAvailablePosition(occupiedPos, maxPerSeat);

            SeatAllocation sa = new SeatAllocation();
            sa.setSeat(seat);
            sa.setStudent(unallocated.get(studentIdx));
            sa.setExamSchedule(schedule);
            sa.setStartTime(start);
            sa.setEndTime(end);
            sa.setPositionIndex(positionIndex);

            newAllocations.add(sa);
            occupiedPos.add(positionIndex);
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
    @Cacheable(value = CacheNames.SEATING_PLAN_PDF, key = "#examScheduleId + ':ROOM_WISE:V2'")
    public byte[] generateSeatingPlanPdf(Long examScheduleId) {
        return buildSeatingPlanPdf(examScheduleId, SeatingPlanPdfFormat.ROOM_WISE);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.SEATING_PLAN_PDF, key = "#examScheduleId + ':' + (#format == null ? 'ROOM_WISE' : #format.trim().replace('-', '_').toUpperCase()) + ':V2'")
    public byte[] generateSeatingPlanPdf(Long examScheduleId, String format) {
        SeatingPlanPdfFormat selectedFormat = SeatingPlanPdfFormat.from(format);
        return buildSeatingPlanPdf(examScheduleId, selectedFormat);
    }

    private byte[] buildSeatingPlanPdf(Long examScheduleId, SeatingPlanPdfFormat selectedFormat) {
        ExamSchedule schedule = fetchSchedule(examScheduleId);
        SeatingPlanPdfFormat effectiveFormat = SeatingPlanPdfFormat.ROOM_WISE;

        Map<String, Object> data = new HashMap<>();
        populateSchoolBrandingData(data);

        data.put("title", "Seating Plan");
        data.put("examName", schedule.getExam().getName());
        data.put("subjectName", schedule.getSubject().getName());
        data.put("className", schedule.getAcademicClass() != null ? schedule.getAcademicClass().getName() : "-");
        data.put("sectionName", schedule.getSection() != null ? schedule.getSection().getSectionName() : "All Sections");
        data.put("examDate", schedule.getExamDate() != null ? schedule.getExamDate().toString() : "-");
        data.put("startTime", schedule.getTimeslot() != null ? String.valueOf(schedule.getTimeslot().getStartTime()) : "-");
        data.put("endTime", schedule.getTimeslot() != null ? String.valueOf(schedule.getTimeslot().getEndTime()) : "-");
        data.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
        data.put("format", effectiveFormat.name());
        data.put("isRoomWise", true);
        data.put("isAdminTable", false);

        LocalDateTime start = deriveStartTime(schedule);
        LocalDateTime end = deriveEndTime(schedule);
        List<SeatAllocationRepository.SeatingPlanPdfProjection> allocations =
            allocationRepository.findSeatingPlanRowsByRoomsAndTimeOverlap(examScheduleId, start, end);
        data.put("totalAssigned", allocations.size());
        data.put("rows", Collections.emptyList());
        data.put("rooms", buildRoomWiseLayout(allocations));

        return pdfGenerationService.generatePdfFromHtml("em/seating-plan", data);
    }

    private List<SeatingPlanRoomPdfDTO> buildRoomWiseLayout(List<SeatAllocationRepository.SeatingPlanPdfProjection> allocations) {
        Map<Long, RoomAggregation> grouped = new HashMap<>();

        for (SeatAllocationRepository.SeatingPlanPdfProjection allocation : allocations) {
            Long roomId = allocation.getRoomId() != null ? allocation.getRoomId() : -1L;
            String roomName = allocation.getRoomName() == null || allocation.getRoomName().isBlank()
                ? "Unknown Room"
                : allocation.getRoomName();
            int rowNumber = Math.max(1, Optional.ofNullable(allocation.getRowNumber()).orElse(1));
            int columnNumber = Math.max(1, Optional.ofNullable(allocation.getColumnNumber()).orElse(1));

            RoomAggregation roomAggregation = grouped.computeIfAbsent(
                roomId,
                key -> new RoomAggregation(roomId, roomName)
            );

            String rollDisplay = formatRollClassCode(allocation);
            roomAggregation.rows
                .computeIfAbsent(rowNumber, key -> new TreeMap<>())
                .computeIfAbsent(columnNumber, key -> new ArrayList<>())
                .add(rollDisplay);
            roomAggregation.totalStudents++;
        }

        return grouped.values().stream()
            .sorted(Comparator
                .comparing((RoomAggregation room) -> room.roomName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(room -> room.roomId))
            .map(this::toRoomPdfDto)
            .collect(Collectors.toList());
    }

    private SeatingPlanRoomPdfDTO toRoomPdfDto(RoomAggregation roomAggregation) {
        int maxBenchCount = roomAggregation.rows.values().stream()
            .flatMap(columns -> columns.keySet().stream())
            .max(Integer::compareTo)
            .orElse(0);

        List<Integer> benchHeaders = maxBenchCount > 0
            ? IntStream.rangeClosed(1, maxBenchCount).boxed().collect(Collectors.toList())
            : Collections.emptyList();

        List<RowDTO> rows = roomAggregation.rows.entrySet().stream()
            .map(rowEntry -> {
                List<BenchDTO> benches = IntStream.rangeClosed(1, Math.max(1, maxBenchCount))
                    .mapToObj(columnNumber -> {
                        List<String> rolls = rowEntry.getValue().getOrDefault(columnNumber, Collections.emptyList());
                        String display = rolls.isEmpty() ? "-" : String.join(" | ", rolls);
                        return BenchDTO.builder()
                            .rowNumber(rowEntry.getKey())
                            .columnNumber(columnNumber)
                            .display(display)
                            .build();
                    })
                    .collect(Collectors.toList());

                return RowDTO.builder()
                    .rowNumber(rowEntry.getKey())
                    .benches(benches)
                    .build();
            })
            .collect(Collectors.toList());

        return SeatingPlanRoomPdfDTO.builder()
            .roomName(roomAggregation.roomName)
            .totalStudents(roomAggregation.totalStudents)
            .maxBenchCount(maxBenchCount)
            .benchHeaders(benchHeaders)
            .rows(rows)
            .build();
    }

    private static final class RoomAggregation {
        private final Long roomId;
        private final String roomName;
        private final Map<Integer, Map<Integer, List<String>>> rows = new TreeMap<>();
        private int totalStudents;

        private RoomAggregation(Long roomId, String roomName) {
            this.roomId = roomId;
            this.roomName = roomName;
        }
    }

    private String formatRollClassCode(SeatAllocationRepository.SeatingPlanPdfProjection allocation) {
        if (allocation.getRollNo() == null) {
            return "-";
        }
        String classNumber = extractClassNumber(allocation.getClassName());
        return allocation.getRollNo() + "-C" + classNumber;
    }

    private String extractClassNumber(String className) {
        if (className == null || className.isBlank()) {
            return "NA";
        }

        String digitsOnly = className.replaceAll("\\D+", "");
        if (!digitsOnly.isBlank()) {
            return digitsOnly;
        }

        return className.trim();
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
    @CacheEvict(value = {CacheNames.ROOM_AVAILABILITY, CacheNames.SEATING_PLAN_PDF}, allEntries = true)
    public void deleteAllocation(Long allocationId) {
        if (!allocationRepository.existsById(allocationId)) {
            throw new ResourceNotFoundException("SeatAllocation not found with id: " + allocationId);
        }
        allocationRepository.deleteById(allocationId);
    }

    @Transactional
    @CacheEvict(value = {CacheNames.ROOM_AVAILABILITY, CacheNames.SEATING_PLAN_PDF}, allEntries = true)
    public void bulkDeleteAllocations(List<Long> allocationIds) {
        if (allocationIds == null || allocationIds.isEmpty()) return;
        allocationRepository.deleteAllByIdInBatch(allocationIds);
    }

    // ── Private helpers ──────────────────────────────────────────

    /**
     * Finds the smallest positionIndex in [0, maxPerSeat) that is not yet occupied.
     */
    private int findNextAvailablePosition(Set<Integer> occupied, int maxPerSeat) {
        for (int i = 0; i < maxPerSeat; i++) {
            if (!occupied.contains(i)) {
                return i;
            }
        }
        throw new BadRequestException("No available position on this seat (all " + maxPerSeat + " positions are taken)");
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
        int maxPerSeat = sa.getExamSchedule().getMaxStudentsPerSeat();
        int posIdx = sa.getPositionIndex();

        String label = maxPerSeat == 1 ? "" : positionLabel(posIdx);
        String positionSuffix = label.isEmpty() ? "" : " - " + label;

        return SeatAllocationResponseDTO.builder()
            .allocationId(sa.getId())
            .studentName((firstName + " " + (lastName != null ? lastName : "")).trim())
            .enrollmentNumber(sa.getStudent().getEnrollmentNumber())
            .rollNo(sa.getStudent().getRollNo())
            .seatLabel(sa.getSeat().getLabel() + positionSuffix)
            .positionIndex(posIdx)
            .positionLabel(label)
            .seatId(sa.getSeat().getId())
            .studentId(sa.getStudent().getUuid())
            .roomName(sa.getSeat().getRoom().getName())
            .rowNumber(sa.getSeat().getRowNumber())
            .columnNumber(sa.getSeat().getColumnNumber())
            .startTime(sa.getStartTime())
            .endTime(sa.getEndTime())
            .subjectName(sa.getExamSchedule().getSubject().getName())
            .className(sa.getExamSchedule().getAcademicClass().getName())
            .build();
    }

    private void populateSchoolBrandingData(Map<String, Object> data) {
        String schoolName = appSettingService.getValue("school.name", "My School");
        String shortName = appSettingService.getValue("school.short_name", "");
        data.put("schoolName", schoolName);
        data.put("schoolShortName", shortName.isBlank() ? schoolName : shortName);
        data.put("schoolTagline", appSettingService.getValue("school.tagline", ""));
        data.put("schoolAddress", appSettingService.getValue("school.address", ""));
        data.put("schoolPhone", appSettingService.getValue("school.phone", ""));
        data.put("schoolEmail", appSettingService.getValue("school.email", ""));

        String headerMode = appSettingService.getValue("school.id_card_header_mode", "TEXT");
        String headerImageUrl = appSettingService.getValue("school.id_card_header_image_url", "");
        String headerImageBase64 = "";
        if ("IMAGE".equalsIgnoreCase(headerMode) && !headerImageUrl.isBlank()) {
            headerImageBase64 = pdfGenerationService.fetchRemoteImageAsBase64OrEmpty(headerImageUrl);
        }
        data.put("headerImageEnabled", !headerImageBase64.isBlank());
        data.put("headerImageBase64", headerImageBase64);

        String logoUrl = appSettingService.getValue("school.logo_url", "");
        if (!logoUrl.isBlank()) {
            data.put("schoolLogoBase64", pdfGenerationService.fetchRemoteImageAsBase64(logoUrl));
        } else {
            data.put("schoolLogoBase64", pdfGenerationService.loadSchoolLogoBase64());
        }
    }
}
