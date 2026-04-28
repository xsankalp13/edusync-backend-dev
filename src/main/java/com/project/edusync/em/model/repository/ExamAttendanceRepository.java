package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.ExamAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ExamAttendanceRepository extends JpaRepository<ExamAttendance, Long> {

    @Query("""
        SELECT ea FROM ExamAttendance ea
        JOIN FETCH ea.student st
        JOIN FETCH st.userProfile up
        JOIN FETCH st.section sec
        JOIN FETCH sec.academicClass ac
        WHERE ea.examSchedule.id = :examScheduleId
          AND ea.room.id = :roomId
        """)
    List<ExamAttendance> findByExamScheduleIdAndRoomIdWithStudent(@Param("examScheduleId") Long examScheduleId,
                                                                  @Param("roomId") Long roomId);

    @Query("""
        SELECT ea FROM ExamAttendance ea
        WHERE ea.examSchedule.id = :examScheduleId
          AND ea.student.id IN :studentIds
        """)
    List<ExamAttendance> findByExamScheduleIdAndStudentIds(@Param("examScheduleId") Long examScheduleId,
                                                           @Param("studentIds") Collection<Long> studentIds);

    @Query("""
        SELECT ea FROM ExamAttendance ea
        WHERE ea.examSchedule.id IN :examScheduleIds
          AND ea.student.id IN :studentIds
        """)
    List<ExamAttendance> findByExamScheduleIdsAndStudentIds(@Param("examScheduleIds") Collection<Long> examScheduleIds,
                                                            @Param("studentIds") Collection<Long> studentIds);

    boolean existsByExamScheduleIdAndRoomIdAndFinalizedTrue(Long examScheduleId, Long roomId);

    boolean existsByExamScheduleIdInAndRoomIdAndFinalizedTrue(Collection<Long> examScheduleIds, Long roomId);
}

