package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.hrms.dto.statutory.*;
import com.project.edusync.hrms.model.entity.StatutoryConfig;
import com.project.edusync.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service("statutoryService")
@RequiredArgsConstructor
public class StatutoryServiceImpl {

    private final StatutoryConfigRepository configRepo;
    private final PayrollRunRepository payrollRunRepo;
    private final PayslipRepository payslipRepo;

    @Transactional
    public StatutoryConfigDTO createOrUpdateConfig(StatutoryConfigDTO dto) {
        StatutoryConfig cfg = configRepo.findByFinancialYearAndActiveTrue(dto.financialYear())
                .orElse(new StatutoryConfig());
        cfg.setFinancialYear(dto.financialYear());
        cfg.setPfApplicable(dto.pfApplicable());
        cfg.setPfEmployeeRate(nvl(dto.pfEmployeeRate()));
        cfg.setPfEmployerRate(nvl(dto.pfEmployerRate()));
        cfg.setPfCeilingAmount(dto.pfCeilingAmount());
        cfg.setEsiApplicable(dto.esiApplicable());
        cfg.setEsiEmployeeRate(nvl(dto.esiEmployeeRate()));
        cfg.setEsiEmployerRate(nvl(dto.esiEmployerRate()));
        cfg.setEsiWageLimit(dto.esiWageLimit());
        cfg.setPtApplicable(dto.ptApplicable());
        cfg.setPtState(dto.ptState());
        cfg.setPtSlabs(dto.ptSlabs());
        cfg.setActive(true);
        return toDTO(configRepo.save(cfg));
    }

    @Transactional(readOnly = true)
    public StatutoryConfigDTO getConfig(String financialYear) {
        return toDTO(configRepo.findByFinancialYearAndActiveTrue(financialYear)
                .orElseThrow(() -> new ResourceNotFoundException("Config not found for FY: " + financialYear)));
    }

    @Transactional(readOnly = true)
    public PfReportDTO getPfReport(int month, int year, String financialYear) {
        StatutoryConfig cfg = configRepo.findByFinancialYearAndActiveTrue(financialYear)
                .orElseThrow(() -> new ResourceNotFoundException("Statutory config not found for FY: " + financialYear));
        if (!cfg.isPfApplicable())
            throw new EdusyncException("PF is not applicable for FY " + financialYear, HttpStatus.BAD_REQUEST);

        List<Object[]> rows = payslipRepo.findPayslipSummaryByMonthYear(month, year);
        List<PfReportDTO.PfReportRow> reportRows = rows.stream().map(r -> {
            BigDecimal gross = toBd(r[3]);
            BigDecimal pfWages = cfg.getPfCeilingAmount() != null ? gross.min(cfg.getPfCeilingAmount()) : gross;
            BigDecimal empEe = pfWages.multiply(cfg.getPfEmployeeRate());
            BigDecimal empEr = pfWages.multiply(cfg.getPfEmployerRate());
            return new PfReportDTO.PfReportRow((String) r[0], (String) r[1], gross, pfWages, empEe, empEr);
        }).collect(Collectors.toList());
        return new PfReportDTO(month, year, reportRows);
    }

    @Transactional(readOnly = true)
    public EsiReportDTO getEsiReport(int month, int year, String financialYear) {
        StatutoryConfig cfg = configRepo.findByFinancialYearAndActiveTrue(financialYear)
                .orElseThrow(() -> new ResourceNotFoundException("Statutory config not found for FY: " + financialYear));
        if (!cfg.isEsiApplicable())
            throw new EdusyncException("ESI is not applicable for FY " + financialYear, HttpStatus.BAD_REQUEST);
        List<Object[]> rows = payslipRepo.findPayslipSummaryByMonthYear(month, year);
        List<EsiReportDTO.EsiReportRow> reportRows = rows.stream()
                .filter(r -> cfg.getEsiWageLimit() == null || toBd(r[3]).compareTo(cfg.getEsiWageLimit()) <= 0)
                .map(r -> {
                    BigDecimal gross = toBd(r[3]);
                    return new EsiReportDTO.EsiReportRow((String) r[0], (String) r[1], gross,
                            gross.multiply(cfg.getEsiEmployeeRate()), gross.multiply(cfg.getEsiEmployerRate()));
                }).collect(Collectors.toList());
        return new EsiReportDTO(month, year, reportRows);
    }

    @Transactional(readOnly = true)
    public PtReportDTO getPtReport(int month, int year, String financialYear) {
        StatutoryConfig cfg = configRepo.findByFinancialYearAndActiveTrue(financialYear)
                .orElseThrow(() -> new ResourceNotFoundException("Statutory config not found for FY: " + financialYear));
        if (!cfg.isPtApplicable())
            throw new EdusyncException("PT is not applicable for FY " + financialYear, HttpStatus.BAD_REQUEST);
        List<Object[]> rows = payslipRepo.findPayslipSummaryByMonthYear(month, year);
        List<PtSlabDTO> slabs = cfg.getPtSlabs() != null ? cfg.getPtSlabs() : List.of();
        List<PtReportDTO.PtReportRow> reportRows = rows.stream().map(r -> {
            BigDecimal gross = toBd(r[3]);
            BigDecimal pt = slabs.stream()
                    .filter(s -> gross.compareTo(s.minSalary()) >= 0
                            && (s.maxSalary() == null || gross.compareTo(s.maxSalary()) <= 0))
                    .map(PtSlabDTO::monthlyTax).findFirst().orElse(BigDecimal.ZERO);
            return new PtReportDTO.PtReportRow((String) r[0], (String) r[1], gross, pt);
        }).collect(Collectors.toList());
        return new PtReportDTO(month, year, cfg.getPtState(), reportRows);
    }

    @Transactional(readOnly = true)
    public SalaryRegisterDTO getSalaryRegister(int month, int year) {
        List<Object[]> rows = payslipRepo.findPayslipSummaryByMonthYear(month, year);
        List<SalaryRegisterDTO.SalaryRegisterRow> reportRows = rows.stream().map(r ->
                new SalaryRegisterDTO.SalaryRegisterRow((String) r[0], (String) r[1], (String) r[2],
                        toBd(r[3]), toBd(r[4]), toBd(r[5]))).collect(Collectors.toList());
        return new SalaryRegisterDTO(month, year, reportRows);
    }

    @Transactional(readOnly = true)
    public HeadcountReportDTO getHeadcountReport() {
        LocalDate today = LocalDate.now();
        List<Object[]> rows = payslipRepo.findActiveStaffHeadcount();
        int total = rows.size();
        int teaching = (int) rows.stream().filter(r -> "TEACHING".equals(r[3])).count();
        int admin = (int) rows.stream().filter(r -> "NON_TEACHING_ADMIN".equals(r[3])).count();
        int support = (int) rows.stream().filter(r -> "NON_TEACHING_SUPPORT".equals(r[3])).count();
        List<HeadcountReportDTO.HeadcountRow> reportRows = rows.stream().map(r ->
                new HeadcountReportDTO.HeadcountRow((String) r[0], (String) r[1], (String) r[3], (String) r[4],
                        r[5] != null ? LocalDate.parse(r[5].toString()) : null)).collect(Collectors.toList());
        return new HeadcountReportDTO(today, total, teaching, admin, support, reportRows);
    }

    @Transactional(readOnly = true)
    public TdsReportDTO getTdsReport(int month, int year) {
        List<Object[]> rows = payslipRepo.findPayslipSummaryByMonthYear(month, year);
        List<TdsReportDTO.TdsReportRow> reportRows = rows.stream().map(r ->
                new TdsReportDTO.TdsReportRow((String) r[0], (String) r[1], toBd(r[3]), toBd(r[6]))).collect(Collectors.toList());
        return new TdsReportDTO(month, year, reportRows);
    }

    @Transactional(readOnly = true)
    public ComplianceSummaryDTO getComplianceSummary(String financialYear) {
        StatutoryConfig cfg = configRepo.findByFinancialYearAndActiveTrue(financialYear).orElse(null);
        return new ComplianceSummaryDTO(financialYear,
                cfg != null && cfg.isPfApplicable(), BigDecimal.ZERO,
                cfg != null && cfg.isEsiApplicable(), BigDecimal.ZERO,
                cfg != null && cfg.isPtApplicable(), BigDecimal.ZERO,
                0, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public Form16DataDTO getForm16Data(String financialYear) {
        // financialYear format: "2025-2026"
        String[] parts = financialYear.split("-");
        int fromYear = Integer.parseInt(parts[0].trim());
        int toYear = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : fromYear + 1;
        List<Object[]> rows = payslipRepo.findAnnualSummaryByFinancialYear(fromYear, toYear);
        List<Form16DataDTO.Form16Row> reportRows = rows.stream().map(r ->
                new Form16DataDTO.Form16Row((String) r[0], (String) r[1], toBd(r[2]), toBd(r[3]),
                        new BigDecimal("50000"), toBd(r[2]).subtract(new BigDecimal("50000")).max(BigDecimal.ZERO)))
                .collect(Collectors.toList());
        return new Form16DataDTO(financialYear, reportRows);
    }

    @Transactional(readOnly = true)
    public AttritionReportDTO getAttritionReport(LocalDate from, LocalDate to) {
        List<Object[]> separations = payslipRepo.findSeparationsByDateRange(from, to);
        List<Object[]> hirings = payslipRepo.findHiringsByDateRange(from, to);
        int separated = separations.size();
        int hired = hirings.size();
        double rate = separated > 0 ? (double) separated / Math.max(1, separated + hired) * 100 : 0;
        List<AttritionReportDTO.AttritionRow> rows = separations.stream().map(r ->
                new AttritionReportDTO.AttritionRow((String) r[0], (String) r[1],
                        r[2] != null ? LocalDate.parse(r[2].toString()) : null, (String) r[3])).collect(Collectors.toList());
        return new AttritionReportDTO(from, to, hired, separated, rate, rows);
    }

    private StatutoryConfigDTO toDTO(StatutoryConfig c) {
        return new StatutoryConfigDTO(c.getFinancialYear(), c.isPfApplicable(), c.getPfEmployeeRate(),
                c.getPfEmployerRate(), c.getPfCeilingAmount(), c.isEsiApplicable(), c.getEsiEmployeeRate(),
                c.getEsiEmployerRate(), c.getEsiWageLimit(), c.isPtApplicable(), c.getPtState(), c.getPtSlabs());
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private BigDecimal toBd(Object v) { return v instanceof BigDecimal bd ? bd : v instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO; }
}


