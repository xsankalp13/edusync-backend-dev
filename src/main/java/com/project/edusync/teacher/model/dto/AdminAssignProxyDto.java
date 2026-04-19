package com.project.edusync.teacher.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Admin payload for manually assigning a proxy teacher to cover an absent teacher's class.
 */
public record AdminAssignProxyDto(

        /** UUID of the absent / requesting-off teacher's User. */
        @NotNull(message = "absentStaffUserUuid is required")
        UUID absentStaffUserUuid,

        /** UUID of the proxy (substitute) teacher's User. */
        @NotNull(message = "proxyStaffUserUuid is required")
        UUID proxyStaffUserUuid,

        /** Date the proxy class falls on. */
        @NotNull(message = "periodDate is required")
        LocalDate periodDate,

        /** Section to be covered. */
        UUID sectionUuid,

        /** Subject description. */
        @NotBlank(message = "subject is required")
        String subject
) {}
