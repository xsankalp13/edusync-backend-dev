package com.project.edusync.ams.model.service.implementation;

import com.project.edusync.ams.model.dto.request.LateClockInReviewDTO;
import com.project.edusync.ams.model.dto.response.LateClockInRequestDTO;
import com.project.edusync.ams.model.entity.AttendanceType;
import com.project.edusync.ams.model.entity.LateClockInRequest;
import com.project.edusync.ams.model.enums.LateClockInStatus;
import com.project.edusync.ams.model.repository.AttendanceTypeRepository;
import com.project.edusync.ams.model.repository.LateClockInRequestRepository;
import com.project.edusync.ams.model.repository.StaffDailyAttendanceRepository;
import com.project.edusync.ams.model.service.LateClockInService;
import com.project.edusync.uis.repository.StaffRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LateClockInServiceImpl implements LateClockInService {

    private final LateClockInRequestRepository lateClockInRepo;
    private final StaffDailyAttendanceRepository attendanceRepo;
    private final AttendanceTypeRepository attendanceTypeRepo;
    private final StaffRepository staffRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<LateClockInRequestDTO> listRequests(LateClockInStatus status, Pageable pageable) {
        return lateClockInRepo.findAllByStatus(status, pageable)
                .map(this::toDTO);
    }

    @Override
    public long countPending() {
        return lateClockInRepo.countByStatus(LateClockInStatus.PENDING);
    }

    @Override
    @Transactional
    public LateClockInRequestDTO review(UUID uuid, LateClockInReviewDTO dto, Long adminUserId) {
        LateClockInRequest request = lateClockInRepo.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("LateClockInRequest not found: " + uuid));

        if (request.getStatus() != LateClockInStatus.PENDING) {
            throw new IllegalStateException("This request has already been reviewed.");
        }

        boolean approved = "APPROVE".equalsIgnoreCase(dto.getAction());
        request.setStatus(approved ? LateClockInStatus.APPROVED : LateClockInStatus.REJECTED);
        request.setAdminRemarks(dto.getRemarks());
        request.setReviewedBy(adminUserId);

        // On APPROVAL — upgrade the attendance record to PRESENT
        if (approved && request.getAttendance() != null) {
            AttendanceType presentType = attendanceTypeRepo.findByShortCodeIgnoreCase("P")
                    .orElse(null);
            if (presentType != null) {
                var attendance = request.getAttendance();
                attendance.setAttendanceType(presentType);
                String note = "Attendance upgraded to PRESENT by admin after late clock-in review.";
                attendance.setNotes(note);
                attendanceRepo.save(attendance);
                log.info("LateClockIn APPROVED — upgraded attendance for staffId={} date={}",
                        request.getStaffId(), request.getAttendanceDate());
            }
        }

        lateClockInRepo.save(request);
        return toDTO(request);
    }

    // ── Mapper ─────────────────────────────────────────────────────────────────

    private LateClockInRequestDTO toDTO(LateClockInRequest req) {
        var builder = LateClockInRequestDTO.builder()
                .uuid(req.getUuid())
                .staffId(req.getStaffId())
                .attendanceDate(req.getAttendanceDate())
                .clockInTime(req.getClockInTime())
                .minutesLate(req.getMinutesLate())
                .reason(req.getReason())
                .status(req.getStatus())
                .adminRemarks(req.getAdminRemarks())
                .createdAt(req.getCreatedAt() != null ? java.time.ZonedDateTime.of(req.getCreatedAt(), java.time.ZoneId.systemDefault()) : null);

        // Enrich with staff details
        staffRepository.findById(req.getStaffId()).ifPresent(staff -> {
            String firstName = staff.getUserProfile() != null ? staff.getUserProfile().getFirstName() : "";
            String lastName  = staff.getUserProfile() != null ? staff.getUserProfile().getLastName()  : "";
            builder.staffName((firstName + " " + lastName).trim());
            builder.employeeId(staff.getEmployeeId());
            builder.designation(staff.getDesignation() != null
                    ? staff.getDesignation().getDesignationName() : null);
        });

        return builder.build();
    }
}
