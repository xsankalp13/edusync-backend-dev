package com.project.edusync.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TimeUtilsTest {

    @Test
    void isoNow_shouldReturnParsableInstant() {
        String now = TimeUtils.isoNow();
        assertNotNull(now);
        assertFalse(now.isBlank());
        Instant parsed = Instant.parse(now);
        assertNotNull(parsed);
    }
}

