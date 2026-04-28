package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.Invigilation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InvigilationRepository extends JpaRepository<Invigilation, Long> {
    interface InvigilatorRoomProjection {
        Long getExamScheduleId();
        Long getRoomId();
        String getRoomName();
        String getSubjectName();
        String getClassName();
        java.time.LocalDate getExamDate();
        java.time.LocalTime getStartTime();
        java.time.LocalTime getEndTime();
    }

    boolean existsByExamScheduleIdAndStaffId(Long examScheduleId, Long staffId);
    boolean existsByExamScheduleIdAndRole(Long examScheduleId, com.project.edusync.em.model.enums.InvigilationRole role);
    boolean existsByExamScheduleIdAndRoom_IdAndStaffId(Long examScheduleId, Long roomId, Long staffId);
    List<Invigilation> findByStaffIdAndExamSchedule_TimeslotId(Long staffId, Long timeslotId);
    List<Invigilation> findByExamScheduleId(Long examScheduleId);
    List<Invigilation> findByStaffId(Long staffId);

    @Query("""
        SELECT i.examSchedule.id AS examScheduleId,
               i.room.id AS roomId,
               i.room.name AS roomName,
               i.examSchedule.subject.name AS subjectName,
               i.examSchedule.academicClass.name AS className,
               i.examSchedule.examDate AS examDate,
               i.examSchedule.timeslot.startTime AS startTime,
               i.examSchedule.timeslot.endTime AS endTime
        FROM Invigilation i
        WHERE i.staff.id = :staffId
        ORDER BY i.examSchedule.examDate ASC, i.examSchedule.timeslot.startTime ASC
        """)
    List<InvigilatorRoomProjection> findAssignedRoomsByStaffId(@Param("staffId") Long staffId);

    @Query("SELECT i.examSchedule.exam.id FROM Invigilation i WHERE i.id = :invigilationId")
    java.util.Optional<Long> findExamIdByInvigilationId(@Param("invigilationId") Long invigilationId);
}
