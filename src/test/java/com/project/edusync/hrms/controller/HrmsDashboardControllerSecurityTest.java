package com.project.edusync.hrms.controller;

import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.common.security.CustomUserDetailService;
import com.project.edusync.common.utils.RequestUtil;
import com.project.edusync.hrms.dto.dashboard.HrmsDashboardSummaryDTO;
import com.project.edusync.hrms.service.HrmsDashboardService;
import com.project.edusync.superadmin.audit.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HrmsDashboardController.class, properties = "api.url=/api/v1")
@AutoConfigureMockMvc
@Import(HrmsDashboardControllerSecurityTest.TestSecurityConfig.class)
class HrmsDashboardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HrmsDashboardService hrmsDashboardService;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private RequestUtil requestUtil;

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(withDefaults());
            return http.build();
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("admin")
                            .password("{noop}password")
                            .roles("ADMIN")
                            .build(),
                    User.withUsername("teacher")
                            .password("{noop}password")
                            .roles("TEACHER")
                            .build()
            );
        }

        @Bean
        AuthUtil authUtil() {
            return Mockito.mock(AuthUtil.class);
        }

        @Bean
        CustomUserDetailService customUserDetailService() {
            return Mockito.mock(CustomUserDetailService.class);
        }
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void summaryAllowedForAdmin() throws Exception {
        when(hrmsDashboardService.getSummary()).thenReturn(new HrmsDashboardSummaryDTO(
                0,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                List.of(),
                List.of(),
                List.of()
        ));

        mockMvc.perform(get("/api/v1/auth/hrms/dashboard/summary"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"TEACHER"})
    void summaryForbiddenForTeacher() throws Exception {
        mockMvc.perform(get("/api/v1/auth/hrms/dashboard/summary"))
                .andExpect(status().isForbidden());
    }
}

