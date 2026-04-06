package com.project.edusync.hrms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.common.security.CustomUserDetailService;
import com.project.edusync.common.utils.RequestUtil;
import com.project.edusync.hrms.dto.grade.StaffGradeCreateDTO;
import com.project.edusync.hrms.dto.grade.StaffGradeResponseDTO;
import com.project.edusync.hrms.model.enums.TeachingWing;
import com.project.edusync.hrms.service.StaffGradeService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StaffGradeController.class, properties = "api.url=/api/v1")
@AutoConfigureMockMvc
@Import(StaffGradeControllerSecurityTest.TestSecurityConfig.class)
class StaffGradeControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StaffGradeService staffGradeService;

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
    void listGradesAllowedForAdmin() throws Exception {
        when(staffGradeService.listGrades()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/auth/hrms/grades"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"TEACHER"})
    void listGradesForbiddenForTeacher() throws Exception {
        mockMvc.perform(get("/api/v1/auth/hrms/grades"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"SCHOOL_ADMIN"})
    void createGradeAllowedForSchoolAdmin() throws Exception {
        StaffGradeCreateDTO request = new StaffGradeCreateDTO(
                "PRT",
                "Primary Teacher",
                TeachingWing.PRIMARY,
                new BigDecimal("25000"),
                new BigDecimal("45000"),
                1,
                2,
                null
        );

        when(staffGradeService.createGrade(any())).thenReturn(new StaffGradeResponseDTO(
                1L, null, "PRT", "Primary Teacher", TeachingWing.PRIMARY,
                new BigDecimal("25000"), new BigDecimal("45000"), 1, 2, null,
                true, null, null
        ));

        mockMvc.perform(post("/api/v1/auth/hrms/grades")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = {"TEACHER"})
    void deleteGradeForbiddenForTeacher() throws Exception {
        doNothing().when(staffGradeService).deleteGrade(1L);

        mockMvc.perform(delete("/api/v1/auth/hrms/grades/1"))
                .andExpect(status().isForbidden());
    }
}
