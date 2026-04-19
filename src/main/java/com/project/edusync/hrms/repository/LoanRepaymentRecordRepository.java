package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.LoanRepaymentRecord;
import com.project.edusync.hrms.model.enums.RepaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoanRepaymentRecordRepository extends JpaRepository<LoanRepaymentRecord, Long> {
    List<LoanRepaymentRecord> findByLoan_Id(Long loanId);
    List<LoanRepaymentRecord> findByLoan_IdAndStatus(Long loanId, RepaymentStatus status);
    List<LoanRepaymentRecord> findByPayrollRunRef(UUID payrollRunRef);
}

