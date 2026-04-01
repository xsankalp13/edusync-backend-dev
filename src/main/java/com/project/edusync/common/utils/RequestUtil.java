package com.project.edusync.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class RequestUtil {
    /**
     * Helper method to extract the client's IP address from the request.
     * This is the standard "best practice" method.
     */
    public String getClientIp(HttpServletRequest request) {
        // 'X-Forwarded-For' is the most common header added by proxies and
        // load balancers (like NGINX, AWS ELB, etc.) to store the
        // *original* client's IP.
        String xfHeader = request.getHeader("X-Forwarded-For");
        log.trace("Attempting IP extraction using X-Forwarded-For");
        if (xfHeader != null && StringUtils.hasText(xfHeader)) {
            // It can be a list: "client, proxy1, proxy2". We want the first one.
            String extractedIp = xfHeader.split(",")[0].trim();
            log.trace("IP extracted from request: {}", extractedIp);
            return extractedIp;
        }
        log.trace("IP extraction failed using X-Forwarded-For, falling back to remote address: {}", request.getRemoteAddr());
        // If 'X-Forwarded-For' is not present, we fall back to the
        // IP of the machine that made the direct request (which might
        // be a proxy, but it's the best we have).
        return request.getRemoteAddr();
    }
}
