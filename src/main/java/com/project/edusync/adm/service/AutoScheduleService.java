package com.project.edusync.adm.service;

import com.project.edusync.adm.model.dto.response.ScheduleResponseDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * Service for automated, conflict-free timetable generation using a Genetic Algorithm.
 */
public interface AutoScheduleService {

    /**
     * Starts an asynchronous timetable generation process and streams progress via SSE.
     * @param sectionId The section to generate for.
     * @return SseEmitter object for streaming updates.
     */
    SseEmitter generateTimetableStream(UUID sectionId);

    /**
     * Internal: Runs one generation of the GA and returns progress.
     */
    void processGeneration(UUID sectionId);
}
