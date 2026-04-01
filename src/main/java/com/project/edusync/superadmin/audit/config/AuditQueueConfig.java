package com.project.edusync.superadmin.audit.config;

import com.project.edusync.superadmin.audit.model.entity.AuditLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class AuditQueueConfig {

    @Bean
    public BlockingQueue<AuditLog> auditLogQueue() {
        return new LinkedBlockingQueue<>(10_000);
    }
}

