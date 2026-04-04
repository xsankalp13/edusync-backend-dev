package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.Invigilation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InvigilationRepository extends JpaRepository<Invigilation, Long> {
    boolean existsByExamScheduleIdAndStaffId(Long examScheduleId, Long staffId);
    boolean existsByExamScheduleIdAndRole(Long examScheduleId, com.project.edusync.em.model.enums.InvigilationRole role);
    List<Invigilation> findByStaffIdAndExamSchedule_TimeslotId(Long staffId, Long timeslotId);
    List<Invigilation> findByExamScheduleId(Long examScheduleId);
    List<Invigilation> findByStaffId(Long staffId);
}

