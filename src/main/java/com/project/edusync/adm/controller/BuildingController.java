package com.project.edusync.adm.controller;

import com.project.edusync.adm.model.dto.request.BuildingRequestDto;
import com.project.edusync.adm.model.dto.response.BuildingResponseDto;
import com.project.edusync.adm.service.BuildingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/buildings")
@RequiredArgsConstructor
@Tag(name = "Building Management", description = "Endpoints for managing campus building master data")
public class BuildingController {

    private final BuildingService buildingService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SCHOOL_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(
            summary = "Create building",
            description = "Creates a new building (e.g., Block A, Main Building).",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Building created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SCHOOL_ADMIN or SUPER_ADMIN"),
            @ApiResponse(responseCode = "409", description = "Building name already exists")
    })
    public ResponseEntity<BuildingResponseDto> createBuilding(@Valid @RequestBody BuildingRequestDto requestDto) {
        return new ResponseEntity<>(buildingService.createBuilding(requestDto), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SCHOOL_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(
            summary = "List buildings",
            description = "Returns all configured buildings.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Buildings fetched successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SCHOOL_ADMIN or SUPER_ADMIN")
    })
    public ResponseEntity<List<BuildingResponseDto>> getAllBuildings() {
        return ResponseEntity.ok(buildingService.getAllBuildings());
    }

    @PutMapping("/{buildingId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SCHOOL_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(
            summary = "Update building",
            description = "Updates building details by UUID.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Building updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SCHOOL_ADMIN or SUPER_ADMIN"),
            @ApiResponse(responseCode = "404", description = "Building not found"),
            @ApiResponse(responseCode = "409", description = "Building name already exists")
    })
    public ResponseEntity<BuildingResponseDto> updateBuilding(
            @Parameter(description = "Building UUID", required = true)
            @PathVariable UUID buildingId,
            @Valid @RequestBody BuildingRequestDto requestDto) {
        return ResponseEntity.ok(buildingService.updateBuilding(buildingId, requestDto));
    }

    @DeleteMapping("/{buildingId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SCHOOL_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(
            summary = "Delete building",
            description = "Deletes a building. Fails when rooms are still linked to this building.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Building deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SCHOOL_ADMIN or SUPER_ADMIN"),
            @ApiResponse(responseCode = "404", description = "Building not found"),
            @ApiResponse(responseCode = "400", description = "Building cannot be deleted because rooms reference it")
    })
    public ResponseEntity<Void> deleteBuilding(@Parameter(description = "Building UUID", required = true) @PathVariable UUID buildingId) {
        buildingService.deleteBuilding(buildingId);
        return ResponseEntity.noContent().build();
    }
}


