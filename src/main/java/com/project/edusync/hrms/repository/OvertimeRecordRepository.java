package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.OvertimeRecord;
import com.project.edusync.hrms.model.enums.OvertimeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OvertimeRecordRepository extends JpaRepository<OvertimeRecord, Long> {
    Optional<OvertimeRecord> findByUuid(UUID uuid);
    List<OvertimeRecord> findByStaff_Id(Long staffId);
    List<OvertimeRecord> findByStaff_IdAndStatus(Long staffId, OvertimeStatus status);
    List<OvertimeRecord> findByStatus(OvertimeStatus status);
    
    List<OvertimeRecord> findByStaff_IdAndStatusAndCompensationTypeAndWorkDateBetweenAndActiveTrue(
            Long staffId, OvertimeStatus status, String compensationType, 
            java.time.LocalDate startDate, java.time.LocalDate endDate);

    List<OvertimeRecord> findByPayrollRunRefAndStatusAndActiveTrue(UUID payrollRunRef, OvertimeStatus status);
}

