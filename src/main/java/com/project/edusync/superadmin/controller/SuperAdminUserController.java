package com.project.edusync.superadmin.controller;

import com.project.edusync.common.model.dto.response.MessageResponse;
import com.project.edusync.superadmin.model.dto.GuardianSummaryDto;
import com.project.edusync.superadmin.model.dto.SuperAdminResetPasswordRequestDto;
import com.project.edusync.superadmin.model.dto.SuperAdminResetPasswordResponseDto;
import com.project.edusync.superadmin.service.SuperAdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("${api.url}/super")
@RequiredArgsConstructor
@Tag(name = "SuperAdmin User Control", description = "Guardian listing, session invalidation and password reset")
public class SuperAdminUserController {

    private final SuperAdminUserService superAdminUserService;

    @GetMapping("/users/guardians")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Paginated guardian list", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Page<GuardianSummaryDto>> listGuardians(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir) {

        int clampedSize = Math.min(Math.max(size, 1), 100);
        int pageIndex = Math.max(page, 0);
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageIndex, clampedSize, sort);
        return ResponseEntity.ok(superAdminUserService.listGuardians(search, pageable));
    }

    @PostMapping("/users/{staffUuid}/force-logout")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Force logout a specific staff user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<MessageResponse> forceLogout(
            @PathVariable UUID staffUuid) {

        MessageResponse response = superAdminUserService.forceLogout(staffUuid);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/invalidate-all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Force logout all users", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<MessageResponse> invalidateAllSessions() {
        MessageResponse response = superAdminUserService.invalidateAllSessions();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{staffUuid}/reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Reset staff user password", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<SuperAdminResetPasswordResponseDto> resetPassword(
            @PathVariable UUID staffUuid,
            @RequestBody(required = false) SuperAdminResetPasswordRequestDto requestBody) {

        String newPassword = requestBody == null ? null : requestBody.newPassword();
        SuperAdminResetPasswordResponseDto response = superAdminUserService.resetPassword(staffUuid, newPassword);

        return ResponseEntity.ok(response);
    }
}



