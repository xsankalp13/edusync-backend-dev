package com.project.edusync.superadmin.audit.config;

import com.project.edusync.superadmin.audit.interceptor.AuditOperationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class AuditLoggingWebConfig implements WebMvcConfigurer {

    private final AuditOperationInterceptor auditOperationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditOperationInterceptor).addPathPatterns("/**");
    }
}

