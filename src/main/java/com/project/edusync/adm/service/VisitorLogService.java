package com.project.edusync.adm.service;

import com.project.edusync.adm.model.dto.VisitorLogDto;
import com.project.edusync.adm.model.entity.VisitorLog;
import com.project.edusync.adm.repository.VisitorLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VisitorLogService {

    private final VisitorLogRepository visitorLogRepository;

    @Transactional
    public VisitorLogDto createVisitorLog(VisitorLogDto dto) {
        VisitorLog log = new VisitorLog();
        log.setName(dto.getName());
        log.setGender(dto.getGender());
        log.setPhoneNo(dto.getPhoneNo());
        log.setPurpose(dto.getPurpose());
        log.setWhomToMeet(dto.getWhomToMeet());

        // Mask Aadhaar before saving. Example: "XXXX-XXXX-1234"
        String aadhaar = dto.getAadhaarNo();
        if (aadhaar != null && aadhaar.length() >= 4) {
            String lastFour = aadhaar.substring(aadhaar.length() - 4);
            log.setAadhaarNo("XXXX-XXXX-" + lastFour);
        } else {
            log.setAadhaarNo(aadhaar);
        }

        VisitorLog savedLog = visitorLogRepository.save(log);
        return mapToDto(savedLog);
    }

    @Transactional(readOnly = true)
    public List<VisitorLogDto> getVisitorLogs(String period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        if ("weekly".equalsIgnoreCase(period)) {
            startDate = now.minusDays(7);
        } else if ("monthly".equalsIgnoreCase(period)) {
            startDate = now.minusDays(30);
        } else {
            // Default to daily
            startDate = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        }

        List<VisitorLog> logs = visitorLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, now);
        return logs.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private VisitorLogDto mapToDto(VisitorLog log) {
        VisitorLogDto dto = new VisitorLogDto();
        dto.setId(log.getUuid());
        dto.setName(log.getName());
        dto.setGender(log.getGender());
        dto.setAadhaarNo(log.getAadhaarNo());
        dto.setPhoneNo(log.getPhoneNo());
        dto.setPurpose(log.getPurpose());
        dto.setWhomToMeet(log.getWhomToMeet());
        dto.setVisitTime(log.getCreatedAt());
        return dto;
    }
}
