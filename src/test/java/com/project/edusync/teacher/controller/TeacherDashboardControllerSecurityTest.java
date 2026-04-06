package com.project.edusync.teacher.controller;

import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.common.security.CustomUserDetailService;
import com.project.edusync.common.utils.RequestUtil;
import com.project.edusync.superadmin.audit.service.AuditLogService;
import com.project.edusync.teacher.model.dto.TeacherDashboardSummaryResponseDto;
import com.project.edusync.teacher.model.dto.TeacherHomeroomResponseDto;
import com.project.edusync.teacher.service.AttendanceExportService;
import com.project.edusync.teacher.service.TeacherDashboardService;
import org.junit.jupiter.api.Test;
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
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TeacherDashboardController.class)
@AutoConfigureMockMvc
@Import(TeacherDashboardControllerSecurityTest.TestSecurityConfig.class)
class TeacherDashboardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TeacherDashboardService teacherDashboardService;

    @MockitoBean
    private AttendanceExportService attendanceExportService;

    @MockitoBean
    private AuthUtil authUtil;

    @MockitoBean
    private CustomUserDetailService customUserDetailService;

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
                    User.withUsername("student")
                            .password("{noop}password")
                            .roles("STUDENT")
                            .build()
            );
        }
    }

    @Test
    @WithMockUser(roles = {"TEACHER"})
    void dashboardSummaryReturnsAttendanceDtoShape() throws Exception {
        LocalDate date = LocalDate.of(2026, 4, 6);

        when(authUtil.getCurrentUserId()).thenReturn(42L);
        when(teacherDashboardService.getDashboardSummary(eq(42L), eq(date)))
                .thenReturn(TeacherDashboardSummaryResponseDto.builder()
                        .date(date)
                        .totalStudents(145)
                        .classesToday(6)
                        .attendance(TeacherHomeroomResponseDto.TodayAttendance.builder()
                                .present(138)
                                .absent(5)
                                .late(2)
                                .notMarked(0)
                                .percentage(new BigDecimal("95.2"))
                                .attendanceMarkedToday(true)
                                .build())
                        .alerts(TeacherDashboardSummaryResponseDto.Alerts.builder()
                                .atRiskStudentCount(3)
                                .pendingLeaveRequests(1)
                                .belowThresholdCount(5)
                                .build())
                        .nextClass(TeacherDashboardSummaryResponseDto.NextClass.builder()
                                .subject("Mathematics")
                                .className("Class 10")
                                .sectionName("A")
                                .room("Room 201")
                                .build())
                        .build());

        mockMvc.perform(get("/api/v1/teacher/dashboard-summary").param("date", "2026-04-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendance.present").value(138))
                .andExpect(jsonPath("$.attendance.absent").value(5))
                .andExpect(jsonPath("$.attendance.late").value(2))
                .andExpect(jsonPath("$.attendance.notMarked").value(0))
                .andExpect(jsonPath("$.attendance.percentage").value(95.2))
                .andExpect(jsonPath("$.attendance.attendanceMarkedToday").value(true))
                .andExpect(jsonPath("$.attendance.id").doesNotExist())
                .andExpect(jsonPath("$.attendance.student").doesNotExist())
                .andExpect(jsonPath("$.attendance.recordedBy").doesNotExist());
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void dashboardSummaryForbiddenForStudentRole() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/dashboard-summary"))
                .andExpect(status().isForbidden());
    }
}



