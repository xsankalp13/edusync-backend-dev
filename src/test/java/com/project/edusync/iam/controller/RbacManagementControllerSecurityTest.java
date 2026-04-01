package com.project.edusync.iam.controller;

import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.common.security.CustomUserDetailService;
import com.project.edusync.iam.model.dto.rbac.PermissionResponseDTO;
import com.project.edusync.iam.model.dto.rbac.RoleSummaryDTO;
import com.project.edusync.iam.service.RbacManagementService;
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
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RbacManagementController.class, properties = "api.url=/api/v1")
@AutoConfigureMockMvc
@Import(RbacManagementControllerSecurityTest.TestSecurityConfig.class)
class RbacManagementControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RbacManagementService rbacManagementService;

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
                    User.withUsername("school-admin")
                            .password("{noop}password")
                            .authorities("rbac:permission:read")
                            .build(),
                    User.withUsername("basic-user")
                            .password("{noop}password")
                            .authorities("profile:read:own")
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
    @WithMockUser(authorities = {"rbac:permission:read"})
    void listPermissionsAllowedWhenPermissionPresent() throws Exception {
        when(rbacManagementService.listPermissions(any())).thenReturn(List.of(new PermissionResponseDTO(1, "rbac:permission:read", true)));

        mockMvc.perform(get("/api/v1/auth/iam/rbac/permissions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"profile:read:own"})
    void listPermissionsForbiddenWhenPermissionMissing() throws Exception {
        mockMvc.perform(get("/api/v1/auth/iam/rbac/permissions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"SUPER_ADMIN"})
    void listRolesAllowedForSuperAdmin() throws Exception {
        when(rbacManagementService.listRoles()).thenReturn(List.of(new RoleSummaryDTO(1, "SUPER_ADMIN")));

        mockMvc.perform(get("/api/v1/auth/iam/rbac/roles"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"rbac:permission:read"})
    void listRolesForbiddenForNonSuperAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/auth/iam/rbac/roles"))
                .andExpect(status().isForbidden());
    }
}
