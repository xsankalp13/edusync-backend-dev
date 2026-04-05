package com.project.edusync.hrms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.common.security.CustomUserDetailService;
import com.project.edusync.common.utils.RequestUtil;
import com.project.edusync.hrms.dto.leave.LeaveApplicationCreateDTO;
import com.project.edusync.hrms.service.LeaveManagementService;
import com.project.edusync.superadmin.audit.service.AuditLogService;
import com.project.edusync.uis.repository.StaffRepository;
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

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LeaveManagementController.class, properties = "api.url=/api/v1")
@AutoConfigureMockMvc
@Import(LeaveManagementControllerSecurityTest.TestSecurityConfig.class)
class LeaveManagementControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LeaveManagementService leaveManagementService;

    @MockitoBean
    private StaffRepository staffRepository;

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
                    User.withUsername("teacher")
                            .password("{noop}password")
                            .roles("TEACHER")
                            .build(),
                    User.withUsername("parent")
                            .password("{noop}password")
                            .roles("PARENT")
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
    @WithMockUser(roles = {"TEACHER"})
    void applyForLeaveAllowedForTeacherWithUuidLeaveTypeRef() throws Exception {
        LeaveApplicationCreateDTO request = new LeaveApplicationCreateDTO(
                "55555555-5555-5555-5555-555555555555",
                LocalDate.of(2026, 4, 14),
                LocalDate.of(2026, 4, 14),
                false,
                null,
                "Personal work",
                null
        );

        when(leaveManagementService.applyForCurrentStaff(any())).thenReturn(null);

        mockMvc.perform(post("/api/v1/auth/hrms/leaves/applications")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = {"PARENT"})
    void applyForLeaveForbiddenForUnauthorizedRole() throws Exception {
        LeaveApplicationCreateDTO request = new LeaveApplicationCreateDTO(
                "55555555-5555-5555-5555-555555555555",
                LocalDate.of(2026, 4, 14),
                LocalDate.of(2026, 4, 14),
                false,
                null,
                "Personal work",
                null
        );

        mockMvc.perform(post("/api/v1/auth/hrms/leaves/applications")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}

