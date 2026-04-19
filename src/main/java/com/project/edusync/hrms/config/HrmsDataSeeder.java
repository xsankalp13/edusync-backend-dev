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
import com.project.edusync.hrms.repository.StaffDesignationRepository;
import com.project.edusync.hrms.model.entity.StaffDesignation;
import com.project.edusync.hrms.model.entity.OnboardingTemplate;
import com.project.edusync.hrms.model.entity.OnboardingTemplateTask;
import com.project.edusync.hrms.model.enums.AssignedParty;
import com.project.edusync.hrms.repository.OnboardingTemplateRepository;
import com.project.edusync.hrms.repository.OnboardingTemplateTaskRepository;
import com.project.edusync.uis.model.enums.StaffCategory;
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
    private final StaffDesignationRepository staffDesignationRepository;
    private final OnboardingTemplateRepository onboardingTemplateRepository;
    private final OnboardingTemplateTaskRepository onboardingTemplateTaskRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedDefaultLeaveTypes();
        seedDefaultStaffGrades();
        seedDefaultSalaryComponents();
        seedDefaultDesignations();
        seedDefaultOnboardingTemplates();
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

    private void seedDefaultDesignations() {
        List<StaffDesignationSeed> seeds = List.of(
                new StaffDesignationSeed("TGT", "Trained Graduate Teacher", StaffCategory.TEACHING, 1),
                new StaffDesignationSeed("PGT", "Post Graduate Teacher", StaffCategory.TEACHING, 2),
                new StaffDesignationSeed("RPGT", "Retrained PG Teacher", StaffCategory.TEACHING, 3),
                new StaffDesignationSeed("PRT", "Primary Teacher", StaffCategory.TEACHING, 4),
                new StaffDesignationSeed("HM", "Head Master", StaffCategory.TEACHING, 5),
                new StaffDesignationSeed("VICE_P", "Vice Principal", StaffCategory.NON_TEACHING_ADMIN, 6),
                new StaffDesignationSeed("LAB_ASST", "Laboratory Assistant", StaffCategory.NON_TEACHING_SUPPORT, 7),
                new StaffDesignationSeed("LIBRARIAN", "Librarian", StaffCategory.NON_TEACHING_SUPPORT, 8),
                new StaffDesignationSeed("CLERK", "Office Clerk", StaffCategory.NON_TEACHING_ADMIN, 9),
                new StaffDesignationSeed("ACCOUNTANT", "Accountant", StaffCategory.NON_TEACHING_ADMIN, 10),
                new StaffDesignationSeed("PEON", "Peon / Office Attendant", StaffCategory.NON_TEACHING_SUPPORT, 11),
                new StaffDesignationSeed("SECURITY", "Security Guard", StaffCategory.NON_TEACHING_SUPPORT, 12),
                new StaffDesignationSeed("IT_ADMIN", "IT Administrator", StaffCategory.NON_TEACHING_ADMIN, 13),
                new StaffDesignationSeed("COUNSELOR", "Student Counselor", StaffCategory.TEACHING, 14)
        );

        int inserted = 0;
        for (StaffDesignationSeed seed : seeds) {
            if (staffDesignationRepository.findByDesignationCodeIgnoreCase(seed.code()).isPresent()) {
                continue;
            }

            StaffDesignation designation = new StaffDesignation();
            designation.setDesignationCode(seed.code());
            designation.setDesignationName(seed.name());
            designation.setCategory(seed.category());
            designation.setSortOrder(seed.sortOrder());
            designation.setActive(true);
            staffDesignationRepository.save(designation);
            inserted++;
        }

        if (inserted > 0) {
            log.info("HRMS seeder inserted {} default staff designations.", inserted);
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

    private record StaffDesignationSeed(
            String code,
            String name,
            StaffCategory category,
            int sortOrder
    ) {
    }

    private void seedDefaultOnboardingTemplates() {
        int inserted = 0;

        // 1. Teaching Staff Template
        if (!onboardingTemplateRepository.existsByTemplateName("Standard Teaching Staff Onboarding")) {
            OnboardingTemplate teachingTemplate = new OnboardingTemplate();
            teachingTemplate.setTemplateName("Standard Teaching Staff Onboarding");
            teachingTemplate.setDescription("Standard 20-step onboarding process for all teaching staff including academic induction.");
            teachingTemplate.setActive(true);
            teachingTemplate = onboardingTemplateRepository.save(teachingTemplate);

            List<OnboardingTaskSeed> teachingTasks = List.of(
                    new OnboardingTaskSeed("Collect signed Appointment Letter", "Ensure the final signed copy is placed in the employee file.", 0, AssignedParty.HR),
                    new OnboardingTaskSeed("Collect educational certificates", "Originals for verification and copies for records.", 0, AssignedParty.STAFF),
                    new OnboardingTaskSeed("Collect Aadhaar card & PAN card copies", "Required for payroll and identity verification.", 0, AssignedParty.STAFF),
                    new OnboardingTaskSeed("Collect passport-size photographs", "Require 4 copies for ID and records.", 0, AssignedParty.STAFF),
                    new OnboardingTaskSeed("Collect experience / relieving letter", "From previous employer, if applicable.", 1, AssignedParty.STAFF),
                    new OnboardingTaskSeed("Collect medical fitness certificate", "Must be from a registered medical practitioner.", 3, AssignedParty.STAFF),
                    new OnboardingTaskSeed("Complete staff registration in ERP", "Create the profile in Shiksha Intelligence.", 0, AssignedParty.HR),
                    new OnboardingTaskSeed("Create school email account", "e.g., firstname.lastname@school.edu", 1, AssignedParty.HR),
                    new OnboardingTaskSeed("Enroll in biometric / attendance system", "Register fingerprints or facial recognition.", 1, AssignedParty.HR),
                    new OnboardingTaskSeed("Issue ID card", "Print and hand over the permanent ID card.", 3, AssignedParty.HR),
                    new OnboardingTaskSeed("Conduct campus & facilities tour", "Familiarize staff with academic blocks, staff room, cafeteria, etc.", 1, AssignedParty.HR),
                    new OnboardingTaskSeed("Introduce to department team & Principal", "Brief introductory meeting.", 1, AssignedParty.BOTH),
                    new OnboardingTaskSeed("Share school policies handbook", "Include leave, attendance, and code of conduct.", 2, AssignedParty.HR),
                    new OnboardingTaskSeed("Conduct child protection & POCSO session", "Mandatory awareness and compliance session.", 7, AssignedParty.HR),
                    new OnboardingTaskSeed("Share class/subject assignment & timetable", "Provide the academic schedule.", 3, AssignedParty.HR),
                    new OnboardingTaskSeed("Assign mentor teacher / buddy", "For academic guidance during probation.", 3, AssignedParty.HR),
                    new OnboardingTaskSeed("Complete ERP/LMS training session", "Training on grading, attendance, and lesson planning modules.", 7, AssignedParty.BOTH),
                    new OnboardingTaskSeed("Initiate background verification (BGV)", "Start the third-party BGV process.", 7, AssignedParty.HR),
                    new OnboardingTaskSeed("30-day probation check-in review", "First feedback session.", 30, AssignedParty.BOTH),
                    new OnboardingTaskSeed("90-day probation final review", "Final appraisal for confirmation.", 90, AssignedParty.HR)
            );
            saveTasks(teachingTemplate, teachingTasks);
            inserted++;
        }

        // 2. Non-Teaching Staff Template
        if (!onboardingTemplateRepository.existsByTemplateName("Standard Administrative Staff Onboarding")) {
            OnboardingTemplate supportTemplate = new OnboardingTemplate();
            supportTemplate.setTemplateName("Standard Administrative Staff Onboarding");
            supportTemplate.setDescription("Standard onboarding process for admin and support staff.");
            supportTemplate.setActive(true);
            supportTemplate = onboardingTemplateRepository.save(supportTemplate);

            List<OnboardingTaskSeed> supportTasks = List.of(
                    new OnboardingTaskSeed("Collect signed Appointment Letter", "Ensure the final signed copy is placed in the employee file.", 0, AssignedParty.HR),
                    new OnboardingTaskSeed("Collect ID & Address proof", "Aadhaar card & PAN card copies.", 0, AssignedParty.STAFF),
                    new OnboardingTaskSeed("Collect passport-size photographs", "Require 4 copies for ID and records.", 0, AssignedParty.STAFF),
                    new OnboardingTaskSeed("Collect experience / relieving letter", "From previous employer, if applicable.", 1, AssignedParty.STAFF),
                    new OnboardingTaskSeed("Complete staff registration in ERP", "Create the profile in Shiksha Intelligence.", 0, AssignedParty.HR),
                    new OnboardingTaskSeed("Create school email account", "If applicable for the role.", 1, AssignedParty.HR),
                    new OnboardingTaskSeed("Enroll in biometric / attendance system", "Register fingerprints or facial recognition.", 1, AssignedParty.HR),
                    new OnboardingTaskSeed("Issue ID card", "Print and hand over the permanent ID card.", 3, AssignedParty.HR),
                    new OnboardingTaskSeed("Conduct campus & facilities tour", "Familiarize staff with relevant workspaces.", 1, AssignedParty.HR),
                    new OnboardingTaskSeed("Introduce to reporting manager", "Brief introductory meeting.", 1, AssignedParty.BOTH),
                    new OnboardingTaskSeed("Share school policies handbook", "Include leave, attendance, and code of conduct.", 2, AssignedParty.HR),
                    new OnboardingTaskSeed("Job-specific software/tool training", "Training on accounting, inventory, or relevant tools.", 7, AssignedParty.BOTH),
                    new OnboardingTaskSeed("30-day probation check-in review", "First feedback session.", 30, AssignedParty.BOTH),
                    new OnboardingTaskSeed("90-day probation final review", "Final appraisal for confirmation.", 90, AssignedParty.HR)
            );
            saveTasks(supportTemplate, supportTasks);
            inserted++;
        }

        if (inserted > 0) {
            log.info("HRMS seeder inserted {} default onboarding templates with tasks.", inserted);
        }
    }

    private void saveTasks(OnboardingTemplate template, List<OnboardingTaskSeed> tasks) {
        int order = 1;
        for (OnboardingTaskSeed seed : tasks) {
            OnboardingTemplateTask task = new OnboardingTemplateTask();
            task.setTemplate(template);
            task.setTaskOrder(order++);
            task.setTaskTitle(seed.title());
            task.setDescription(seed.description());
            task.setDueAfterDays(seed.dueAfterDays());
            task.setAssignedParty(seed.party());
            task.setActive(true);
            onboardingTemplateTaskRepository.save(task);
        }
    }

    private record OnboardingTaskSeed(
            String title,
            String description,
            int dueAfterDays,
            AssignedParty party
    ) {
    }
}
