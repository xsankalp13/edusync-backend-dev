package com.project.edusync.superadmin.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SuperAdminResetPasswordResponseDto(
        String message,
        String temporaryPassword
) {
}


