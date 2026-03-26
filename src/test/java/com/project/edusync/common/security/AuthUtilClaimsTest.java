package com.project.edusync.common.security;

import com.project.edusync.iam.model.entity.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthUtilClaimsTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generatesAndExtractsDashboardClaims() {
        AuthUtil authUtil = new AuthUtil();
        ReflectionTestUtils.setField(authUtil, "secretKey", "01234567890123456789012345678901");
        ReflectionTestUtils.setField(authUtil, "jwtExpirationTime", 3600000L);
        authUtil.init();

        Role role = new Role();
        role.setName("profile:read:own");

        Long userId = 42L;
        Long academicYearId = 2026L;
        String token = authUtil.generateAccessToken("student.user", Set.of(role), userId, academicYearId);

        assertEquals(userId, ((Number) authUtil.getClaimValueFromToken(token, "user_id")).longValue());
        assertEquals(academicYearId, ((Number) authUtil.getClaimValueFromToken(token, "academic_year_id")).longValue());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("student.user", null, authUtil.getAuthoritiesFromToken(token));
        authentication.setDetails(Map.of("academic_year_id", authUtil.getClaimValueFromToken(token, "academic_year_id")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertEquals(academicYearId, authUtil.getCurrentAcademicYearId());
    }
}

