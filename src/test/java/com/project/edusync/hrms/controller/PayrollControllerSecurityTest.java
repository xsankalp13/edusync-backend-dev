package com.project.edusync.hrms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.common.security.CustomUserDetailService;
import com.project.edusync.common.utils.RequestUtil;
import com.project.edusync.hrms.dto.payroll.PayrollRunCreateDTO;
import com.project.edusync.hrms.dto.payroll.PayrollRunResponseDTO;
import com.project.edusync.hrms.model.enums.PayrollRunStatus;
import com.project.edusync.hrms.service.PayrollService;
import com.project.edusync.superadmin.audit.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PayrollController.class, properties = "api.url=/api/v1")
@AutoConfigureMockMvc
@Import(PayrollControllerSecurityTest.TestSecurityConfig.class)
class PayrollControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PayrollService payrollService;

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
    void createRunAllowedForAdmin() throws Exception {
        when(payrollService.createRun(any())).thenReturn(new PayrollRunResponseDTO(
                1L,
                null,
                2026,
                4,
                PayrollRunStatus.PROCESSED,
                "April payroll",
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                LocalDateTime.now(),
                List.of()
        ));

        mockMvc.perform(post("/api/v1/auth/hrms/payroll/runs")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PayrollRunCreateDTO(2026, 4, "April payroll"))))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = {"TEACHER"})
    void createRunForbiddenForTeacher() throws Exception {
        mockMvc.perform(post("/api/v1/auth/hrms/payroll/runs")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PayrollRunCreateDTO(2026, 4, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"SCHOOL_ADMIN"})
    void listRunsAllowedForSchoolAdmin() throws Exception {
        when(payrollService.listRuns(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/auth/hrms/payroll/runs"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void approveRunAllowedForAdmin() throws Exception {
        when(payrollService.approveRun(1L)).thenReturn(new PayrollRunResponseDTO(
                1L,
                null,
                2026,
                4,
                PayrollRunStatus.APPROVED,
                "Approved",
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                LocalDateTime.now(),
                List.of()
        ));

        mockMvc.perform(post("/api/v1/auth/hrms/payroll/runs/1/approve"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"TEACHER"})
    void disburseRunForbiddenForTeacher() throws Exception {
        mockMvc.perform(post("/api/v1/auth/hrms/payroll/runs/1/disburse"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"TEACHER"})
    void getRunForbiddenForTeacher() throws Exception {
        mockMvc.perform(get("/api/v1/auth/hrms/payroll/runs/1"))
                .andExpect(status().isForbidden());
    }
}


