package com.project.edusync.common.utils;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public final class PublicIdentifierResolver {

    private PublicIdentifierResolver() {
    }

    public static <T> T resolve(
            String identifier,
            Function<UUID, Optional<T>> uuidLookup,
            Function<Long, Optional<T>> idLookup,
            String entityName
    ) {
        if (identifier == null || identifier.isBlank()) {
            throw new EdusyncException("Identifier is required", HttpStatus.BAD_REQUEST);
        }

        if (looksLikeUuid(identifier)) {
            try {
                UUID uuid = UUID.fromString(identifier);
                return uuidLookup.apply(uuid)
                        .orElseThrow(() -> new ResourceNotFoundException(entityName + " not found with identifier: " + identifier));
            } catch (IllegalArgumentException ex) {
                throw new EdusyncException("Invalid identifier: " + identifier, HttpStatus.BAD_REQUEST);
            }
        }

        try {
            Long id = Long.parseLong(identifier);
            return idLookup.apply(id)
                    .orElseThrow(() -> new ResourceNotFoundException(entityName + " not found with identifier: " + identifier));
        } catch (NumberFormatException ex) {
            throw new EdusyncException("Invalid identifier: " + identifier, HttpStatus.BAD_REQUEST);
        }
    }

    private static boolean looksLikeUuid(String identifier) {
        return identifier.indexOf('-') >= 0;
    }
}

