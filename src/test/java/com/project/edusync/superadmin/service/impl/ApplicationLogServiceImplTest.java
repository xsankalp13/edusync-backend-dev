package com.project.edusync.superadmin.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.superadmin.model.dto.LogTailResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationLogServiceImplTest {

    @Test
    void tailLogsReturnsMostRecentLines() throws IOException {
        Path logFile = Files.createTempFile("edusync-log", ".log");
        Files.write(logFile, List.of(
                "2026-03-31T10:00:00.000Z INFO 1 --- [main] com.project.Test : line-1",
                "2026-03-31T10:00:01.000Z WARN 1 --- [main] com.project.Test : line-2",
                "2026-03-31T10:00:02.000Z ERROR 1 --- [main] com.project.Test : line-3"
        ));

        ApplicationLogServiceImpl service = new ApplicationLogServiceImpl();
        ReflectionTestUtils.setField(service, "configuredLogFile", logFile.toString());

        LogTailResponseDto response = service.tailLogs(2, null);

        assertEquals(2, response.totalLinesReturned());
        assertEquals("WARN", response.entries().get(0).level());
        assertEquals("ERROR", response.entries().get(1).level());
        assertEquals("line-3", response.entries().get(1).message());
    }

    @Test
    void tailLogsFiltersByLevel() throws IOException {
        Path logFile = Files.createTempFile("edusync-log", ".log");
        Files.write(logFile, List.of(
                "2026-03-31T10:00:00.000Z INFO 1 --- [main] com.project.Test : line-1",
                "2026-03-31T10:00:01.000Z ERROR 1 --- [main] com.project.Test : line-2",
                "2026-03-31T10:00:02.000Z ERROR 1 --- [main] com.project.Test : line-3"
        ));

        ApplicationLogServiceImpl service = new ApplicationLogServiceImpl();
        ReflectionTestUtils.setField(service, "configuredLogFile", logFile.toString());

        LogTailResponseDto response = service.tailLogs(200, "ERROR");

        assertEquals(2, response.totalLinesReturned());
        assertTrue(response.entries().stream().allMatch(entry -> "ERROR".equals(entry.level())));
    }

    @Test
    void tailLogsThrowsNotFoundWhenLogFileNotConfigured() {
        ApplicationLogServiceImpl service = new ApplicationLogServiceImpl();
        ReflectionTestUtils.setField(service, "configuredLogFile", "");

        EdusyncException ex = assertThrows(EdusyncException.class, () -> service.tailLogs(100, null));

        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }
}

