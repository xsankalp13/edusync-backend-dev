package com.project.edusync.hrms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.common.security.CustomUserDetailService;
import com.project.edusync.common.utils.RequestUtil;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingBulkCreateDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingCreateDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingUpdateDTO;
import com.project.edusync.hrms.service.StaffSalaryMappingService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StaffSalaryMappingController.class, properties = "api.url=/api/v1")
@AutoConfigureMockMvc
@Import(StaffSalaryMappingControllerSecurityTest.TestSecurityConfig.class)
class StaffSalaryMappingControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StaffSalaryMappingService staffSalaryMappingService;

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
    void listMappingsAllowedForAdmin() throws Exception {
        when(staffSalaryMappingService.listMappings(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/auth/hrms/salary/mappings"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"TEACHER"})
    void listMappingsForbiddenForTeacher() throws Exception {
        mockMvc.perform(get("/api/v1/auth/hrms/salary/mappings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"SCHOOL_ADMIN"})
    void getByStaffIdentifierAllowedForSchoolAdmin() throws Exception {
        when(staffSalaryMappingService.getMappingsByStaffIdentifier("101")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/auth/hrms/salary/mappings/staff/101"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"TEACHER"})
    void getByStaffIdentifierForbiddenForTeacher() throws Exception {
        mockMvc.perform(get("/api/v1/auth/hrms/salary/mappings/staff/101"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"TEACHER"})
    void createMappingForbiddenForTeacher() throws Exception {
        StaffSalaryMappingCreateDTO request = new StaffSalaryMappingCreateDTO(
                "101",
                "10",
                LocalDate.of(2026, 4, 1),
                null,
                "Mapping",
                List.of()
        );

        mockMvc.perform(post("/api/v1/auth/hrms/salary/mappings")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void updateAllowedForAdmin() throws Exception {
        StaffSalaryMappingUpdateDTO request = new StaffSalaryMappingUpdateDTO(
                "10",
                LocalDate.of(2026, 4, 1),
                null,
                "Updated mapping",
                List.of()
        );

        mockMvc.perform(put("/api/v1/auth/hrms/salary/mappings/1")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"TEACHER"})
    void updateForbiddenForTeacher() throws Exception {
        StaffSalaryMappingUpdateDTO request = new StaffSalaryMappingUpdateDTO(
                "10",
                LocalDate.of(2026, 4, 1),
                null,
                "Updated mapping",
                List.of()
        );

        mockMvc.perform(put("/api/v1/auth/hrms/salary/mappings/1")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"SCHOOL_ADMIN"})
    void bulkCreateAllowedForSchoolAdmin() throws Exception {
        StaffSalaryMappingBulkCreateDTO request = new StaffSalaryMappingBulkCreateDTO(
                "10",
                List.of("101", "102"),
                LocalDate.of(2026, 4, 1),
                null,
                "Bulk"
        );

        mockMvc.perform(post("/api/v1/auth/hrms/salary/mappings/bulk")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = {"SUPER_ADMIN"})
    void computedAllowedForSuperAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/auth/hrms/salary/mappings/1/computed"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"TEACHER"})
    void computedForbiddenForTeacher() throws Exception {
        mockMvc.perform(get("/api/v1/auth/hrms/salary/mappings/1/computed"))
                .andExpect(status().isForbidden());
    }
}


