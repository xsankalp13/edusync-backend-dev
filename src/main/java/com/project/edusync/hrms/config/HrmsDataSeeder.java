package com.project.edusync.hrms.config;

import com.project.edusync.hrms.model.entity.LeaveTypeConfig;
import com.project.edusync.hrms.model.entity.StaffGrade;
import com.project.edusync.hrms.model.entity.SalaryComponent;
import com.project.edusync.hrms.model.enums.SalaryCalculationMethod;
import com.project.edusync.hrms.model.enums.SalaryComponentType;
import com.project.edusync.hrms.model.enums.TeachingWing;
import com.project.edusync.hrms.repository.LeaveTypeConfigRepository;
import com.project.edusync.hrms.repository.StaffGradeRepository;
import com.project.edusync.hrms.repository.SalaryComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class HrmsDataSeeder implements ApplicationRunner {

    private final LeaveTypeConfigRepository leaveTypeConfigRepository;
    private final StaffGradeRepository staffGradeRepository;
    private final SalaryComponentRepository salaryComponentRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedDefaultLeaveTypes();
        seedDefaultStaffGrades();
        seedDefaultSalaryComponents();
    }

    private void seedDefaultLeaveTypes() {
        List<LeaveTypeSeed> seeds = List.of(
                new LeaveTypeSeed("CL", "Casual Leave", 12, false, 0, false, false, null, true, 1),
                new LeaveTypeSeed("SL", "Sick Leave / Medical Leave", 10, false, 0, false, true, 2, true, 2),
                new LeaveTypeSeed("EL", "Earned Leave", 15, true, 30, false, false, null, true, 3),
                new LeaveTypeSeed("ML", "Maternity Leave", 180, false, 0, false, true, 1, true, 4),
                new LeaveTypeSeed("PL", "Paternity Leave", 15, false, 0, false, false, null, true, 5),
                new LeaveTypeSeed("COMP", "Compensatory Off", 0, false, 0, false, false, null, true, 6),
                new LeaveTypeSeed("LOP", "Loss of Pay", 9999, false, 0, false, false, null, false, 7)
        );

        int inserted = 0;
        for (LeaveTypeSeed seed : seeds) {
            if (leaveTypeConfigRepository.findByLeaveCodeIgnoreCase(seed.code()).isPresent()) {
                continue;
            }

            LeaveTypeConfig entity = new LeaveTypeConfig();
            entity.setLeaveCode(seed.code());
            entity.setDisplayName(seed.displayName());
            entity.setAnnualQuota(seed.annualQuota());
            entity.setCarryForwardAllowed(seed.carryForwardAllowed());
            entity.setMaxCarryForward(seed.maxCarryForward());
            entity.setEncashmentAllowed(seed.encashmentAllowed());
            entity.setRequiresDocument(seed.requiresDocument());
            entity.setDocumentRequiredAfterDays(seed.documentRequiredAfterDays());
            entity.setPaid(seed.paid());
            entity.setSortOrder(seed.sortOrder());
            leaveTypeConfigRepository.save(entity);
            inserted++;
        }

        if (inserted > 0) {
            log.info("HRMS seeder inserted {} default leave types.", inserted);
        }
    }

    private void seedDefaultStaffGrades() {
        List<StaffGradeSeed> seeds = List.of(
                new StaffGradeSeed("PRT", "Primary Teacher", TeachingWing.PRIMARY, "25000", "45000", 1),
                new StaffGradeSeed("TGT", "Trained Graduate Teacher", TeachingWing.SECONDARY, "35000", "65000", 2),
                new StaffGradeSeed("PGT", "Post Graduate Teacher", TeachingWing.SENIOR_SECONDARY, "50000", "90000", 3),
                new StaffGradeSeed("HOD", "Head of Department", TeachingWing.SENIOR_SECONDARY, "70000", "120000", 4),
                new StaffGradeSeed("VP", "Vice Principal", TeachingWing.ALL, "90000", "150000", 5),
                new StaffGradeSeed("PRINCIPAL", "Principal", TeachingWing.ALL, "120000", "250000", 6)
        );

        int inserted = 0;
        for (StaffGradeSeed seed : seeds) {
            if (staffGradeRepository.findByGradeCodeIgnoreCase(seed.code()).isPresent()) {
                continue;
            }

            StaffGrade grade = new StaffGrade();
            grade.setGradeCode(seed.code());
            grade.setGradeName(seed.name());
            grade.setTeachingWing(seed.wing());
            grade.setPayBandMin(new BigDecimal(seed.minBand()));
            grade.setPayBandMax(new BigDecimal(seed.maxBand()));
            grade.setSortOrder(seed.sortOrder());
            grade.setActive(true);
            staffGradeRepository.save(grade);
            inserted++;
        }

        if (inserted > 0) {
            log.info("HRMS seeder inserted {} default staff grades.", inserted);
        }
    }

    private void seedDefaultSalaryComponents() {
        List<SalaryComponentSeed> seeds = List.of(
                new SalaryComponentSeed("BASIC", "Basic Pay", SalaryComponentType.EARNING, SalaryCalculationMethod.FIXED, "0", true, false, 1),
                new SalaryComponentSeed("HRA", "House Rent Allowance", SalaryComponentType.EARNING, SalaryCalculationMethod.PERCENTAGE_OF_BASIC, "40", true, false, 2),
                new SalaryComponentSeed("DA", "Dearness Allowance", SalaryComponentType.EARNING, SalaryCalculationMethod.PERCENTAGE_OF_BASIC, "12", true, false, 3),
                new SalaryComponentSeed("TA", "Transport Allowance", SalaryComponentType.EARNING, SalaryCalculationMethod.FIXED, "3000", false, false, 4),
                new SalaryComponentSeed("SA", "Special Allowance", SalaryComponentType.EARNING, SalaryCalculationMethod.FIXED, "0", true, false, 5),
                new SalaryComponentSeed("PF_EMP", "PF (Employee)", SalaryComponentType.DEDUCTION, SalaryCalculationMethod.PERCENTAGE_OF_BASIC, "12", false, true, 6),
                new SalaryComponentSeed("ESI", "ESI", SalaryComponentType.DEDUCTION, SalaryCalculationMethod.PERCENTAGE_OF_GROSS, "1.75", false, true, 7),
                new SalaryComponentSeed("PT", "Professional Tax", SalaryComponentType.DEDUCTION, SalaryCalculationMethod.FIXED, "200", false, true, 8),
                new SalaryComponentSeed("TDS", "Tax Deducted at Source", SalaryComponentType.DEDUCTION, SalaryCalculationMethod.FIXED, "0", false, true, 9)
        );

        int inserted = 0;
        for (SalaryComponentSeed seed : seeds) {
            if (salaryComponentRepository.existsByComponentCodeIgnoreCaseAndActiveTrue(seed.code())) {
                continue;
            }

            SalaryComponent component = new SalaryComponent();
            component.setComponentCode(seed.code());
            component.setComponentName(seed.name());
            component.setType(seed.type());
            component.setCalculationMethod(seed.method());
            component.setDefaultValue(new BigDecimal(seed.defaultValue()));
            component.setTaxable(seed.taxable());
            component.setStatutory(seed.statutory());
            component.setSortOrder(seed.sortOrder());
            component.setActive(true);
            salaryComponentRepository.save(component);
            inserted++;
        }

        if (inserted > 0) {
            log.info("HRMS seeder inserted {} default salary components.", inserted);
        }
    }

    private record LeaveTypeSeed(
            String code,
            String displayName,
            int annualQuota,
            boolean carryForwardAllowed,
            int maxCarryForward,
            boolean encashmentAllowed,
            boolean requiresDocument,
            Integer documentRequiredAfterDays,
            boolean paid,
            int sortOrder
    ) {
    }

    private record StaffGradeSeed(
            String code,
            String name,
            TeachingWing wing,
            String minBand,
            String maxBand,
            int sortOrder
    ) {
    }

    private record SalaryComponentSeed(
            String code,
            String name,
            SalaryComponentType type,
            SalaryCalculationMethod method,
            String defaultValue,
            boolean taxable,
            boolean statutory,
            int sortOrder
    ) {
    }
}
