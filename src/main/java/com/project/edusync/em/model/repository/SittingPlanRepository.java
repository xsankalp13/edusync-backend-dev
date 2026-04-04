package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.SittingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SittingPlanRepository extends JpaRepository<SittingPlan, Long> {
    Optional<SittingPlan> findByExamScheduleIdAndStudentId(Long examScheduleId, Long studentId);
    boolean existsByExamScheduleIdAndRoomIdAndSeatNumber(Long examScheduleId, Long roomId, String seatNumber);
    long countByExamScheduleIdAndRoomId(Long examScheduleId, Long roomId);
    List<SittingPlan> findByExamScheduleId(Long examScheduleId);
    List<SittingPlan> findByRoomId(Long roomId);
}

