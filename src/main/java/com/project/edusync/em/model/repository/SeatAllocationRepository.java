package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.Seat;
import com.project.edusync.em.model.entity.SeatAllocation;
import com.project.edusync.em.model.enums.ExamAttendanceStatus;
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

    interface AdmitCardSeatAllocationProjection {
        Long getScheduleId();
        Long getStudentId();
        Long getSeatId();
        String getSeatLabel();
        Long getRoomId();
        String getRoomName();
        String getSubjectName();
    }

    interface SeatingPlanPdfProjection {
        Long getRoomId();
        String getRoomName();
        Integer getRowNumber();
        Integer getColumnNumber();
        Integer getPositionIndex();
        Integer getRollNo();
        String getClassName();
    }

    interface ExamRoomStudentProjection {
        Long getExamScheduleId();
        Long getStudentId();
        Integer getRollNo();
        String getFirstName();
        String getLastName();
        String getClassName();
        String getSubjectName();
        Integer getRowNumber();
        Integer getColumnNumber();
        String getSeatNumber();
        ExamAttendanceStatus getAttendanceStatus();
        Boolean getMalpractice();
        Boolean getFinalized();
        Boolean getEntryAllowed();
    }

    interface ExamControllerRoomSummaryProjection {
        Long getRoomId();
        String getRoomName();
        long getAllocatedCount();
        long getMarkedCount();
    }

    interface ExamControllerStudentSeatProjection {
        Long getExamScheduleId();
        Long getRoomId();
        String getRoomName();
        Long getStudentId();
        Integer getRollNo();
        String getFirstName();
        String getLastName();
        String getClassName();
        String getSubjectName();
        Integer getRowNumber();
        Integer getColumnNumber();
        String getSeatNumber();
        ExamAttendanceStatus getAttendanceStatus();
        Boolean getEntryAllowed();
    }

    interface RoomStudentScheduleProjection {
        Long getStudentId();
        Long getExamScheduleId();
    }

    // ── 1. Occupied seat IDs in a room (ANY allocation = occupied) ─────────
    @Query("""
        SELECT DISTINCT sa.seat.id FROM SeatAllocation sa
        WHERE sa.seat.room.id = :roomId
          AND sa.startTime < :endTime
          AND sa.endTime > :startTime
        """)
    Set<Long> findOccupiedSeatIdsInRoom(
        @Param("roomId") Long roomId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    // ── 2. Total allocation count per room (SINGLE query) ─────────────────
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

    // ── 3. Bulk student conflict check ────────────────────────────────────
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

    // ── 4. Pessimistic lock — lock ALL seats in room ──────────────────────
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("""
        SELECT s FROM Seat s
        WHERE s.room.id = :roomId
        ORDER BY s.rowNumber ASC, s.columnNumber ASC
        """)
    List<Seat> lockAllSeatsInRoom(@Param("roomId") Long roomId);

    // ── 5. Per-seat occupancy via GROUP BY (no subquery, no N+1) ──────────
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

    @Query("""
        SELECT sa.seat.id, COUNT(sa.id)
        FROM SeatAllocation sa
        WHERE sa.seat.room.id IN :roomIds
          AND sa.startTime < :endTime
          AND sa.endTime > :startTime
        GROUP BY sa.seat.id
        """)
    List<Object[]> countAllocationsPerSeatInRooms(
        @Param("roomIds") Collection<Long> roomIds,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    // ── 6. Fetch allocations with all joins ───────────────────────────────
    @Query("""
        SELECT sa FROM SeatAllocation sa
        JOIN FETCH sa.seat s
        JOIN FETCH s.room r
        JOIN FETCH sa.student st
        JOIN FETCH st.userProfile up
        JOIN FETCH sa.examSchedule es
        JOIN FETCH es.subject sub
        JOIN FETCH es.academicClass ac
        WHERE sa.examSchedule.id = :examScheduleId
        ORDER BY s.rowNumber ASC, s.columnNumber ASC, sa.positionIndex ASC
        """)
    List<SeatAllocation> findByExamScheduleWithDetails(
        @Param("examScheduleId") Long examScheduleId);

    @Query("""
        SELECT sa.seat.room.id AS roomId,
               sa.seat.room.name AS roomName,
               sa.seat.rowNumber AS rowNumber,
               sa.seat.columnNumber AS columnNumber,
               sa.positionIndex AS positionIndex,
               sa.student.rollNo AS rollNo,
               sa.examSchedule.academicClass.name AS className
        FROM SeatAllocation sa
        WHERE sa.examSchedule.id = :examScheduleId
        ORDER BY sa.seat.room.name ASC,
                 sa.seat.rowNumber ASC,
                 sa.seat.columnNumber ASC,
                 sa.positionIndex ASC
        """)
    List<SeatingPlanPdfProjection> findSeatingPlanRowsByExamScheduleId(
        @Param("examScheduleId") Long examScheduleId);

    @Query("""
        SELECT sa.seat.room.id AS roomId,
               sa.seat.room.name AS roomName,
               sa.seat.rowNumber AS rowNumber,
               sa.seat.columnNumber AS columnNumber,
               sa.positionIndex AS positionIndex,
               sa.student.rollNo AS rollNo,
               sa.examSchedule.academicClass.name AS className
        FROM SeatAllocation sa
        WHERE sa.seat.room.id IN (
            SELECT DISTINCT sa0.seat.room.id
            FROM SeatAllocation sa0
            WHERE sa0.examSchedule.id = :examScheduleId
        )
          AND sa.startTime < :endTime
          AND sa.endTime > :startTime
        ORDER BY sa.seat.room.name ASC,
                 sa.seat.rowNumber ASC,
                 sa.seat.columnNumber ASC,
                 sa.positionIndex ASC
        """)
    List<SeatingPlanPdfProjection> findSeatingPlanRowsByRoomsAndTimeOverlap(
        @Param("examScheduleId") Long examScheduleId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    // ── 7. Single student conflict check ──────────────────────────────────
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

    // ── 8. Simple fetch by schedule ───────────────────────────────────────
    List<SeatAllocation> findByExamScheduleId(Long examScheduleId);

    // ── 9. Find seat IDs at full capacity ─────────────────────────────────
    //    Returns seat IDs where allocation count >= maxPerSeat
    @Query("""
        SELECT sa.seat.id
        FROM SeatAllocation sa
        WHERE sa.seat.room.id = :roomId
          AND sa.startTime < :endTime
          AND sa.endTime > :startTime
        GROUP BY sa.seat.id
        HAVING COUNT(sa.id) >= :maxPerSeat
        """)
    Set<Long> findFullSeatIdsInRoom(
        @Param("roomId") Long roomId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("maxPerSeat") int maxPerSeat);

    // ── 10. Count full seats per room (for room availability) ─────────────
    @Query("""
        SELECT sa.seat.room.id, COUNT(DISTINCT sa.seat.id)
        FROM SeatAllocation sa
        WHERE sa.startTime < :endTime
          AND sa.endTime > :startTime
        GROUP BY sa.seat.room.id, sa.seat.id
        HAVING COUNT(sa.id) >= :maxPerSeat
        """)
    List<Object[]> countFullSeatsPerRoom(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("maxPerSeat") int maxPerSeat);

    // ── 11. Occupied position details per seat in room ────────────────────
    //    Returns [seatId, positionIndex, subjectName, className, studentFullName]
    @Query("""
        SELECT sa.seat.id, sa.positionIndex,
               sa.examSchedule.subject.name,
               sa.examSchedule.academicClass.name,
               CONCAT(sa.student.userProfile.firstName, ' ', COALESCE(sa.student.userProfile.lastName, ''))
        FROM SeatAllocation sa
        WHERE sa.seat.room.id = :roomId
          AND sa.startTime < :endTime
          AND sa.endTime > :startTime
        ORDER BY sa.seat.id, sa.positionIndex
        """)
    List<Object[]> findOccupiedSlotDetailsInRoom(
        @Param("roomId") Long roomId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    @Query("""
        SELECT sa.seat.room.uuid, sa.seat.id, sa.positionIndex,
               sa.examSchedule.subject.name,
               sa.examSchedule.academicClass.name,
               CONCAT(sa.student.userProfile.firstName, ' ', COALESCE(sa.student.userProfile.lastName, ''))
        FROM SeatAllocation sa
        WHERE sa.seat.room.id IN :roomIds
          AND sa.startTime < :endTime
          AND sa.endTime > :startTime
        ORDER BY sa.seat.id, sa.positionIndex
        """)
    List<Object[]> findOccupiedSlotDetailsInRooms(
        @Param("roomIds") Collection<Long> roomIds,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    // ── 12. Find occupied positionIndices for a specific seat ─────────────
    @Query("""
        SELECT sa.positionIndex FROM SeatAllocation sa
        WHERE sa.seat.id = :seatId
          AND sa.startTime < :endTime
          AND sa.endTime > :startTime
        """)
    Set<Integer> findOccupiedPositionIndices(
        @Param("seatId") Long seatId,
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

    @Query("""
        SELECT sa FROM SeatAllocation sa
        JOIN FETCH sa.seat s
        JOIN FETCH s.room r
        JOIN FETCH sa.student st
        WHERE sa.examSchedule.id IN :scheduleIds
          AND st.id IN :studentIds
        """)
    List<SeatAllocation> findByExamScheduleIdsAndStudentIdsWithSeat(
        @Param("scheduleIds") Collection<Long> scheduleIds,
        @Param("studentIds") Collection<Long> studentIds);

    @Query("""
        SELECT sa.examSchedule.id AS scheduleId,
               sa.student.id AS studentId,
               sa.seat.id AS seatId,
               sa.seat.label AS seatLabel,
               sa.seat.room.id AS roomId,
               sa.seat.room.name AS roomName,
               sa.examSchedule.subject.name AS subjectName
        FROM SeatAllocation sa
        WHERE sa.examSchedule.id = :scheduleId
        """)
    List<AdmitCardSeatAllocationProjection> findAdmitCardAllocationsByScheduleId(
        @Param("scheduleId") Long scheduleId);

    @Query("""
        SELECT sa.examSchedule.id AS scheduleId,
               sa.student.id AS studentId,
               sa.seat.id AS seatId,
               sa.seat.label AS seatLabel,
               sa.seat.room.id AS roomId,
               sa.seat.room.name AS roomName,
               sa.examSchedule.subject.name AS subjectName
        FROM SeatAllocation sa
        WHERE sa.examSchedule.id = :scheduleId
          AND sa.student.id IN :studentIds
        """)
    List<AdmitCardSeatAllocationProjection> findAdmitCardAllocationsByScheduleIdAndStudentIds(
        @Param("scheduleId") Long scheduleId,
        @Param("studentIds") Collection<Long> studentIds);

    @Query("""
        SELECT sa.examSchedule.id AS scheduleId,
               sa.student.id AS studentId,
               sa.seat.id AS seatId,
               sa.seat.label AS seatLabel,
               sa.seat.room.id AS roomId,
               sa.seat.room.name AS roomName,
               sa.examSchedule.subject.name AS subjectName
        FROM SeatAllocation sa
        WHERE sa.examSchedule.id IN :scheduleIds
          AND sa.student.id IN :studentIds
        """)
    List<AdmitCardSeatAllocationProjection> findAdmitCardAllocationsByScheduleIdsAndStudentIds(
        @Param("scheduleIds") Collection<Long> scheduleIds,
        @Param("studentIds") Collection<Long> studentIds);

    @Query("""
        SELECT sa.examSchedule.id AS scheduleId,
               sa.student.id AS studentId,
               sa.seat.id AS seatId,
               sa.seat.label AS seatLabel,
               sa.seat.room.id AS roomId,
               sa.seat.room.name AS roomName,
               sa.examSchedule.subject.name AS subjectName
        FROM SeatAllocation sa
        WHERE sa.examSchedule.id IN :scheduleIds
        """)
    List<AdmitCardSeatAllocationProjection> findAdmitCardAllocationsByScheduleIds(
        @Param("scheduleIds") Collection<Long> scheduleIds);

    // ── 14. Room occupancy details for mode and occupiedBy ─────────────────
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

    // ── 15. Conflict check: same seat, overlapping time, same exam schedule ──
    @Query("""
        SELECT COUNT(sa) > 0 FROM SeatAllocation sa
        WHERE sa.seat.id = :seatId
          AND sa.startTime < :endTime
          AND sa.endTime > :startTime
          AND sa.examSchedule.id = :examScheduleId
        """)
    boolean existsScheduleConflictOnSeat(
        @Param("seatId") Long seatId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("examScheduleId") Long examScheduleId);

    @Query("""
        SELECT DISTINCT sa.seat.id
        FROM SeatAllocation sa
        WHERE sa.seat.room.id = :roomId
          AND sa.startTime < :endTime
          AND sa.endTime > :startTime
          AND sa.examSchedule.id = :examScheduleId
        """)
    Set<Long> findSeatIdsAlreadyUsedByScheduleInRoom(
        @Param("roomId") Long roomId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("examScheduleId") Long examScheduleId);

    @Query("""
        SELECT sa.examSchedule.id AS examScheduleId,
               sa.student.id AS studentId,
               sa.student.rollNo AS rollNo,
               sa.student.userProfile.firstName AS firstName,
               sa.student.userProfile.lastName AS lastName,
               sa.student.section.academicClass.name AS className,
               sa.examSchedule.subject.name AS subjectName,
               sa.seat.rowNumber AS rowNumber,
               sa.seat.columnNumber AS columnNumber,
               sa.seat.label AS seatNumber,
               ea.status AS attendanceStatus,
               ea.malpracticeReported AS malpractice,
                ea.finalized AS finalized,
                COALESCE(eed.allowed, true) AS entryAllowed
        FROM SeatAllocation sa
        LEFT JOIN ExamAttendance ea
               ON ea.examSchedule.id = sa.examSchedule.id
              AND ea.student.id = sa.student.id
              AND ea.room.id = sa.seat.room.id
        LEFT JOIN ExamEntryDecision eed
               ON eed.examSchedule.id = sa.examSchedule.id
              AND eed.student.id = sa.student.id
        WHERE sa.seat.room.id = :roomId
          AND sa.startTime < :endTime
          AND sa.endTime > :startTime
        ORDER BY sa.seat.rowNumber ASC,
                 sa.seat.columnNumber ASC,
                 sa.positionIndex ASC
        """)
    List<ExamRoomStudentProjection> findExamRoomStudentsByTimeWindow(@Param("roomId") Long roomId,
                                                                     @Param("startTime") LocalDateTime startTime,
                                                                     @Param("endTime") LocalDateTime endTime);

    @Query("""
        SELECT sa.student.id
        FROM SeatAllocation sa
        WHERE sa.seat.room.id = :roomId
          AND sa.startTime < :endTime
          AND sa.endTime > :startTime
        """)
    List<Long> findExamRoomStudentIdsByTimeWindow(@Param("roomId") Long roomId,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    @Query("""
        SELECT sa.student.id AS studentId,
               sa.examSchedule.id AS examScheduleId
        FROM SeatAllocation sa
        WHERE sa.seat.room.id = :roomId
          AND sa.startTime < :endTime
          AND sa.endTime > :startTime
          AND sa.student.id IN :studentIds
        """)
    List<RoomStudentScheduleProjection> findStudentSchedulesInRoomByTimeWindowAndStudentIds(@Param("roomId") Long roomId,
                                                                                             @Param("startTime") LocalDateTime startTime,
                                                                                             @Param("endTime") LocalDateTime endTime,
                                                                                             @Param("studentIds") Collection<Long> studentIds);

    @Query("SELECT sa.examSchedule.exam.id FROM SeatAllocation sa WHERE sa.id = :allocationId")
    Optional<Long> findExamIdByAllocationId(@Param("allocationId") Long allocationId);

    @Query("""
        SELECT sa.seat.room.id AS roomId,
               sa.seat.room.name AS roomName,
               COUNT(sa.id) AS allocatedCount,
               COUNT(ea.id) AS markedCount
        FROM SeatAllocation sa
        LEFT JOIN ExamAttendance ea
               ON ea.examSchedule.id = sa.examSchedule.id
              AND ea.student.id = sa.student.id
              AND ea.room.id = sa.seat.room.id
        WHERE sa.examSchedule.exam.id = :examId
        GROUP BY sa.seat.room.id, sa.seat.room.name
        ORDER BY sa.seat.room.name ASC
        """)
    List<ExamControllerRoomSummaryProjection> findExamControllerRoomSummariesByExamId(@Param("examId") Long examId);

    @Query("""
        SELECT sa.examSchedule.id AS examScheduleId,
               sa.seat.room.id AS roomId,
               sa.seat.room.name AS roomName,
               sa.student.id AS studentId,
               sa.student.rollNo AS rollNo,
               sa.student.userProfile.firstName AS firstName,
               sa.student.userProfile.lastName AS lastName,
               sa.student.section.academicClass.name AS className,
               sa.examSchedule.subject.name AS subjectName,
               sa.seat.rowNumber AS rowNumber,
               sa.seat.columnNumber AS columnNumber,
               sa.seat.label AS seatNumber,
               ea.status AS attendanceStatus,
               COALESCE(eed.allowed, true) AS entryAllowed
        FROM SeatAllocation sa
        LEFT JOIN ExamAttendance ea
               ON ea.examSchedule.id = sa.examSchedule.id
              AND ea.student.id = sa.student.id
              AND ea.room.id = sa.seat.room.id
        LEFT JOIN ExamEntryDecision eed
               ON eed.examSchedule.id = sa.examSchedule.id
              AND eed.student.id = sa.student.id
        WHERE sa.examSchedule.exam.id = :examId
        ORDER BY sa.student.section.academicClass.name ASC,
                 sa.student.rollNo ASC,
                 sa.seat.room.name ASC,
                 sa.examSchedule.id ASC
        """)
    List<ExamControllerStudentSeatProjection> findExamControllerStudentRowsByExamId(@Param("examId") Long examId);
}
