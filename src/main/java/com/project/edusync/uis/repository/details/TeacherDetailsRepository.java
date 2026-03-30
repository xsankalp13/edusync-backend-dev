package com.project.edusync.uis.repository.details;

import com.project.edusync.uis.model.entity.details.TeacherDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeacherDetailsRepository extends JpaRepository<TeacherDetails, Long> {

    Optional<TeacherDetails> findByStaff_Id(Long staffId);

    @Query("SELECT t FROM TeacherDetails t WHERE t.id = :teacherId AND t.staff.isActive = true")
    Optional<TeacherDetails> findActiveById(Long teacherId);

    @Query("SELECT DISTINCT t FROM TeacherDetails t " +
            "JOIN t.teachableSubjects ts " +
            "WHERE ts.uuid = :subjectId AND ts.isActive = true AND t.staff.isActive = true")
    List<TeacherDetails> findQualifiedTeachersForSubject(UUID subjectId);

    @Query("SELECT DISTINCT t FROM TeacherDetails t JOIN FETCH t.teachableSubjects ts WHERE t.staff.isActive = true AND ts.isActive = true")
    List<TeacherDetails> findAllActiveWithSubjects();

    @Query("""
            SELECT DISTINCT t FROM TeacherDetails t
            JOIN t.teachableSubjects ts
            WHERE ts.uuid = :subjectId
              AND t.staff.isActive = true
              AND NOT EXISTS (
                SELECT 1 FROM Schedule s
                JOIN s.timeslot sts
                WHERE s.teacher.id = t.id
                  AND s.isActive = true
                  AND sts.dayOfWeek = (SELECT dt.dayOfWeek FROM Timeslot dt WHERE dt.uuid = :timeslotId)
                  AND sts.startTime = (SELECT dt.startTime FROM Timeslot dt WHERE dt.uuid = :timeslotId)
              )
            """)
    List<TeacherDetails> findAvailableTeachersForSlot(UUID subjectId, UUID timeslotId);
}
