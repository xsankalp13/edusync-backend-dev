package com.project.edusync.iam.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.iam.model.dto.rbac.PermissionResponseDTO;
import com.project.edusync.iam.model.dto.rbac.RolePermissionLinkResponseDTO;
import com.project.edusync.iam.model.dto.rbac.RoleSummaryDTO;
import com.project.edusync.iam.model.entity.Permission;
import com.project.edusync.iam.model.entity.Role;
import com.project.edusync.iam.repository.PermissionRepository;
import com.project.edusync.iam.repository.RoleRepository;
import com.project.edusync.iam.service.RbacManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RbacManagementServiceImpl implements RbacManagementService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public PermissionResponseDTO createPermission(String permissionName) {
        String normalizedName = normalizePermissionName(permissionName);
        log.info("RBAC create permission requested for '{}'", normalizedName);

        if (permissionRepository.existsByName(normalizedName)) {
            throw new EdusyncException("Permission already exists: " + normalizedName, HttpStatus.CONFLICT);
        }

        Permission permission = new Permission();
        permission.setName(normalizedName);
        permission.setActive(true);

        Permission saved = permissionRepository.save(permission);
        log.info("RBAC permission created id={} name={}", saved.getId(), saved.getName());
        return toPermissionResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponseDTO> listPermissions(String query) {
        log.info("RBAC list permissions requested with query='{}'", query);
        List<Permission> permissions = StringUtils.hasText(query)
                ? permissionRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query.trim())
                : permissionRepository.findAllByOrderByNameAsc();

        return permissions.stream().map(this::toPermissionResponse).toList();
    }

    @Override
    @Transactional
    public RolePermissionLinkResponseDTO assignPermissionToRole(Integer roleId, Integer permissionId) {
        Role role = findRoleById(roleId);
        Permission permission = findPermissionById(permissionId);

        Set<Permission> permissions = role.getPermissions() == null ? new HashSet<>() : role.getPermissions();
        boolean added = permissions.add(permission);
        role.setPermissions(permissions);
        roleRepository.save(role);

        String message = added
                ? "Permission linked to role successfully"
                : "Permission is already linked to role";

        log.info("RBAC assign permission result roleId={} permissionId={} added={}", roleId, permissionId, added);
        return new RolePermissionLinkResponseDTO(role.getId().intValue(), role.getName(), permission.getId(), permission.getName(), message);
    }

    @Override
    @Transactional
    public RolePermissionLinkResponseDTO revokePermissionFromRole(Integer roleId, Integer permissionId) {
        Role role = findRoleById(roleId);
        Permission permission = findPermissionById(permissionId);

        Set<Permission> permissions = role.getPermissions() == null ? new HashSet<>() : role.getPermissions();
        boolean removed = permissions.remove(permission);
        role.setPermissions(permissions);
        roleRepository.save(role);

        String message = removed
                ? "Permission revoked from role successfully"
                : "Permission was not linked to role";

        log.info("RBAC revoke permission result roleId={} permissionId={} removed={}", roleId, permissionId, removed);
        return new RolePermissionLinkResponseDTO(role.getId().intValue(), role.getName(), permission.getId(), permission.getName(), message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponseDTO> listPermissionsByRole(Integer roleId) {
        Role role = findRoleById(roleId);
        log.info("RBAC list role permissions requested roleId={} roleName={}", roleId, role.getName());

        return role.getPermissions().stream()
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .map(this::toPermissionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleSummaryDTO> listRoles() {
        return roleRepository.findAll().stream()
                .sorted((left, right) -> left.getId().compareTo(right.getId()))
                .map(role -> new RoleSummaryDTO(role.getId().intValue(), normalizeRoleName(role.getName())))
                .toList();
    }

    private Role findRoleById(Integer roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
    }

    private Permission findPermissionById(Integer permissionId) {
        return permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", permissionId));
    }

    private String normalizePermissionName(String permissionName) {
        if (permissionName == null) {
            return null;
        }
        return permissionName.trim().toLowerCase();
    }

    private PermissionResponseDTO toPermissionResponse(Permission permission) {
        return new PermissionResponseDTO(permission.getId(), permission.getName(), permission.isActive());
    }

    private String normalizeRoleName(String roleName) {
        if (roleName == null) {
            return null;
        }
        return roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;
    }
}

