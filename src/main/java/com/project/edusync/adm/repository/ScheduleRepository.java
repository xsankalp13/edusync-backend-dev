package com.project.edusync.adm.repository;

import com.project.edusync.adm.model.entity.Schedule;
import com.project.edusync.adm.model.entity.Timeslot;
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

    interface TeacherSubjectPairProjection {
        Long getTeacherId();

        String getFirstName();

        String getLastName();

        UUID getSubjectUuid();
    }

    interface SubjectScheduledPeriodsProjection {
        UUID getSubjectId();

        Long getScheduledPeriods();
    }

    @Query("SELECT s FROM Schedule s WHERE s.uuid = :scheduleId AND s.isActive = true")
    Optional<Schedule> findActiveById(UUID scheduleId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Schedule s WHERE s.uuid = :scheduleId AND s.isActive = true")
    boolean existsActiveById(UUID scheduleId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Schedule s WHERE s.room.uuid = :roomId AND s.isActive = true")
    boolean existsActiveByRoomUuid(UUID roomId);

    @Transactional
    @Modifying
    @Query("UPDATE Schedule s SET s.isActive = false WHERE s.uuid = :scheduleId")
    void softDeleteById(UUID scheduleId);

    @Transactional
    @Modifying
    @Query("UPDATE Schedule s SET s.isActive = false WHERE s.section.uuid = :sectionId AND s.isActive = true")
    void softDeleteBySectionId(@Param("sectionId") UUID sectionId);

    @Query("SELECT s FROM Schedule s WHERE s.isActive = true")
    List<Schedule> findAllActive();

    @Query("SELECT s FROM Schedule s WHERE s.section.uuid = :sectionId AND s.isActive = true")
    List<Schedule> findAllActiveBySectionUuid(UUID sectionId);

    @Query("""
            SELECT s.subject.uuid as subjectId,
                   COUNT(s.id) as scheduledPeriods
            FROM Schedule s
            WHERE s.isActive = true
              AND s.section.academicClass.uuid = :classId
              AND s.subject.uuid IN :subjectIds
            GROUP BY s.subject.uuid
            """)
    List<SubjectScheduledPeriodsProjection> findScheduledPeriodsByClassAndSubjectIds(UUID classId, List<UUID> subjectIds);

    @Query("""
            SELECT s FROM Schedule s
            JOIN FETCH s.subject sub
            JOIN FETCH s.teacher td
            JOIN FETCH s.room room
            JOIN FETCH s.timeslot ts
            WHERE s.section.uuid = :sectionId
              AND s.isActive = true
            """)
    List<Schedule> findAllActiveWithReferencesBySectionUuid(UUID sectionId);

    @Query("""
            SELECT td.id as teacherId,
                   up.firstName as firstName,
                   up.lastName as lastName,
                   sub.uuid as subjectUuid
            FROM Schedule s
            JOIN s.teacher td
            JOIN td.staff st
            JOIN st.userProfile up
            JOIN s.subject sub
            WHERE s.isActive = true
            GROUP BY td.id, up.firstName, up.lastName, sub.uuid
            ORDER BY up.firstName ASC, up.lastName ASC
            """)
    List<TeacherSubjectPairProjection> findTeacherSubjectPairs();

    @Query(value = """
            SELECT ac.uuid as classId,
                   ac.name as className,
                   sec.uuid as sectionId,
                   sec.section_name as sectionName,
                   CASE
                       WHEN count(s.id) = 0 THEN 'MISSING'
                       WHEN sum(case when s.status = 2 then 1 else 0 end) > 0 THEN 'PUBLISHED'
                       WHEN sum(case when s.status = 1 then 1 else 0 end) > 0 THEN 'DRAFT'
                       ELSE 'DRAFT'
                   END as scheduleStatus,
                   count(s.id) as totalPeriods,
                   COALESCE(MIN(s.created_at), sec.created_at) as createdAt,
                   COALESCE(MAX(s.updated_at), sec.updated_at) as lastUpdatedAt
            FROM sections sec
            JOIN classes ac ON sec.class_id = ac.id
            LEFT JOIN schedule s ON s.section_id = sec.id AND s.is_active = true
            WHERE sec.is_active = true
            GROUP BY ac.uuid, ac.name, sec.uuid, sec.section_name, sec.created_at, sec.updated_at
            ORDER BY ac.name ASC, sec.section_name ASC
            """, nativeQuery = true)
    List<TimetableOverviewProjection> findTimetableOverview();

    // --- Conflict Detection Queries (Now returning the conflicting schedule for better error messages) ---

    @Query("""
            SELECT s FROM Schedule s
            JOIN FETCH s.timeslot ts
            JOIN FETCH s.section sec
            JOIN FETCH sec.academicClass ac
            WHERE s.teacher.id = :teacherId
              AND s.isActive = true
              AND (:scheduleId IS NULL OR s.uuid != :scheduleId)
              AND (:excludeSectionId IS NULL OR s.section.uuid != :excludeSectionId)
              AND ts.uuid = :timeslotId
            """)
    Optional<Schedule> findTeacherConflict(Long teacherId, UUID timeslotId, UUID scheduleId, UUID excludeSectionId);

    @Query("""
            SELECT s FROM Schedule s
            JOIN FETCH s.timeslot ts
            JOIN FETCH s.section sec
            JOIN FETCH sec.academicClass ac
            WHERE s.room.uuid = :roomId
              AND s.isActive = true
              AND (:scheduleId IS NULL OR s.uuid != :scheduleId)
              AND (:excludeSectionId IS NULL OR s.section.uuid != :excludeSectionId)
              AND ts.uuid = :timeslotId
            """)
    Optional<Schedule> findRoomConflict(UUID roomId, UUID timeslotId, UUID scheduleId, UUID excludeSectionId);

    @Query("""
            SELECT s FROM Schedule s
            JOIN FETCH s.timeslot ts
            WHERE s.section.uuid = :sectionId
              AND s.isActive = true
              AND (:scheduleId IS NULL OR s.uuid != :scheduleId)
              AND (:excludeSectionId IS NULL OR s.section.uuid != :excludeSectionId)
              AND ts.uuid = :timeslotId
            """)
    Optional<Schedule> findSectionConflict(UUID sectionId, UUID timeslotId, UUID scheduleId, UUID excludeSectionId);

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

    @Query("""
            SELECT COUNT(DISTINCT s.section.id)
            FROM Schedule s
            WHERE s.teacher.id = :teacherId
              AND s.isActive = true
            """)
    long countDistinctActiveSectionsByTeacherId(@Param("teacherId") Long teacherId);

    @Query("""
            SELECT DISTINCT s.timeslot
            FROM Schedule s
            WHERE s.teacher.id = :teacherId
              AND s.isActive = true
            """)
    List<Timeslot> findDistinctActiveTimeslotsByTeacherId(@Param("teacherId") Long teacherId);
}