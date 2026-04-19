package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.dto.statutory.PtSlabDTO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "hrms_statutory_configs")
@Getter
@Setter
@NoArgsConstructor
public class StatutoryConfig extends AuditableEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "financial_year", nullable = false, length = 10, unique = true)
    private String financialYear;

    @Column(name = "pf_applicable", nullable = false)
    private boolean pfApplicable = false;

    @Column(name = "pf_employee_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal pfEmployeeRate = BigDecimal.ZERO;

    @Column(name = "pf_employer_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal pfEmployerRate = BigDecimal.ZERO;

    @Column(name = "pf_ceiling_amount", precision = 14, scale = 2)
    private BigDecimal pfCeilingAmount;

    @Column(name = "esi_applicable", nullable = false)
    private boolean esiApplicable = false;

    @Column(name = "esi_employee_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal esiEmployeeRate = BigDecimal.ZERO;

    @Column(name = "esi_employer_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal esiEmployerRate = BigDecimal.ZERO;

    @Column(name = "esi_wage_limit", precision = 14, scale = 2)
    private BigDecimal esiWageLimit;

    @Column(name = "pt_applicable", nullable = false)
    private boolean ptApplicable = false;

    @Column(name = "pt_state", length = 40)
    private String ptState;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "pt_slabs", columnDefinition = "jsonb")
    private List<PtSlabDTO> ptSlabs;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

