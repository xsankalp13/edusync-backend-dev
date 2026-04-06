package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.Payslip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    Optional<Payslip> findByIdAndActiveTrue(Long payslipId);

    Page<Payslip> findByPayrollRun_IdAndActiveTrue(Long payrollRunId, Pageable pageable);

    List<Payslip> findByPayrollRun_IdAndActiveTrue(Long payrollRunId);

    Page<Payslip> findByStaff_IdAndActiveTrueOrderByPayYearDescPayMonthDesc(Long staffId, Pageable pageable);

    Optional<Payslip> findByUuid(java.util.UUID uuid);
}


