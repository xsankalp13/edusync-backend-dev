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
}


