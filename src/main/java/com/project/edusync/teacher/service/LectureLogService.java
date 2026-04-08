package com.project.edusync.teacher.service;

import com.project.edusync.teacher.model.dto.LectureLogRequestDto;
import com.project.edusync.teacher.model.dto.LectureLogResponseDto;
import com.project.edusync.teacher.model.dto.LectureLogUploadInitRequestDto;
import com.project.edusync.teacher.model.dto.LectureLogUploadInitResponseDto;

import java.util.UUID;

public interface LectureLogService {
    LectureLogResponseDto getLectureLog(Long teacherId, UUID scheduleUuid);
    LectureLogResponseDto saveLectureLog(Long teacherId, LectureLogRequestDto requestDto);
    LectureLogUploadInitResponseDto initUpload(Long teacherId, LectureLogUploadInitRequestDto request);
    /** Student-readable: find a log by scheduleUuid with no teacher ownership check. Returns empty if not logged yet. */
    java.util.Optional<LectureLogResponseDto> getLectureLogByScheduleUuid(UUID scheduleUuid);
}
