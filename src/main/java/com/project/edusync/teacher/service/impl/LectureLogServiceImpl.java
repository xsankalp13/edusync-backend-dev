package com.project.edusync.teacher.service.impl;

import com.project.edusync.adm.model.entity.Schedule;
import com.project.edusync.adm.repository.ScheduleRepository;
import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.UserRepository;
import com.project.edusync.teacher.model.dto.LectureLogRequestDto;
import com.project.edusync.teacher.model.dto.LectureLogResponseDto;
import com.project.edusync.teacher.model.dto.LectureLogUploadInitRequestDto;
import com.project.edusync.teacher.model.dto.LectureLogUploadInitResponseDto;
import com.project.edusync.teacher.model.entity.LectureLog;
import com.project.edusync.teacher.repository.LectureLogRepository;
import com.project.edusync.teacher.service.LectureLogService;
import com.project.edusync.uis.config.MediaUploadProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LectureLogServiceImpl implements LectureLogService {

    private final LectureLogRepository lectureLogRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final MediaUploadProperties mediaUploadProperties;

    @Override
    @Transactional(readOnly = true)
    public LectureLogResponseDto getLectureLog(Long teacherId, UUID scheduleUuid) {
        return lectureLogRepository.findByScheduleUuidAndTeacherId(scheduleUuid, teacherId.intValue())
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("LectureLog", "scheduleUuid", scheduleUuid));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<LectureLogResponseDto> getLectureLogByScheduleUuid(UUID scheduleUuid) {
        return lectureLogRepository.findByScheduleUuid(scheduleUuid).map(this::toDto);
    }

    @Override
    @Transactional
    public LectureLogResponseDto saveLectureLog(Long teacherId, LectureLogRequestDto requestDto) {
        User teacher = userRepository.findById(teacherId.intValue())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherId));

        Schedule schedule = scheduleRepository.findActiveById(requestDto.getScheduleUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "uuid", requestDto.getScheduleUuid()));

        Optional<LectureLog> existingOpt = lectureLogRepository.findByScheduleAndTeacher(schedule, teacher);

        LectureLog logToSave = existingOpt.orElseGet(() -> {
            LectureLog newLog = new LectureLog();
            newLog.setSchedule(schedule);
            newLog.setTeacher(teacher);
            return newLog;
        });

        logToSave.setTitle(requestDto.getTitle());
        logToSave.setDescription(requestDto.getDescription());
        logToSave.setDocumentUrls(requestDto.getDocumentUrls() != null ? requestDto.getDocumentUrls() : new java.util.ArrayList<>());
        logToSave.setHasTakenTest(requestDto.isHasTakenTest());

        LectureLog savedLog = lectureLogRepository.save(logToSave);
        return toDto(savedLog);
    }

    @Override
    @Transactional(readOnly = true)
    public LectureLogUploadInitResponseDto initUpload(Long teacherId, LectureLogUploadInitRequestDto request) {
        User user = userRepository.findById(teacherId.intValue())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherId));

        // Use auto/upload to support documents (PDF/Word) on Cloudinary
        String provider = mediaUploadProperties.getProvider() != null ? mediaUploadProperties.getProvider().trim().toLowerCase() : "cloudinary";
        
        if ("cloudinary".equals(provider)) {
            MediaUploadProperties.Cloudinary cfg = mediaUploadProperties.getCloudinary();
            if (!StringUtils.hasText(cfg.getCloudName()) || !StringUtils.hasText(cfg.getApiKey()) || !StringUtils.hasText(cfg.getApiSecret())) {
                throw new EdusyncException("Cloudinary upload is not configured properly.", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            String objectKey = "lectures/" + user.getUuid() + "/" + Instant.now().getEpochSecond() + "_" + UUID.randomUUID();
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String folder = cfg.getFolder() + "_documents"; // Use a distinct folder or reuse
            
            String signatureBase = "folder=" + folder + "&public_id=" + objectKey + "&timestamp=" + timestamp;
            String signature = sha1Hex(signatureBase + cfg.getApiSecret());

            Map<String, String> fields = new HashMap<>();
            fields.put("api_key", cfg.getApiKey());
            fields.put("timestamp", timestamp);
            fields.put("signature", signature);
            fields.put("folder", folder);
            fields.put("public_id", objectKey);

            return LectureLogUploadInitResponseDto.builder()
                    .provider("cloudinary")
                    .method("POST")
                    .uploadUrl("https://api.cloudinary.com/v1_1/" + cfg.getCloudName() + "/auto/upload")
                    .objectKey(objectKey)
                    .expiresAt(Instant.now().plusSeconds(mediaUploadProperties.getUploadInitTtlSeconds()))
                    .fields(fields)
                    .headers(Map.of())
                    .build();
        }

        throw new EdusyncException("Unsupported media provider for documents: " + provider, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private LectureLogResponseDto toDto(LectureLog entity) {
        return LectureLogResponseDto.builder()
                .uuid(entity.getUuid())
                .scheduleUuid(entity.getSchedule().getUuid())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .documentUrls(entity.getDocumentUrls())
                .hasTakenTest(entity.isHasTakenTest())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String sha1Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new EdusyncException("Could not generate upload signature.", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}
