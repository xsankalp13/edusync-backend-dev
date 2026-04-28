package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.PayrollRun;
import com.project.edusync.hrms.model.enums.PayrollRunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {

    boolean existsByPayYearAndPayMonthAndActiveTrue(Integer payYear, Integer payMonth);

    Page<PayrollRun> findByActiveTrueOrderByPayYearDescPayMonthDesc(Pageable pageable);

    Optional<PayrollRun> findByIdAndActiveTrue(Long runId);

    Optional<PayrollRun> findByUuid(java.util.UUID uuid);

    @Query("""
            SELECT COALESCE(SUM(r.totalNet), 0)
            FROM PayrollRun r
            WHERE r.active = true
              AND r.payYear = :payYear
              AND r.payMonth = :payMonth
              AND r.status IN :statuses
            """)
    BigDecimal sumTotalNetByMonthAndStatuses(
            @Param("payYear") Integer payYear,
            @Param("payMonth") Integer payMonth,
            @Param("statuses") Collection<PayrollRunStatus> statuses
    );

    /**
     * Projection for grouped monthly payroll sums.
     */
    interface MonthlyPayrollSumProjection {
        Integer getPayYear();
        Integer getPayMonth();
        java.math.BigDecimal getTotalNet();
    }

    /**
     * Returns (payYear, payMonth, totalNet) for each month in the range.
     * Replaces the 6-iteration payroll loop — 1 query instead of 6.
     */
    @Query("""
            SELECT r.payYear as payYear, r.payMonth as payMonth,
                   COALESCE(SUM(r.totalNet), 0) as totalNet
            FROM PayrollRun r
            WHERE r.active = true
              AND r.status IN :statuses
              AND (r.payYear > :startYear
                   OR (r.payYear = :startYear AND r.payMonth >= :startMonth))
              AND (r.payYear < :endYear
                   OR (r.payYear = :endYear AND r.payMonth <= :endMonth))
            GROUP BY r.payYear, r.payMonth
            ORDER BY r.payYear, r.payMonth
            """)
    List<MonthlyPayrollSumProjection> sumPayrollGroupedByMonth(
            @Param("startYear") int startYear,
            @Param("startMonth") int startMonth,
            @Param("endYear") int endYear,
            @Param("endMonth") int endMonth,
            @Param("statuses") Collection<PayrollRunStatus> statuses
    );
}


