package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.PayrollEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayrollEntryRepository extends JpaRepository<PayrollEntry, Long> {

    List<PayrollEntry> findByPayrollRun_IdAndActiveTrue(Long runId);

    List<PayrollEntry> findByPayrollRun_IdAndActiveTrueOrderByStaff_IdAsc(Long runId);
}


