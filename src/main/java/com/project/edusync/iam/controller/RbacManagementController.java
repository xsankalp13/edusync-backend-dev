package com.project.edusync.iam.controller;

import com.project.edusync.iam.model.dto.rbac.CreatePermissionRequestDTO;
import com.project.edusync.iam.model.dto.rbac.PermissionResponseDTO;
import com.project.edusync.iam.model.dto.rbac.RolePermissionLinkResponseDTO;
import com.project.edusync.iam.model.dto.rbac.RoleSummaryDTO;
import com.project.edusync.iam.service.RbacManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/iam/rbac")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RBAC Management", description = "ROLE_SCHOOL_ADMIN APIs to manage permissions and role-permission mappings")
public class RbacManagementController {

    private final RbacManagementService rbacManagementService;

    @PostMapping("/permissions")
    @PreAuthorize("hasAuthority('rbac:permission:create')")
    @Operation(
            summary = "Create permission",
            description = "Creates a new permission in domain:action:scope format.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Permission created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Missing required permission"),
            @ApiResponse(responseCode = "409", description = "Permission already exists")
    })
    public ResponseEntity<PermissionResponseDTO> createPermission(@Valid @RequestBody CreatePermissionRequestDTO request) {
        log.info("RBAC API: create permission requested name={}", request.name());
        PermissionResponseDTO response = rbacManagementService.createPermission(request.name());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('rbac:permission:read')")
    @Operation(
            summary = "List permissions",
            description = "Returns all permissions or filters by a case-insensitive query.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Permissions fetched"),
            @ApiResponse(responseCode = "403", description = "Missing required permission")
    })
    public ResponseEntity<List<PermissionResponseDTO>> listPermissions(
            @RequestParam(value = "query", required = false) String query) {

        log.info("RBAC API: list permissions requested query={}", query);
        return ResponseEntity.ok(rbacManagementService.listPermissions(query));
    }

    @PostMapping("/roles/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('rbac:role-permission:assign')")
    @Operation(
            summary = "Assign permission to role",
            description = "Idempotently links a permission to a role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Permission assignment result returned"),
            @ApiResponse(responseCode = "403", description = "Missing required permission"),
            @ApiResponse(responseCode = "404", description = "Role or permission not found")
    })
    public ResponseEntity<RolePermissionLinkResponseDTO> assignPermissionToRole(
            @PathVariable Integer roleId,
            @PathVariable Integer permissionId) {

        log.info("RBAC API: assign permission requested roleId={} permissionId={}", roleId, permissionId);
        return ResponseEntity.ok(rbacManagementService.assignPermissionToRole(roleId, permissionId));
    }

    @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('rbac:role-permission:revoke')")
    @Operation(
            summary = "Revoke permission from role",
            description = "Idempotently removes a permission from a role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Permission revoke result returned"),
            @ApiResponse(responseCode = "403", description = "Missing required permission"),
            @ApiResponse(responseCode = "404", description = "Role or permission not found")
    })
    public ResponseEntity<RolePermissionLinkResponseDTO> revokePermissionFromRole(
            @PathVariable Integer roleId,
            @PathVariable Integer permissionId) {

        log.info("RBAC API: revoke permission requested roleId={} permissionId={}", roleId, permissionId);
        return ResponseEntity.ok(rbacManagementService.revokePermissionFromRole(roleId, permissionId));
    }

    @GetMapping("/roles/{roleId}/permissions")
    @PreAuthorize("hasAuthority('rbac:role-permission:read')")
    @Operation(
            summary = "List role permissions",
            description = "Returns all permissions linked to the specified role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role permissions fetched"),
            @ApiResponse(responseCode = "403", description = "Missing required permission"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    public ResponseEntity<List<PermissionResponseDTO>> listPermissionsByRole(@PathVariable Integer roleId) {
        log.info("RBAC API: list role permissions requested roleId={}", roleId);
        return ResponseEntity.ok(rbacManagementService.listPermissionsByRole(roleId));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "List roles",
            description = "Returns all roles with numeric IDs for RBAC management UI.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Roles fetched"),
            @ApiResponse(responseCode = "403", description = "Only SUPER_ADMIN can access")
    })
    public ResponseEntity<List<RoleSummaryDTO>> listRoles() {
        return ResponseEntity.ok(rbacManagementService.listRoles());
    }
}

