package com.project.edusync.superadmin.audit.service;

import com.project.edusync.superadmin.audit.model.entity.AuditLog;
import com.project.edusync.superadmin.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditAsyncWriter {

    private final BlockingQueue<AuditLog> auditLogQueue;
    private final AuditLogRepository auditLogRepository;

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void flush() {
        List<AuditLog> batch = new ArrayList<>(200);
        auditLogQueue.drainTo(batch, 200);

        if (batch.isEmpty()) {
            return;
        }

        try {
            auditLogRepository.saveAll(batch);
            log.debug("Audit batch flushed: size={}", batch.size());
        } catch (Exception ex) {
            log.error("Audit batch write failed: size={}, error={}", batch.size(), ex.getMessage(), ex);
        }
    }
}



