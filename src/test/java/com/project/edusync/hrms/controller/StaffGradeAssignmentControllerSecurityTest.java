package com.project.edusync.hrms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.common.security.CustomUserDetailService;
import com.project.edusync.common.utils.RequestUtil;
import com.project.edusync.hrms.dto.grade.StaffGradeAssignmentCreateDTO;
import com.project.edusync.hrms.dto.grade.StaffGradeAssignmentResponseDTO;
import com.project.edusync.hrms.service.StaffGradeAssignmentService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StaffGradeAssignmentController.class, properties = "api.url=/api/v1")
@AutoConfigureMockMvc
@Import(StaffGradeAssignmentControllerSecurityTest.TestSecurityConfig.class)
class StaffGradeAssignmentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StaffGradeAssignmentService staffGradeAssignmentService;

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
    @WithMockUser(roles = {"SUPER_ADMIN"})
    void assignAllowedForSuperAdmin() throws Exception {
        StaffGradeAssignmentCreateDTO request = new StaffGradeAssignmentCreateDTO(
                "66666666-6666-6666-6666-666666666666",
                "77777777-7777-7777-7777-777777777777",
                LocalDate.of(2026, 4, 1),
                "ORD-2026-001",
                "Promotion"
        );

        when(staffGradeAssignmentService.assign(any())).thenReturn(new StaffGradeAssignmentResponseDTO(
                1L, null, 101L, "Ravi Kumar", 2L, "TGT", "Trained Graduate Teacher",
                LocalDate.of(2026, 4, 1), null, "ORD-2026-001", 201L, "Promotion", null
        ));

        mockMvc.perform(post("/api/v1/auth/hrms/grades/assign")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = {"TEACHER"})
    void assignForbiddenForTeacher() throws Exception {
        StaffGradeAssignmentCreateDTO request = new StaffGradeAssignmentCreateDTO(
                "66666666-6666-6666-6666-666666666666",
                "77777777-7777-7777-7777-777777777777",
                LocalDate.of(2026, 4, 1),
                "ORD-2026-001",
                "Promotion"
        );

        mockMvc.perform(post("/api/v1/auth/hrms/grades/assign")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"SCHOOL_ADMIN"})
    void listAssignmentsAllowedForSchoolAdmin() throws Exception {
        when(staffGradeAssignmentService.listCurrentAssignments(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/auth/hrms/grades/assignments"))
                .andExpect(status().isOk());
    }
}
