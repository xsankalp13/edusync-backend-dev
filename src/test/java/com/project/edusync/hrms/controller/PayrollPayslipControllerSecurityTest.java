package com.project.edusync.hrms.controller;

import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.common.security.CustomUserDetailService;
import com.project.edusync.common.utils.RequestUtil;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PayrollPayslipController.class, properties = "api.url=/api/v1")
@AutoConfigureMockMvc
@Import(PayrollPayslipControllerSecurityTest.TestSecurityConfig.class)
class PayrollPayslipControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

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
    void listRunPayslipsAllowedForAdmin() throws Exception {
        when(payrollService.listPayslipsByRun(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/auth/hrms/payroll/runs/1/payslips"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"TEACHER"})
    void getPayslipForbiddenForTeacher() throws Exception {
        mockMvc.perform(get("/api/v1/auth/hrms/payroll/payslips/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void listStaffPayslipsAllowedForAdmin() throws Exception {
        when(payrollService.listPayslipsByStaff(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/auth/hrms/payroll/staff/101/payslips"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void downloadPayslipPdfAllowedForAdmin() throws Exception {
        when(payrollService.getPayslipPdf(1L)).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/v1/auth/hrms/payroll/payslips/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType(APPLICATION_PDF));
    }

    @Test
    @WithMockUser(roles = {"TEACHER"})
    void downloadPayslipPdfForbiddenForTeacher() throws Exception {
        mockMvc.perform(get("/api/v1/auth/hrms/payroll/payslips/1/pdf"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"TEACHER"})
    void listMyPayslipsAllowedForTeacher() throws Exception {
        when(payrollService.listMyPayslips(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/auth/hrms/payroll/self/payslips"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"TEACHER"})
    void downloadMyPayslipPdfAllowedForTeacher() throws Exception {
        when(payrollService.getMyPayslipPdf(1L)).thenReturn(new byte[]{4, 5, 6});

        mockMvc.perform(get("/api/v1/auth/hrms/payroll/self/payslips/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType(APPLICATION_PDF));
    }
}




