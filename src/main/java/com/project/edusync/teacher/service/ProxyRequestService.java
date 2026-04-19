package com.project.edusync.teacher.service;

import com.project.edusync.ams.model.entity.StaffDailyAttendance;
import com.project.edusync.ams.model.repository.StaffDailyAttendanceRepository;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.UserRepository;
import com.project.edusync.teacher.model.dto.*;
import com.project.edusync.teacher.model.entity.ProxyRequest;
import com.project.edusync.teacher.model.enums.ProxyRequestStatus;
import com.project.edusync.teacher.repository.ProxyRequestRepository;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProxyRequestService {

    private final ProxyRequestRepository proxyRequestRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final StaffDailyAttendanceRepository staffAttendanceRepository;

    // ── Teacher-facing operations ──────────────────────────────────────

    /**
     * Returns all proxy requests where the authenticated teacher is either
     * the requester or the target (both sent and received), newest first.
     */
    @Transactional(readOnly = true)
    public List<ProxyRequestResponseDto> getMyProxyRequests(Long userId) {
        return proxyRequestRepository.findAllByUserId(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Creates a peer proxy request from one teacher to another.
     *
     * @param requestedByUserId The authenticated teacher's user id.
     * @param dto               Create payload.
     * @return The created proxy request DTO.
     */
    @Transactional
    public ProxyRequestResponseDto createPeerRequest(Long requestedByUserId, ProxyRequestCreateDto dto) {
        User requestedBy = userRepository.findByIdAsLong(requestedByUserId)
                .orElseThrow(() -> new EntityNotFoundException("Requesting user not found"));

        User requestedTo = userRepository.findByUuid(dto.requestedToUserUuid())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Target teacher not found: " + dto.requestedToUserUuid()));

        if (requestedBy.getId().equals(requestedTo.getId())) {
            throw new IllegalArgumentException("Cannot send a proxy request to yourself");
        }

        ProxyRequest request = new ProxyRequest();
        request.setRequestedBy(requestedBy);
        request.setRequestedTo(requestedTo);
        request.setSubject(dto.subject());
        request.setPeriodDate(dto.periodDate() != null ? dto.periodDate() : LocalDate.now());
        request.setSectionUuid(dto.sectionUuid());
        request.setStatus(ProxyRequestStatus.PENDING);
        request.syncIsAccepted();

        ProxyRequest saved = proxyRequestRepository.save(request);
        log.info("Peer proxy request {} created: {} → {}", saved.getId(),
                requestedBy.getUsername(), requestedTo.getUsername());
        return toDto(saved);
    }

    /**
     * Accepts a proxy request directed at the current user.
     *
     * @param requestId     The proxy_requests.id to accept.
     * @param currentUserId The authenticated teacher's user id — must match requestedTo.
     */
    @Transactional
    public ProxyRequestResponseDto acceptProxyRequest(Long requestId, Long currentUserId) {
        ProxyRequest request = findRequestOrThrow(requestId);
        assertAddressedToCurrentUser(request, currentUserId);
        assertPending(request);

        request.setStatus(ProxyRequestStatus.ACCEPTED);
        request.syncIsAccepted();
        ProxyRequest saved = proxyRequestRepository.save(request);
        log.info("Proxy request {} accepted by user {}", requestId, currentUserId);
        return toDto(saved);
    }

    /**
     * Declines a proxy request directed at the current user.
     *
     * @param requestId     The proxy_requests.id to decline.
     * @param currentUserId The authenticated teacher's user id — must match requestedTo.
     * @param reason        Optional decline reason.
     */
    @Transactional
    public ProxyRequestResponseDto declineProxyRequest(Long requestId, Long currentUserId, String reason) {
        ProxyRequest request = findRequestOrThrow(requestId);
        assertAddressedToCurrentUser(request, currentUserId);
        assertPending(request);

        request.setStatus(ProxyRequestStatus.DECLINED);
        request.setDeclineReason(reason);
        request.syncIsAccepted();
        ProxyRequest saved = proxyRequestRepository.save(request);
        log.info("Proxy request {} declined by user {}", requestId, currentUserId);
        return toDto(saved);
    }

    /**
     * Cancels a proxy request that the current user originally sent.
     *
     * @param requestId     The proxy_requests.id to cancel.
     * @param currentUserId The authenticated teacher's user id — must match requestedBy.
     */
    @Transactional
    public ProxyRequestResponseDto cancelMyRequest(Long requestId, Long currentUserId) {
        ProxyRequest request = findRequestOrThrow(requestId);
        if (!request.getRequestedBy().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only cancel your own proxy requests");
        }
        if (request.getStatus() == ProxyRequestStatus.CANCELLED) {
            throw new IllegalStateException("Request is already cancelled");
        }

        request.setStatus(ProxyRequestStatus.CANCELLED);
        request.syncIsAccepted();
        ProxyRequest saved = proxyRequestRepository.save(request);
        log.info("Proxy request {} cancelled by user {}", requestId, currentUserId);
        return toDto(saved);
    }

    /**
     * Returns today's confirmed proxy classes for the authenticated teacher.
     */
    @Transactional(readOnly = true)
    public List<ProxyRequestResponseDto> getMyProxyScheduleToday(Long userId) {
        LocalDate today = LocalDate.now();
        return proxyRequestRepository
                .findByRequestedToIdAndPeriodDateAndStatus(userId, today, ProxyRequestStatus.ACCEPTED)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ── Admin-facing operations ────────────────────────────────────────

    /**
     * Admin manually assigns a proxy teacher for an absent teacher's class.
     * Creates a new ProxyRequest in ACCEPTED state (no peer confirmation needed).
     */
    @Transactional
    public ProxyRequestResponseDto adminAssignProxy(AdminAssignProxyDto dto) {
        User absentUser = userRepository.findByUuid(dto.absentStaffUserUuid())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Absent staff user not found: " + dto.absentStaffUserUuid()));

        User proxyUser = userRepository.findByUuid(dto.proxyStaffUserUuid())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Proxy staff user not found: " + dto.proxyStaffUserUuid()));

        ProxyRequest request = new ProxyRequest();
        request.setRequestedBy(absentUser);
        request.setRequestedTo(proxyUser);
        request.setSubject(dto.subject());
        request.setPeriodDate(dto.periodDate());
        request.setSectionUuid(dto.sectionUuid());
        request.setStatus(ProxyRequestStatus.ACCEPTED);
        request.syncIsAccepted();

        ProxyRequest saved = proxyRequestRepository.save(request);
        log.info("Admin assigned proxy {} → {} for date {}", absentUser.getUsername(),
                proxyUser.getUsername(), dto.periodDate());
        return toDto(saved);
    }

    /**
     * Admin reassigns a proxy request to a different teacher.
     *
     * @param requestId          The proxy_requests.id.
     * @param newProxyUserUuid   UUID of the new proxy teacher's User record.
     */
    @Transactional
    public ProxyRequestResponseDto adminReassignProxy(Long requestId, UUID newProxyUserUuid) {
        ProxyRequest request = findRequestOrThrow(requestId);

        User newProxy = userRepository.findByUuid(newProxyUserUuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "New proxy teacher not found: " + newProxyUserUuid));

        request.setRequestedTo(newProxy);
        request.setStatus(ProxyRequestStatus.ACCEPTED);
        request.setDeclineReason(null);
        request.syncIsAccepted();

        ProxyRequest saved = proxyRequestRepository.save(request);
        log.info("Admin reassigned proxy request {} to user {}", requestId, newProxy.getUsername());
        return toDto(saved);
    }

    /**
     * Admin cancels/revokes a proxy request.
     */
    @Transactional
    public ProxyRequestResponseDto adminCancelProxy(Long requestId) {
        ProxyRequest request = findRequestOrThrow(requestId);
        request.setStatus(ProxyRequestStatus.CANCELLED);
        request.syncIsAccepted();
        ProxyRequest saved = proxyRequestRepository.save(request);
        log.info("Admin cancelled proxy request {}", requestId);
        return toDto(saved);
    }

    /**
     * Returns all pending/accepted proxy requests on the given date (admin dashboard).
     */
    @Transactional(readOnly = true)
    public List<ProxyRequestResponseDto> getActiveRequestsOnDate(LocalDate date) {
        return proxyRequestRepository.findActiveRequestsOnDate(date)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Returns absent teaching staff for the given date by querying HRMS attendance records
     * where the attendance type is an absence mark.
     *
     * @param date The date to check.
     */
    @Transactional(readOnly = true)
    public List<AbsentStaffDto> getAbsentStaffOnDate(LocalDate date) {
        // Find all staff_daily_attendance records where type is absence and date matches.
        // We join with Staff to get designation and filter by TEACHING category.
        return staffAttendanceRepository
                .findAll()
                .stream()
                .filter(a -> date.equals(a.getAttendanceDate())
                        && Boolean.TRUE.equals(a.getAttendanceType() != null
                                && a.getAttendanceType().isAbsenceMark()))
                .map(a -> buildAbsentStaffDto(a, date))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * Returns proxy load stats for all teaching staff — used for equitable assignment.
     */
    @Transactional(readOnly = true)
    public List<ProxyLoadStatsDto> getProxyLoadStats(LocalDate date) {
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate monthStart = date.withDayOfMonth(1);
        LocalDate monthEnd = date.with(TemporalAdjusters.lastDayOfMonth());

        return staffRepository.findAll()
                .stream()
                .filter(s -> s.getUser() != null && s.isActive())
                .map(staff -> {
                    Long userId = staff.getUser().getId();
                    String name = resolveFullName(staff);
                    int weeklyCount = (int) proxyRequestRepository
                            .countAcceptedProxiesBetween(userId, weekStart, weekEnd);
                    int monthlyCount = (int) proxyRequestRepository
                            .countAcceptedProxiesBetween(userId, monthStart, monthEnd);
                    return new ProxyLoadStatsDto(
                            staff.getUser().getUuid(), name, weeklyCount, monthlyCount);
                })
                .filter(s -> s.proxiesThisMonth() > 0 || s.proxiesThisWeek() > 0)
                .sorted(java.util.Comparator.comparingInt(ProxyLoadStatsDto::proxiesThisWeek))
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private ProxyRequest findRequestOrThrow(Long requestId) {
        return proxyRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Proxy request not found: " + requestId));
    }

    private void assertAddressedToCurrentUser(ProxyRequest request, Long currentUserId) {
        if (!request.getRequestedTo().getId().equals(currentUserId)) {
            throw new AccessDeniedException("This proxy request is not addressed to you");
        }
    }

    private void assertPending(ProxyRequest request) {
        if (request.getStatus() != ProxyRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Proxy request is already " + request.getStatus() + " — cannot modify");
        }
    }

    private ProxyRequestResponseDto toDto(ProxyRequest r) {
        r.getRequestedBy().getUuid(); // trigger lazy load safely within transaction
        r.getRequestedTo().getUuid();

        String byName = resolveUserDisplayName(r.getRequestedBy().getId());
        String toName = resolveUserDisplayName(r.getRequestedTo().getId());

        return new ProxyRequestResponseDto(
                r.getId(),
                r.getUuid(),
                r.getRequestedBy().getUuid(),
                byName,
                r.getRequestedTo().getUuid(),
                toName,
                r.getSubject(),
                r.getPeriodDate(),
                r.getSectionUuid(),
                r.getStatus(),
                Boolean.TRUE.equals(r.getIsAccepted()),
                r.getDeclineReason(),
                r.getCreatedAt()
        );
    }

    private String resolveUserDisplayName(Long userId) {
        Optional<Staff> staffOpt = staffRepository.findAll().stream()
                .filter(s -> s.getUser() != null && s.getUser().getId().equals(userId))
                .findFirst();

        if (staffOpt.isPresent() && staffOpt.get().getUserProfile() != null) {
            return resolveFullName(staffOpt.get());
        }
        // Fallback to username
        return userRepository.findByIdAsLong(userId)
                .map(User::getUsername)
                .orElse("Unknown");
    }

    private String resolveFullName(Staff staff) {
        if (staff.getUserProfile() == null) return staff.getUser() != null
                ? staff.getUser().getUsername() : "Unknown";
        var p = staff.getUserProfile();
        String full = (p.getFirstName() + " "
                + (p.getMiddleName() != null ? p.getMiddleName() + " " : "")
                + p.getLastName()).trim();
        return full.isBlank() ? staff.getUser().getUsername() : full;
    }

    private AbsentStaffDto buildAbsentStaffDto(StaffDailyAttendance attendance, LocalDate date) {
        Optional<Staff> staffOpt = staffRepository.findAll().stream()
                .filter(s -> s.getUser() != null && s.getUser().getId().equals(attendance.getStaffId()))
                .findFirst();

        if (staffOpt.isEmpty()) return null;
        Staff staff = staffOpt.get();

        String designation = staff.getDesignation() != null
                ? staff.getDesignation().getDesignationName()
                : staff.getJobTitle();

        boolean proxyCovered = proxyRequestRepository
                .existsByRequestedByIdAndPeriodDateAndStatusNot(
                        staff.getUser().getId(), date, ProxyRequestStatus.CANCELLED);

        return new AbsentStaffDto(
                staff.getUser().getUuid(),
                resolveFullName(staff),
                designation,
                date,
                proxyCovered
        );
    }
}
