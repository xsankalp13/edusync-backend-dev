package com.project.edusync.teacher.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Payload for creating a peer proxy request (teacher → teacher).
 */
public record ProxyRequestCreateDto(

        /** UUID of the teacher being asked to cover. */
        @NotNull(message = "requestedToUserUuid is required")
        UUID requestedToUserUuid,

        /** Subject / topic being proxied. */
        @NotBlank(message = "subject is required")
        String subject,

        /** Date on which the proxy class falls. Defaults to today if null. */
        LocalDate periodDate,

        /** Section UUID that needs to be covered. */
        UUID sectionUuid
) {}
