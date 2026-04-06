package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.Seat;
import com.project.edusync.em.model.entity.SeatAllocation;
import com.project.edusync.em.model.enums.SeatPosition;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface SeatAllocationRepository extends JpaRepository<SeatAllocation, Long> {

    // ── 1. Overlap detection: occupied seat IDs in a room (SINGLE query) ─────
    //    KEPT for backward compat
    @Query("""
        SELECT sa.seat.id FROM SeatAllocation sa
        WHERE sa.seat.room.id = :roomId
          AND sa.startTime < :endTime
          AND sa.endTime > :startTime
        """)
    Set<Long> findOccupiedSeatIdsInRoom(
        @Param("roomId") Long roomId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    // ── 2. Room availability: total allocation count per room — ALL rooms (SINGLE query)
    //    COUNT(sa.id) counts total allocations, not distinct seats, for capacity math
    @Query("""
        SELECT sa.seat.room.id, COUNT(sa.id)
        FROM SeatAllocation sa
        WHERE sa.startTime < :endTime
          AND sa.endTime > :startTime
        GROUP BY sa.seat.room.id
        """)
    List<Object[]> countOccupiedAllocationsPerRoom(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    // ── 3. Bulk student conflict check — ALL students at once, SINGLE query ──
    @Query("""
        SELECT sa.student.id FROM SeatAllocation sa
        WHERE sa.student.id IN :studentIds
          AND sa.startTime < :endTime
          AND sa.endTime > :startTime
        """)
    Set<Long> findAlreadyAllocatedStudentIds(
        @Param("studentIds") Collection<Long> studentIds,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    // ── 4. Pessimistic lock — lock ALL seats in room (unfiltered) ────────────
    //    Locks every seat row in the room to prevent concurrent allocation races.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("""
        SELECT s FROM Seat s
        WHERE s.room.id = :roomId
        ORDER BY s.rowNumber ASC, s.columnNumber ASC
        """)
    List<Seat> lockAllSeatsInRoom(@Param("roomId") Long roomId);

    // ── 4b. Pessimistic lock — lock AVAILABLE seats in room (filtered via NOT EXISTS) ────────────
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("""
        SELECT s FROM Seat s
        WHERE s.room.id = :roomId
          AND NOT EXISTS (
              SELECT 1 FROM SeatAllocation sa
              WHERE sa.seat.id = s.id
                AND sa.startTime < :endTime
                AND sa.endTime > :startTime
                AND (sa.position = com.project.edusync.em.model.enums.SeatPosition.SINGLE OR sa.position = :requestedPosition)
          )
        ORDER BY s.rowNumber ASC, s.columnNumber ASC
        """)
    List<Seat> lockAvailableSeatsInRoom(
        @Param("roomId") Long roomId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("requestedPosition") SeatPosition requestedPosition);

    // ── 5. Per-seat occupancy via GROUP BY (no subquery, no N+1) ─────────────
    //    Returns [seatId, allocationCount] pairs for occupied seats in a time window.
    //    Seats with 0 allocations won't appear — absent = 0.
    @Query("""
        SELECT sa.seat.id, COUNT(sa.id)
        FROM SeatAllocation sa
        WHERE sa.seat.room.id = :roomId
          AND sa.startTime < :endTime
          AND sa.endTime > :startTime
        GROUP BY sa.seat.id
        """)
    List<Object[]> countAllocationsPerSeatInRoom(
        @Param("roomId") Long roomId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    // ── 6. Fetch allocations with all joins — zero lazy-loading issues ────────
    @Query("""
        SELECT sa FROM SeatAllocation sa
        JOIN FETCH sa.seat s
        JOIN FETCH s.room r
        JOIN FETCH sa.student st
        JOIN FETCH st.userProfile up
        WHERE sa.examSchedule.id = :examScheduleId
        ORDER BY s.rowNumber ASC, s.columnNumber ASC
        """)
    List<SeatAllocation> findByExamScheduleWithDetails(
        @Param("examScheduleId") Long examScheduleId);

    // ── 7. Single student conflict check ──────────────────────────────────────
    @Query("""
        SELECT COUNT(sa) > 0 FROM SeatAllocation sa
        WHERE sa.student.id = :studentId
          AND sa.startTime < :endTime
          AND sa.endTime > :startTime
        """)
    boolean isStudentAllocatedInTimeWindow(
        @Param("studentId") Long studentId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    // ── 8. Simple fetch by schedule (for bulk delete etc.) ────────────────────
    List<SeatAllocation> findByExamScheduleId(Long examScheduleId);

    // ── 9. Find seats blocked for a requested side in DOUBLE seating ─────────────
    //    Blocked if seat already has SINGLE allocation, or an allocation on requested side.
    @Query("""
        SELECT DISTINCT sa.seat.id
        FROM SeatAllocation sa
        WHERE sa.seat.room.id = :roomId
          AND sa.startTime < :endTime
          AND sa.endTime > :startTime
          AND (sa.position = com.project.edusync.em.model.enums.SeatPosition.SINGLE OR sa.position = :requestedPosition)
        """)
    Set<Long> findSeatIdsBlockedForPosition(
        @Param("roomId") Long roomId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("requestedPosition") SeatPosition requestedPosition);

    // ── 10. Count seats blocked for requested side per room ────────────────
    @Query("""
        SELECT sa.seat.room.id, COUNT(DISTINCT sa.seat.id)
        FROM SeatAllocation sa
        WHERE sa.startTime < :endTime
          AND sa.endTime > :startTime
          AND (sa.position = com.project.edusync.em.model.enums.SeatPosition.SINGLE OR sa.position = :requestedPosition)
        GROUP BY sa.seat.room.id
        """)
    List<Object[]> countBlockedSeatsPerRoomForPosition(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("requestedPosition") SeatPosition requestedPosition);

    // ── 11. Count allocations on a requested position per room ────────
    @Query("""
        SELECT sa.seat.room.id, COUNT(sa.id)
        FROM SeatAllocation sa
        WHERE sa.startTime < :endTime
          AND sa.endTime > :startTime
          AND sa.position = :position
        GROUP BY sa.seat.room.id
        """)
    List<Object[]> countAllocationsPerRoomByPosition(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("position") SeatPosition position);

    @Query("""
        SELECT sa.seat.id, COUNT(sa.id)
        FROM SeatAllocation sa
        WHERE sa.seat.room.id = :roomId
          AND sa.startTime < :endTime
          AND sa.endTime > :startTime
          AND sa.position = :position
        GROUP BY sa.seat.id
        """)
    List<Object[]> countAllocationsPerSeatInRoomByPosition(
        @Param("roomId") Long roomId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("position") SeatPosition position);

    // ── 12. Find occupied positions for seats in a room during a time window ──
    // Returns [seatId, position] pairs for all allocations overlapping the window.
    // Used to determine which position (LEFT/RIGHT) is already taken.
    @Query("""
        SELECT sa.seat.id, sa.position
        FROM SeatAllocation sa
        WHERE sa.seat.room.id = :roomId
          AND sa.startTime < :endTime
          AND sa.endTime > :startTime
        """)
    List<Object[]> findOccupiedPositionsPerSeatInRoom(
        @Param("roomId") Long roomId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    // ── 13. Find allocation for a specific student in a specific schedule ──
    @Query("""
        SELECT sa FROM SeatAllocation sa
        JOIN FETCH sa.seat s
        JOIN FETCH s.room r
        JOIN FETCH sa.student st
        JOIN FETCH st.userProfile up
        WHERE sa.examSchedule.id = :examScheduleId
          AND sa.student.id = :studentId
        """)
    Optional<SeatAllocation> findByExamScheduleIdAndStudentId(
        @Param("examScheduleId") Long examScheduleId,
        @Param("studentId") Long studentId);

    // ── 14. Find allocations by room to build Room mode and OccpuiedBy ──
    @Query("""
        SELECT sa.seat.room.id, sa.examSchedule.maxStudentsPerSeat, sa.examSchedule.subject.name, sa.examSchedule.academicClass.name, COUNT(sa.id)
        FROM SeatAllocation sa
        WHERE sa.startTime < :endTime
          AND sa.endTime > :startTime
        GROUP BY sa.seat.room.id, sa.examSchedule.maxStudentsPerSeat, sa.examSchedule.subject.name, sa.examSchedule.academicClass.name
        """)
    List<Object[]> findRoomOccupancyDetails(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);
}
