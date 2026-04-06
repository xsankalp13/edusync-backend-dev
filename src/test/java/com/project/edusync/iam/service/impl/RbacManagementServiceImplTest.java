package com.project.edusync.iam.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.iam.model.dto.rbac.PermissionResponseDTO;
import com.project.edusync.iam.model.dto.rbac.RolePermissionLinkResponseDTO;
import com.project.edusync.iam.model.dto.rbac.RoleSummaryDTO;
import com.project.edusync.iam.model.entity.Permission;
import com.project.edusync.iam.model.entity.Role;
import com.project.edusync.iam.repository.PermissionRepository;
import com.project.edusync.iam.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RbacManagementServiceImplTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RbacManagementServiceImpl rbacManagementService;

    private Role role;
    private Permission permission;

    @BeforeEach
    void setUp() {
        role = new Role();
        role.setId(10L);
        role.setName("ROLE_SCHOOL_ADMIN");
        role.setPermissions(new HashSet<>());

        permission = new Permission();
        permission.setId(20);
        permission.setName("rbac:permission:create");
        permission.setActive(true);
    }

    @Test
    void createPermissionThrowsConflictWhenPermissionAlreadyExists() {
        when(permissionRepository.existsByName("rbac:permission:create")).thenReturn(true);

        assertThrows(EdusyncException.class,
                () -> rbacManagementService.createPermission("rbac:permission:create"));
    }

    @Test
    void assignPermissionToRoleIsIdempotent() {
        when(roleRepository.findById(10)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(20)).thenReturn(Optional.of(permission));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RolePermissionLinkResponseDTO first = rbacManagementService.assignPermissionToRole(10, 20);
        RolePermissionLinkResponseDTO second = rbacManagementService.assignPermissionToRole(10, 20);

        assertEquals("Permission linked to role successfully", first.message());
        assertEquals("Permission is already linked to role", second.message());
        assertEquals(1, role.getPermissions().size());
    }

    @Test
    void listPermissionsByRoleReturnsMappedPermissions() {
        role.setPermissions(Set.of(permission));
        when(roleRepository.findById(10)).thenReturn(Optional.of(role));

        PermissionResponseDTO dto = rbacManagementService.listPermissionsByRole(10).getFirst();

        assertEquals(permission.getId(), dto.id());
        assertEquals(permission.getName(), dto.name());
    }

    @Test
    void listRolesNormalizesRolePrefixAndOrdersById() {
        Role superAdmin = new Role();
        superAdmin.setId(1L);
        superAdmin.setName("ROLE_SUPER_ADMIN");

        Role teacher = new Role();
        teacher.setId(3L);
        teacher.setName("ROLE_TEACHER");

        when(roleRepository.findAll()).thenReturn(java.util.List.of(teacher, superAdmin));

        java.util.List<RoleSummaryDTO> roles = rbacManagementService.listRoles();

        assertEquals(2, roles.size());
        assertEquals(1, roles.get(0).id());
        assertEquals("SUPER_ADMIN", roles.get(0).name());
        assertEquals(3, roles.get(1).id());
        assertEquals("TEACHER", roles.get(1).name());
    }
}

