package com.project.edusync.adm.repository;

import com.project.edusync.adm.model.entity.Schedule;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    interface TimetableOverviewProjection {
        UUID getClassId();

        String getClassName();

        UUID getSectionId();

        String getSectionName();

        String getScheduleStatus();

        Long getTotalPeriods();

        LocalDateTime getCreatedAt();

        LocalDateTime getLastUpdatedAt();
    }

    @Query("SELECT s FROM Schedule s WHERE s.uuid = :scheduleId AND s.isActive = true")
    Optional<Schedule> findActiveById(UUID scheduleId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Schedule s WHERE s.uuid = :scheduleId AND s.isActive = true")
    boolean existsActiveById(UUID scheduleId);

    @Transactional
    @Modifying
    @Query("UPDATE Schedule s SET s.isActive = false WHERE s.uuid = :scheduleId")
    void softDeleteById(UUID scheduleId);

    @Transactional
    @Modifying
    @Query("UPDATE Schedule s SET s.isActive = false WHERE s.section.uuid = :sectionId AND s.isActive = true")
    void softDeleteBySectionId(@Param("sectionId") UUID sectionId);

    @Query("SELECT s FROM Schedule s WHERE s.section.uuid = :sectionId AND s.isActive = true")
    List<Schedule> findAllActiveBySectionUuid(UUID sectionId);

    @Query("""
            SELECT ac.uuid as classId,
                   ac.name as className,
                   sec.uuid as sectionId,
                   sec.sectionName as sectionName,
                   CASE
                       WHEN COUNT(s) = 0 THEN 'MISSING'
                       WHEN SUM(CASE WHEN s.status = com.project.edusync.adm.model.enums.ScheduleStatus.PUBLISHED THEN 1 ELSE 0 END) > 0 THEN 'PUBLISHED'
                       WHEN SUM(CASE WHEN s.status = com.project.edusync.adm.model.enums.ScheduleStatus.DRAFT THEN 1 ELSE 0 END) > 0 THEN 'DRAFT'
                       ELSE 'DRAFT'
                   END as scheduleStatus,
                   COUNT(s) as totalPeriods,
                   COALESCE(MIN(s.createdAt), sec.createdAt) as createdAt,
                   COALESCE(MAX(s.updatedAt), sec.updatedAt) as lastUpdatedAt
            FROM Section sec
            JOIN sec.academicClass ac
            LEFT JOIN sec.schedules s ON s.isActive = true
            WHERE sec.isActive = true
            GROUP BY ac.uuid, ac.name, sec.uuid, sec.sectionName, sec.createdAt, sec.updatedAt
            ORDER BY ac.name ASC, sec.sectionName ASC
            """)
    List<TimetableOverviewProjection> findTimetableOverview();

    // --- Conflict Detection Queries ---

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Schedule s " +
            "WHERE s.teacher.id = :teacherId " +
            "AND s.timeslot.uuid = :timeslotId " +
            "AND s.isActive = true " +
            "AND (:scheduleId IS NULL OR s.uuid != :scheduleId)")
    boolean existsTeacherConflict(Long teacherId, UUID timeslotId, UUID scheduleId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Schedule s " +
            "WHERE s.room.uuid = :roomId " +
            "AND s.timeslot.uuid = :timeslotId " +
            "AND s.isActive = true " +
            "AND (:scheduleId IS NULL OR s.uuid != :scheduleId)")
    boolean existsRoomConflict(UUID roomId, UUID timeslotId, UUID scheduleId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Schedule s " +
            "WHERE s.section.uuid = :sectionId " +
            "AND s.timeslot.uuid = :timeslotId " +
            "AND s.isActive = true " +
            "AND (:scheduleId IS NULL OR s.uuid != :scheduleId)")
    boolean existsSectionConflict(UUID sectionId, UUID timeslotId, UUID scheduleId);

    @Query("""
            SELECT s FROM Schedule s
            JOIN FETCH s.subject sub
            JOIN FETCH s.room room
            JOIN FETCH s.timeslot ts
            WHERE s.section.id = :sectionId
              AND s.isActive = true
              AND ts.dayOfWeek = :dayOfWeek
              AND :currentTime BETWEEN ts.startTime AND ts.endTime
            ORDER BY ts.startTime ASC
            """)
    Optional<Schedule> findCurrentClass(Long sectionId, Short dayOfWeek, LocalTime currentTime);

    @Query("""
            SELECT s FROM Schedule s
            JOIN FETCH s.subject sub
            JOIN FETCH s.room room
            JOIN FETCH s.timeslot ts
            WHERE s.section.id = :sectionId
              AND s.isActive = true
              AND ts.dayOfWeek = :dayOfWeek
              AND ts.startTime > :currentTime
            ORDER BY ts.startTime ASC
            """)
    List<Schedule> findUpcomingClasses(Long sectionId, Short dayOfWeek, LocalTime currentTime, Pageable pageable);

    default Optional<Schedule> findNextClass(Long sectionId, Short dayOfWeek, LocalTime currentTime) {
        return findUpcomingClasses(sectionId, dayOfWeek, currentTime, Pageable.ofSize(1)).stream().findFirst();
    }

    @Query("""
            SELECT s FROM Schedule s
            JOIN FETCH s.subject sub
            JOIN FETCH s.room room
            JOIN FETCH s.timeslot ts
            JOIN FETCH s.teacher td
            JOIN FETCH td.staff st
            JOIN FETCH st.userProfile up
            WHERE s.section.id = :sectionId
              AND s.isActive = true
              AND ts.dayOfWeek = :dayOfWeek
            ORDER BY ts.startTime ASC
            """)
    List<Schedule> findDaySchedule(Long sectionId, Short dayOfWeek);
}