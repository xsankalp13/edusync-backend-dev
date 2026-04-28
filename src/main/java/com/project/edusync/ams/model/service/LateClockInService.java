package com.project.edusync.ams.model.service;

import com.project.edusync.ams.model.dto.request.LateClockInReviewDTO;
import com.project.edusync.ams.model.dto.response.LateClockInRequestDTO;
import com.project.edusync.ams.model.enums.LateClockInStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LateClockInService {

    /** Returns paginated list of late clock-in requests, optionally filtered by status */
    Page<LateClockInRequestDTO> listRequests(LateClockInStatus status, Pageable pageable);

    /** Returns count of PENDING requests */
    long countPending();

    /** Admin approves or rejects a specific request */
    LateClockInRequestDTO review(UUID uuid, LateClockInReviewDTO dto, Long adminUserId);
}
