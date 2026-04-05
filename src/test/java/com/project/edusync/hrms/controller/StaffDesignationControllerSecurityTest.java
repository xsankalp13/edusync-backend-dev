package com.project.edusync.hrms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.common.security.CustomUserDetailService;
import com.project.edusync.common.utils.RequestUtil;
import com.project.edusync.hrms.dto.designation.StaffDesignationCreateUpdateDTO;
import com.project.edusync.hrms.dto.designation.StaffDesignationResponseDTO;
import com.project.edusync.hrms.service.StaffDesignationService;
import com.project.edusync.superadmin.audit.service.AuditLogService;
import com.project.edusync.uis.model.enums.StaffCategory;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StaffDesignationController.class, properties = "api.url=/api/v1")
@AutoConfigureMockMvc
@Import(StaffDesignationControllerSecurityTest.TestSecurityConfig.class)
class StaffDesignationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StaffDesignationService staffDesignationService;

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
    void listAllowedForAdmin() throws Exception {
        when(staffDesignationService.list(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/auth/hrms/designations"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"TEACHER"})
    void listForbiddenForTeacher() throws Exception {
        mockMvc.perform(get("/api/v1/auth/hrms/designations"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"SCHOOL_ADMIN"})
    void createAllowedForSchoolAdmin() throws Exception {
        StaffDesignationCreateUpdateDTO request = new StaffDesignationCreateUpdateDTO(
                "PRT",
                "Primary Teacher",
                StaffCategory.TEACHING,
                null,
                10
        );

        when(staffDesignationService.create(any())).thenReturn(new StaffDesignationResponseDTO(
                1L,
                null,
                "PRT",
                "Primary Teacher",
                StaffCategory.TEACHING,
                null,
                10,
                true,
                null,
                null
        ));

        mockMvc.perform(post("/api/v1/auth/hrms/designations")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}

