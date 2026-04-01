package com.project.edusync.superadmin.service;

import com.project.edusync.common.model.dto.response.MessageResponse;
import com.project.edusync.superadmin.model.dto.GuardianSummaryDto;
import com.project.edusync.superadmin.model.dto.SuperAdminResetPasswordResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SuperAdminUserService {

    Page<GuardianSummaryDto> listGuardians(String search, Pageable pageable);

    MessageResponse forceLogout(UUID staffUuid);

    MessageResponse invalidateAllSessions();

    SuperAdminResetPasswordResponseDto resetPassword(UUID staffUuid, String newPassword);
}


