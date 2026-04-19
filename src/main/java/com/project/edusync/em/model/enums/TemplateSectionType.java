package com.project.edusync.em.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TemplateSectionType {
    FIXED,
    OPTIONAL;

    @JsonCreator
    public static TemplateSectionType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return FIXED;
        }
        if ("NORMAL".equalsIgnoreCase(value.trim())) {
            // Backward compatibility for older payloads/snapshots.
            return FIXED;
        }
        return TemplateSectionType.valueOf(value.trim().toUpperCase());
    }
}

