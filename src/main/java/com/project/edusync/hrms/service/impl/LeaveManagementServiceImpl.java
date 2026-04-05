package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.calendar.BulkOperationResultDTO;
import com.project.edusync.hrms.dto.leave.LeaveApplicationCreateDTO;
import com.project.edusync.hrms.dto.leave.LeaveApplicationResponseDTO;
import com.project.edusync.hrms.dto.leave.LeaveBalanceInitRequestDTO;
import com.project.edusync.hrms.dto.leave.LeaveBalanceResponseDTO;
import com.project.edusync.hrms.dto.leave.LeaveReviewDTO;
import com.project.edusync.hrms.model.entity.AcademicCalendarEvent;
import com.project.edusync.hrms.model.entity.LeaveApplication;
import com.project.edusync.hrms.model.entity.LeaveBalance;
import com.project.edusync.hrms.model.entity.LeaveTypeConfig;
import com.project.edusync.hrms.model.enums.DayType;
import com.project.edusync.hrms.model.enums.LeaveApplicationStatus;
import com.project.edusync.hrms.repository.AcademicCalendarEventRepository;
import com.project.edusync.hrms.repository.LeaveApplicationRepository;
import com.project.edusync.hrms.repository.LeaveBalanceRepository;
import com.project.edusync.hrms.repository.LeaveTypeConfigRepository;
import com.project.edusync.hrms.service.LeaveManagementService;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.enums.StaffCategory;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LeaveManagementServiceImpl implements LeaveManagementService {

    private static final Set<LeaveApplicationStatus> OVERLAP_BLOCKING_STATUSES = Set.of(
            LeaveApplicationStatus.PENDING,
            LeaveApplicationStatus.APPROVED
    );

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeConfigRepository leaveTypeConfigRepository;
    private final AcademicCalendarEventRepository academicCalendarEventRepository;
    private final StaffRepository staffRepository;
    private final AuthUtil authUtil;

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveApplicationResponseDTO> listApplications(
            Long currentUserId,
            boolean canViewAll,
            Long staffId,
            LeaveApplicationStatus status,
            String leaveTypeCode,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {
        String normalizedLeaveTypeCode = normalizeLeaveTypeCode(leaveTypeCode);
        Long effectiveStaffId = staffId;
        if (!canViewAll) {
            Staff currentStaff = resolveStaffByUserId(currentUserId);
            effectiveStaffId = currentStaff.getId();
        }
        return leaveApplicationRepository.search(
                        effectiveStaffId,
                        status,
                        normalizedLeaveTypeCode,
                        fromDate,
                        toDate,
                        pageable
                )
                .map(this::toApplicationResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveApplicationResponseDTO getApplicationById(Long applicationId) {
        return toApplicationResponse(findActiveApplication(applicationId));
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveApplicationResponseDTO getApplicationByIdentifier(String identifier) {
        return toApplicationResponse(findActiveApplicationByIdentifier(identifier));
    }

    @Override
    @Transactional
    public LeaveApplicationResponseDTO applyForCurrentStaff(LeaveApplicationCreateDTO dto) {
        Staff currentStaff = getCurrentStaff();
        validateDateRange(dto.fromDate(), dto.toDate());

        LeaveTypeConfig leaveType = findActiveLeaveTypeByIdentifier(dto.leaveTypeRef());
        validateLeaveTypeApplicability(leaveType, currentStaff.getCategory());
        validateHalfDayConstraints(dto);

        if (leaveApplicationRepository.existsOverlapping(currentStaff.getId(), dto.fromDate(), dto.toDate(), OVERLAP_BLOCKING_STATUSES)) {
            throw new EdusyncException("Overlapping leave application exists for the selected date range", HttpStatus.CONFLICT);
        }

        BigDecimal totalDays = calculateTotalDays(dto.fromDate(), dto.toDate(), dto.isHalfDay() != null && dto.isHalfDay(), dto.halfDayType());
        if (totalDays.compareTo(BigDecimal.ZERO) <= 0) {
            throw new EdusyncException("Selected range has no applicable working days", HttpStatus.BAD_REQUEST);
        }

        enforceLeaveTypeRules(leaveType, totalDays, dto.fromDate(), dto.attachmentUrl());
        if (!isLopType(leaveType)) {
            LeaveBalance balance = getOrInitializeBalance(currentStaff, leaveType, academicYearForDate(dto.fromDate()));
            if (balance.getRemaining().compareTo(totalDays) < 0) {
                throw new EdusyncException("Insufficient leave balance", HttpStatus.BAD_REQUEST);
            }
        }

        LeaveApplication application = new LeaveApplication();
        application.setStaff(currentStaff);
        application.setLeaveType(leaveType);
        application.setFromDate(dto.fromDate());
        application.setToDate(dto.toDate());
        application.setTotalDays(totalDays);
        application.setHalfDay(dto.isHalfDay() != null && dto.isHalfDay());
        application.setHalfDayType(dto.halfDayType());
        application.setReason(dto.reason().trim());
        application.setAttachmentUrl(dto.attachmentUrl());
        application.setStatus(LeaveApplicationStatus.PENDING);
        application.setAppliedOn(LocalDateTime.now());

        return toApplicationResponse(leaveApplicationRepository.save(application));
    }

    @Override
    @Transactional
    public LeaveApplicationResponseDTO approve(Long applicationId, Long reviewerUserId, LeaveReviewDTO dto) {
        LeaveApplication application = findActiveApplication(applicationId);
        if (application.getStatus() != LeaveApplicationStatus.PENDING) {
            throw new EdusyncException("Only pending applications can be approved", HttpStatus.BAD_REQUEST);
        }

        User applicantUser = resolveApplicantUser(application);
        if (applicantUser != null && applicantUser.getId().equals(reviewerUserId)) {
            throw new EdusyncException("You cannot approve your own leave application", HttpStatus.BAD_REQUEST);
        }

        if (leaveApplicationRepository.existsOverlappingExcludingId(
                application.getId(),
                application.getStaff().getId(),
                application.getFromDate(),
                application.getToDate(),
                Set.of(LeaveApplicationStatus.APPROVED)
        )) {
            throw new EdusyncException("Conflicting approved leave exists for this date range", HttpStatus.CONFLICT);
        }

        if (!isLopType(application.getLeaveType())) {
            LeaveBalance balance = getOrInitializeBalance(
                    application.getStaff(),
                    application.getLeaveType(),
                    academicYearForDate(application.getFromDate())
            );

            if (balance.getRemaining().compareTo(application.getTotalDays()) < 0) {
                throw new EdusyncException("Insufficient leave balance at approval time", HttpStatus.BAD_REQUEST);
            }

            balance.setUsed(balance.getUsed().add(application.getTotalDays()));
            leaveBalanceRepository.save(balance);
        }

        application.setStatus(LeaveApplicationStatus.APPROVED);
        Staff reviewerStaff = findStaffByUserId(reviewerUserId).orElse(null);
        application.setReviewedBy(reviewerStaff);
        application.setReviewedByUserId(reviewerUserId);
        application.setReviewedByName(resolveReviewerName(reviewerStaff, reviewerUserId));
        application.setReviewedOn(LocalDateTime.now());
        application.setReviewRemarks(dto != null ? dto.remarks() : null);

        return toApplicationResponse(leaveApplicationRepository.save(application));
    }

    @Override
    @Transactional
    public LeaveApplicationResponseDTO approveByIdentifier(String identifier, Long reviewerUserId, LeaveReviewDTO dto) {
        LeaveApplication application = findActiveApplicationByIdentifier(identifier);
        return approve(application.getId(), reviewerUserId, dto);
    }

    @Override
    @Transactional
    public LeaveApplicationResponseDTO reject(Long applicationId, Long reviewerUserId, LeaveReviewDTO dto) {
        LeaveApplication application = findActiveApplication(applicationId);
        if (application.getStatus() != LeaveApplicationStatus.PENDING) {
            throw new EdusyncException("Only pending applications can be rejected", HttpStatus.BAD_REQUEST);
        }

        User applicantUser = resolveApplicantUser(application);
        if (applicantUser != null && applicantUser.getId().equals(reviewerUserId)) {
            throw new EdusyncException("You cannot reject your own leave application", HttpStatus.BAD_REQUEST);
        }

        application.setStatus(LeaveApplicationStatus.REJECTED);
        Staff reviewerStaff = findStaffByUserId(reviewerUserId).orElse(null);
        application.setReviewedBy(reviewerStaff);
        application.setReviewedByUserId(reviewerUserId);
        application.setReviewedByName(resolveReviewerName(reviewerStaff, reviewerUserId));
        application.setReviewedOn(LocalDateTime.now());
        application.setReviewRemarks(dto != null ? dto.remarks() : null);

        return toApplicationResponse(leaveApplicationRepository.save(application));
    }

    @Override
    @Transactional
    public LeaveApplicationResponseDTO rejectByIdentifier(String identifier, Long reviewerUserId, LeaveReviewDTO dto) {
        LeaveApplication application = findActiveApplicationByIdentifier(identifier);
        return reject(application.getId(), reviewerUserId, dto);
    }

    @Override
    @Transactional
    public LeaveApplicationResponseDTO cancelByCurrentStaff(Long applicationId) {
        LeaveApplication application = findActiveApplication(applicationId);
        Staff currentStaff = getCurrentStaff();

        if (!application.getStaff().getId().equals(currentStaff.getId())) {
            throw new EdusyncException("You can only cancel your own leave application", HttpStatus.FORBIDDEN);
        }

        if (application.getStatus() == LeaveApplicationStatus.REJECTED || application.getStatus() == LeaveApplicationStatus.CANCELLED) {
            throw new EdusyncException("This leave application is already closed", HttpStatus.BAD_REQUEST);
        }

        if (application.getStatus() == LeaveApplicationStatus.APPROVED) {
            if (application.getFromDate().isBefore(LocalDate.now())) {
                throw new EdusyncException("Cannot cancel approved leave after leave period has started", HttpStatus.BAD_REQUEST);
            }

            if (!isLopType(application.getLeaveType())) {
                LeaveBalance balance = getOrInitializeBalance(
                        application.getStaff(),
                        application.getLeaveType(),
                        academicYearForDate(application.getFromDate())
                );
                BigDecimal updatedUsed = balance.getUsed().subtract(application.getTotalDays());
                balance.setUsed(updatedUsed.max(BigDecimal.ZERO));
                leaveBalanceRepository.save(balance);
            }
        }

        application.setStatus(LeaveApplicationStatus.CANCELLED);
        application.setReviewedBy(currentStaff);
        application.setReviewedByUserId(currentStaff.getUserProfile() != null && currentStaff.getUserProfile().getUser() != null
                ? currentStaff.getUserProfile().getUser().getId()
                : null);
        application.setReviewedByName(resolveReviewerName(currentStaff, authUtil.getCurrentUserId()));
        application.setReviewedOn(LocalDateTime.now());
        application.setReviewRemarks("Cancelled by applicant");

        return toApplicationResponse(leaveApplicationRepository.save(application));
    }

    @Override
    @Transactional
    public LeaveApplicationResponseDTO cancelByCurrentStaffIdentifier(String identifier) {
        LeaveApplication application = findActiveApplicationByIdentifier(identifier);
        return cancelByCurrentStaff(application.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponseDTO> getBalanceForStaff(Long staffId, String academicYear) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with id: " + staffId));

        String year = normalizeAcademicYear(academicYear);
        return leaveBalanceRepository.findByStaff_IdAndAcademicYearAndActiveTrueOrderByLeaveType_LeaveCodeAsc(staffId, year)
                .stream()
                .map(balance -> toBalanceResponse(balance, staff))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponseDTO> getBalanceForStaffIdentifier(String staffIdentifier, String academicYear) {
        Staff staff = findActiveStaffByIdentifier(staffIdentifier);
        return getBalanceForStaff(staff.getId(), academicYear);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveBalanceResponseDTO> getAllBalances(String academicYear, Pageable pageable) {
        String year = normalizeAcademicYear(academicYear);
        return leaveBalanceRepository.findByAcademicYearAndActiveTrue(year, pageable)
                .map(balance -> toBalanceResponse(balance, balance.getStaff()));
    }

    @Override
    @Transactional
    public BulkOperationResultDTO initializeBalances(LeaveBalanceInitRequestDTO request) {
        String academicYear = normalizeAcademicYear(request.academicYear());
        String carryForwardYear = request.carryForwardFromYear() == null || request.carryForwardFromYear().isBlank()
                ? null
                : normalizeAcademicYear(request.carryForwardFromYear());

        List<Staff> staffList = staffRepository.findAll().stream()
                .filter(Staff::isActive)
                .toList();
        List<LeaveTypeConfig> leaveTypes = leaveTypeConfigRepository.findByActiveTrueOrderBySortOrderAscLeaveCodeAsc();

        int success = 0;
        List<String> errors = new ArrayList<>();

        for (Staff staff : staffList) {
            for (LeaveTypeConfig leaveType : leaveTypes) {
                try {
                    if (!isLeaveTypeApplicableToCategory(leaveType, staff.getCategory())) {
                        continue;
                    }
                    if (leaveBalanceRepository.findByStaff_IdAndLeaveType_IdAndAcademicYearAndActiveTrue(
                            staff.getId(), leaveType.getId(), academicYear
                    ).isPresent()) {
                        continue;
                    }

                    LeaveBalance balance = new LeaveBalance();
                    balance.setStaff(staff);
                    balance.setLeaveType(leaveType);
                    balance.setAcademicYear(academicYear);

                    BigDecimal annualQuota = BigDecimal.valueOf(leaveType.getAnnualQuota());
                    BigDecimal carriedForward = BigDecimal.ZERO;

                    if (carryForwardYear != null && leaveType.isCarryForwardAllowed()) {
                        carriedForward = leaveBalanceRepository.findByStaff_IdAndLeaveType_IdAndAcademicYearAndActiveTrue(
                                        staff.getId(), leaveType.getId(), carryForwardYear
                                )
                                .map(LeaveBalance::getRemaining)
                                .orElse(BigDecimal.ZERO);

                        BigDecimal maxCarry = BigDecimal.valueOf(leaveType.getMaxCarryForward() == null ? 0 : leaveType.getMaxCarryForward());
                        if (carriedForward.compareTo(maxCarry) > 0) {
                            carriedForward = maxCarry;
                        }
                    }

                    balance.setCarriedForward(carriedForward);
                    balance.setTotalQuota(annualQuota.add(carriedForward));
                    balance.setUsed(BigDecimal.ZERO);
                    leaveBalanceRepository.save(balance);
                    success++;
                } catch (Exception ex) {
                    errors.add("staffId=" + staff.getId() + ", leaveType=" + leaveType.getLeaveCode() + ": " + ex.getMessage());
                }
            }
        }

        int totalProcessed = staffList.size() * leaveTypes.size();
        return new BulkOperationResultDTO(totalProcessed, success, totalProcessed - success, errors);
    }

    private LeaveApplication findActiveApplication(Long applicationId) {
        LeaveApplication application = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave application not found with id: " + applicationId));

        if (!application.isActive()) {
            throw new ResourceNotFoundException("Leave application not found with id: " + applicationId);
        }
        return application;
    }

    private LeaveApplication findActiveApplicationByIdentifier(String identifier) {
        LeaveApplication application = PublicIdentifierResolver.resolve(
                identifier,
                leaveApplicationRepository::findByUuid,
                leaveApplicationRepository::findById,
                "Leave application"
        );
        if (!application.isActive()) {
            throw new ResourceNotFoundException("Leave application not found with identifier: " + identifier);
        }
        return application;
    }

    private LeaveTypeConfig findActiveLeaveType(Long leaveTypeId) {
        LeaveTypeConfig leaveType = leaveTypeConfigRepository.findById(leaveTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id: " + leaveTypeId));

        if (!leaveType.isActive()) {
            throw new EdusyncException("Selected leave type is inactive", HttpStatus.BAD_REQUEST);
        }
        return leaveType;
    }

    private LeaveTypeConfig findActiveLeaveTypeByIdentifier(String identifier) {
        LeaveTypeConfig leaveType = PublicIdentifierResolver.resolve(
                identifier,
                leaveTypeConfigRepository::findByUuid,
                leaveTypeConfigRepository::findById,
                "Leave type"
        );

        if (!leaveType.isActive()) {
            throw new EdusyncException("Selected leave type is inactive", HttpStatus.BAD_REQUEST);
        }
        return leaveType;
    }

    private Staff getCurrentStaff() {
        Long currentUserId = authUtil.getCurrentUserId();
        return resolveStaffByUserId(currentUserId);
    }

    private Staff findActiveStaffByIdentifier(String identifier) {
        Staff staff = PublicIdentifierResolver.resolve(
                identifier,
                staffRepository::findByUuid,
                staffRepository::findById,
                "Staff"
        );
        if (!staff.isActive()) {
            throw new ResourceNotFoundException("Staff not found with identifier: " + identifier);
        }
        return staff;
    }

    private Staff resolveStaffByUserId(Long userId) {
        return staffRepository.findByUserProfile_User_Id(userId)
                .orElseThrow(() -> new EdusyncException("Authenticated user is not linked to a staff profile", HttpStatus.FORBIDDEN));
    }

    private java.util.Optional<Staff> findStaffByUserId(Long userId) {
        return staffRepository.findByUserProfile_User_Id(userId);
    }

    private User resolveApplicantUser(LeaveApplication application) {
        if (application.getStaff() == null) {
            return null;
        }
        if (application.getStaff().getUser() != null) {
            return application.getStaff().getUser();
        }
        if (application.getStaff().getUserProfile() != null) {
            return application.getStaff().getUserProfile().getUser();
        }
        return null;
    }

    private String resolveReviewerName(Staff reviewerStaff, Long reviewerUserId) {
        if (reviewerStaff != null && reviewerStaff.getUserProfile() != null) {
            String first = reviewerStaff.getUserProfile().getFirstName() == null ? "" : reviewerStaff.getUserProfile().getFirstName();
            String last = reviewerStaff.getUserProfile().getLastName() == null ? "" : reviewerStaff.getUserProfile().getLastName();
            String fullName = (first + " " + last).trim();
            if (!fullName.isBlank()) {
                return fullName;
            }
        }
        return reviewerUserId == null ? null : "User #" + reviewerUserId;
    }

    private void validateLeaveTypeApplicability(LeaveTypeConfig leaveType, StaffCategory category) {
        if (isLeaveTypeApplicableToCategory(leaveType, category)) {
            return;
        }
        throw new EdusyncException(
                leaveType.getDisplayName() + " is not applicable for " + category + " staff",
                HttpStatus.BAD_REQUEST
        );
    }

    private boolean isLeaveTypeApplicableToCategory(LeaveTypeConfig leaveType, StaffCategory category) {
        if (leaveType.getApplicableCategories() == null || leaveType.getApplicableCategories().isEmpty() || category == null) {
            return true;
        }
        return leaveType.getApplicableCategories().contains(category);
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (toDate.isBefore(fromDate)) {
            throw new EdusyncException("toDate cannot be before fromDate", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateHalfDayConstraints(LeaveApplicationCreateDTO dto) {
        boolean halfDay = dto.isHalfDay() != null && dto.isHalfDay();
        if (!halfDay) {
            return;
        }

        if (!dto.fromDate().equals(dto.toDate())) {
            throw new EdusyncException("Half-day leave must be for a single date", HttpStatus.BAD_REQUEST);
        }

        if (dto.halfDayType() == null) {
            throw new EdusyncException("halfDayType is required when isHalfDay=true", HttpStatus.BAD_REQUEST);
        }
    }

    private BigDecimal calculateTotalDays(LocalDate fromDate, LocalDate toDate, boolean halfDay, Object halfDayType) {
        if (halfDay) {
            DayType dayType = resolveStaffDayType(fromDate, academicYearForDate(fromDate));
            if (dayType == DayType.HOLIDAY || dayType == DayType.RESTRICTED_HOLIDAY || dayType == DayType.VACATION) {
                throw new EdusyncException("Half-day leave cannot be applied on a holiday", HttpStatus.BAD_REQUEST);
            }
            return BigDecimal.valueOf(0.5).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal total = BigDecimal.ZERO;
        String academicYear = academicYearForDate(fromDate);
        LocalDate cursor = fromDate;
        while (!cursor.isAfter(toDate)) {
            DayType dayType = resolveStaffDayType(cursor, academicYear);
            if (dayType == DayType.HALF_DAY) {
                total = total.add(BigDecimal.valueOf(0.5));
            } else if (dayType == DayType.WORKING || dayType == DayType.EXAM_DAY) {
                total = total.add(BigDecimal.ONE);
            }
            cursor = cursor.plusDays(1);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private DayType resolveStaffDayType(LocalDate date, String academicYear) {
        return academicCalendarEventRepository.findByAcademicYearAndDateAndIsActiveTrue(academicYear, date)
                .filter(AcademicCalendarEvent::isAppliesToStaff)
                .map(AcademicCalendarEvent::getDayType)
                .orElseGet(() -> {
                    DayOfWeek day = date.getDayOfWeek();
                    return (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) ? DayType.HOLIDAY : DayType.WORKING;
                });
    }

    private void enforceLeaveTypeRules(LeaveTypeConfig leaveType, BigDecimal totalDays, LocalDate fromDate, String attachmentUrl) {
        if (leaveType.getMinDaysBeforeApply() != null && leaveType.getMinDaysBeforeApply() > 0) {
            long daysInAdvance = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), fromDate);
            if (daysInAdvance < leaveType.getMinDaysBeforeApply()) {
                throw new EdusyncException("Leave must be applied at least " + leaveType.getMinDaysBeforeApply() + " day(s) in advance", HttpStatus.BAD_REQUEST);
            }
        }

        if (leaveType.getMaxConsecutiveDays() != null && totalDays.compareTo(BigDecimal.valueOf(leaveType.getMaxConsecutiveDays())) > 0) {
            throw new EdusyncException("Leave exceeds maximum consecutive days allowed", HttpStatus.BAD_REQUEST);
        }

        if (leaveType.isRequiresDocument()) {
            int requiredAfter = leaveType.getDocumentRequiredAfterDays() == null ? 0 : leaveType.getDocumentRequiredAfterDays();
            if (totalDays.compareTo(BigDecimal.valueOf(requiredAfter)) > 0 && (attachmentUrl == null || attachmentUrl.isBlank())) {
                throw new EdusyncException("Supporting document is required for this leave request", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private LeaveBalance getOrInitializeBalance(Staff staff, LeaveTypeConfig leaveType, String academicYear) {
        return leaveBalanceRepository.findByStaff_IdAndLeaveType_IdAndAcademicYearAndActiveTrue(staff.getId(), leaveType.getId(), academicYear)
                .orElseGet(() -> {
                    LeaveBalance balance = new LeaveBalance();
                    balance.setStaff(staff);
                    balance.setLeaveType(leaveType);
                    balance.setAcademicYear(academicYear);
                    balance.setCarriedForward(BigDecimal.ZERO);
                    balance.setUsed(BigDecimal.ZERO);
                    balance.setTotalQuota(BigDecimal.valueOf(leaveType.getAnnualQuota()));
                    return leaveBalanceRepository.save(balance);
                });
    }

    private String academicYearForDate(LocalDate date) {
        int startYear = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        return startYear + "-" + (startYear + 1);
    }

    private String normalizeAcademicYear(String academicYear) {
        String raw = academicYear == null || academicYear.isBlank() ? academicYearForDate(LocalDate.now()) : academicYear.trim();
        String[] parts = raw.split("-");
        if (parts.length != 2) {
            throw new EdusyncException("Invalid academicYear format. Expected YYYY-YYYY", HttpStatus.BAD_REQUEST);
        }

        try {
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);
            if (end != start + 1) {
                throw new EdusyncException("Invalid academicYear format. Expected YYYY-YYYY", HttpStatus.BAD_REQUEST);
            }
            return start + "-" + end;
        } catch (NumberFormatException ex) {
            throw new EdusyncException("Invalid academicYear format. Expected YYYY-YYYY", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeLeaveTypeCode(String leaveTypeCode) {
        if (leaveTypeCode == null || leaveTypeCode.isBlank()) {
            return null;
        }
        return leaveTypeCode.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isLopType(LeaveTypeConfig leaveType) {
        return "LOP".equalsIgnoreCase(leaveType.getLeaveCode());
    }

    private LeaveApplicationResponseDTO toApplicationResponse(LeaveApplication application) {
        Staff staff = application.getStaff();
        String staffName = (staff.getUserProfile().getFirstName() + " " + staff.getUserProfile().getLastName()).trim();

        return new LeaveApplicationResponseDTO(
                application.getId(),
                application.getUuid() != null ? application.getUuid().toString() : null,
                staff.getId(),
                staffName,
                staff.getEmployeeId(),
                staff.getCategory(),
                staff.getDesignation() != null ? staff.getDesignation().getDesignationName() : null,
                application.getLeaveType().getId(),
                application.getLeaveType().getLeaveCode(),
                application.getLeaveType().getDisplayName(),
                application.getFromDate(),
                application.getToDate(),
                application.getTotalDays(),
                application.isHalfDay(),
                application.getHalfDayType(),
                application.getReason(),
                application.getAttachmentUrl(),
                application.getStatus(),
                application.getAppliedOn(),
                application.getReviewedByUserId(),
                application.getReviewedByName(),
                application.getReviewRemarks(),
                application.getReviewedOn()
        );
    }

    private LeaveBalanceResponseDTO toBalanceResponse(LeaveBalance balance, Staff staff) {
        String staffName = (staff.getUserProfile().getFirstName() + " " + staff.getUserProfile().getLastName()).trim();
        return new LeaveBalanceResponseDTO(
                balance.getId(),
                staff.getId(),
                staffName,
                balance.getLeaveType().getId(),
                balance.getLeaveType().getLeaveCode(),
                balance.getLeaveType().getDisplayName(),
                balance.getAcademicYear(),
                balance.getTotalQuota(),
                balance.getUsed(),
                balance.getCarriedForward(),
                balance.getRemaining()
        );
    }
}

