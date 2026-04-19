package com.project.edusync.util;

import java.time.Instant;

/**
 * Small time utilities used across the application.
 */
public final class TimeUtils {

    private TimeUtils() {}

    /**
     * Return current instant in ISO-8601 UTC format (e.g. 2026-04-15T12:34:56.789Z)
     */
    public static String isoNow() {
        return Instant.now().toString();
    }
}

