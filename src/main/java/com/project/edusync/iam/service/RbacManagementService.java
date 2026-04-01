package com.project.edusync.iam.service;

import com.project.edusync.iam.model.dto.rbac.PermissionResponseDTO;
import com.project.edusync.iam.model.dto.rbac.RolePermissionLinkResponseDTO;
import com.project.edusync.iam.model.dto.rbac.RoleSummaryDTO;

import java.util.List;

public interface RbacManagementService {

    PermissionResponseDTO createPermission(String permissionName);

    List<PermissionResponseDTO> listPermissions(String query);

    RolePermissionLinkResponseDTO assignPermissionToRole(Integer roleId, Integer permissionId);

    RolePermissionLinkResponseDTO revokePermissionFromRole(Integer roleId, Integer permissionId);

    List<PermissionResponseDTO> listPermissionsByRole(Integer roleId);

    List<RoleSummaryDTO> listRoles();
}

