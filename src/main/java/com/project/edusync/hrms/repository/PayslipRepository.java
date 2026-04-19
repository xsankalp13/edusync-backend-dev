package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.Payslip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    Optional<Payslip> findByIdAndActiveTrue(Long payslipId);
    Page<Payslip> findByPayrollRun_IdAndActiveTrue(Long payrollRunId, Pageable pageable);
    List<Payslip> findByPayrollRun_IdAndActiveTrue(Long payrollRunId);
    Page<Payslip> findByStaff_IdAndActiveTrueOrderByPayYearDescPayMonthDesc(Long staffId, Pageable pageable);
    Optional<Payslip> findByUuid(java.util.UUID uuid);

    /**
     * Returns [employeeId, staffName, designation, grossPay, totalDeductions, netPay, tdsDeducted(0)]
     * for all active payslips in the given month/year.
     */
    @Query("""
        SELECT s.employeeId, CONCAT(up.firstName, ' ', up.lastName),
               COALESCE(sd.designationName, s.jobTitle),
               p.grossPay, p.totalDeductions, p.netPay, 0
        FROM Payslip p
        JOIN p.staff s
        JOIN s.userProfile up
        LEFT JOIN s.designation sd
        WHERE p.payMonth = :month AND p.payYear = :year AND p.active = true
    """)
    List<Object[]> findPayslipSummaryByMonthYear(@Param("month") int month, @Param("year") int year);

    /**
     * Returns [employeeId, staffName, category, designation, hireDate]
     */
    @Query("""
        SELECT s.employeeId, CONCAT(up.firstName, ' ', up.lastName),
               s.category, COALESCE(sd.designationName, s.jobTitle), s.hireDate
        FROM Staff s
        JOIN s.userProfile up
        LEFT JOIN s.designation sd
        WHERE s.isActive = true
    """)
    List<Object[]> findActiveStaffHeadcount();

    /**
     * Annual summary for Form 16: [employeeId, staffName, totalGross, totalTds(0)]
     */
    @Query("""
        SELECT s.employeeId, CONCAT(up.firstName, ' ', up.lastName),
               SUM(p.grossPay), 0
        FROM Payslip p
        JOIN p.staff s
        JOIN s.userProfile up
        WHERE p.active = true
          AND ((p.payMonth >= 4 AND p.payYear = :fromYear) OR (p.payMonth < 4 AND p.payYear = :toYear))
        GROUP BY s.employeeId, up.firstName, up.lastName
    """)
    List<Object[]> findAnnualSummaryByFinancialYear(@Param("fromYear") int fromYear, @Param("toYear") int toYear);

    /**
     * Returns [employeeId, staffName, terminationDate, exitReason] for completed exits.
     */
    @Query("""
        SELECT s.employeeId, CONCAT(up.firstName, ' ', up.lastName),
               s.terminationDate, 'RESIGNATION'
        FROM Staff s
        JOIN s.userProfile up
        WHERE s.terminationDate BETWEEN :from AND :to
    """)
    List<Object[]> findSeparationsByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Returns [employeeId, staffName, hireDate] for new hires.
     */
    @Query("""
        SELECT s.employeeId, CONCAT(up.firstName, ' ', up.lastName), s.hireDate
        FROM Staff s
        JOIN s.userProfile up
        WHERE s.hireDate BETWEEN :from AND :to
    """)
    List<Object[]> findHiringsByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}









