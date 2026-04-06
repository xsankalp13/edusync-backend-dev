package com.project.edusync.superadmin.audit.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuditRequestCachingFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String contentType = request.getContentType();
        String path = request.getRequestURI();

        if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
            return true;
        }

        if (path == null) {
            return false;
        }

        String normalizedPath = path.toLowerCase();
        return normalizedPath.contains("/upload")
                || normalizedPath.contains("/import")
                || normalizedPath.contains("/profile/image")
                || normalizedPath.contains("/bulk");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        HttpServletRequest wrapped = request instanceof ContentCachingRequestWrapper
                ? request
                : new ContentCachingRequestWrapper(request);

        filterChain.doFilter(wrapped, response);
    }
}



