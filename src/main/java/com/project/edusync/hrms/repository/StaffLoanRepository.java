package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.StaffLoan;
import com.project.edusync.hrms.model.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffLoanRepository extends JpaRepository<StaffLoan, Long> {
    Optional<StaffLoan> findByUuid(UUID uuid);
    List<StaffLoan> findByStaff_Id(Long staffId);
    List<StaffLoan> findByStaff_IdAndStatus(Long staffId, LoanStatus status);
    List<StaffLoan> findByStatusIn(List<LoanStatus> statuses);
    List<StaffLoan> findByStaff_IdAndStatusIn(Long staffId, List<LoanStatus> statuses);
}

