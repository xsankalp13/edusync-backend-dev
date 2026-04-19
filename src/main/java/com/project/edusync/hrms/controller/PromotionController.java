package com.project.edusync.hrms.controller;

import org.springframework.data.domain.Page;
import com.project.edusync.hrms.dto.promotion.PromotionCreateDTO;
import com.project.edusync.hrms.dto.promotion.PromotionResponseDTO;
import com.project.edusync.hrms.dto.promotion.PromotionReviewDTO;
import com.project.edusync.hrms.model.enums.PromotionStatus;
import com.project.edusync.hrms.service.PromotionService;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/hrms/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;
    private final UserManagementService userManagementService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Page<PromotionResponseDTO>> list(
            @RequestParam(required = false) PromotionStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(promotionService.list(status, pageable));
    }

    @GetMapping("/{ref}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<PromotionResponseDTO> getByIdentifier(@PathVariable String ref) {
        return ResponseEntity.ok(promotionService.getByIdentifier(ref));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<PromotionResponseDTO> initiate(
            @Valid @RequestBody PromotionCreateDTO dto,
            Authentication authentication
    ) {
        User user = validateAndGetAuthenticatedUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(promotionService.initiate(dto, user.getId()));
    }

    @PostMapping("/{ref}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<PromotionResponseDTO> approve(
            @PathVariable String ref,
            @Valid @RequestBody(required = false) PromotionReviewDTO dto,
            Authentication authentication
    ) {
        User user = validateAndGetAuthenticatedUser(authentication);
        PromotionReviewDTO safeDto = dto != null ? dto : new PromotionReviewDTO(null);
        return ResponseEntity.ok(promotionService.approve(ref, user.getId(), safeDto));
    }

    @PostMapping("/{ref}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<PromotionResponseDTO> reject(
            @PathVariable String ref,
            @Valid @RequestBody(required = false) PromotionReviewDTO dto,
            Authentication authentication
    ) {
        User user = validateAndGetAuthenticatedUser(authentication);
        PromotionReviewDTO safeDto = dto != null ? dto : new PromotionReviewDTO(null);
        return ResponseEntity.ok(promotionService.reject(ref, user.getId(), safeDto));
    }

    @GetMapping("/staff/{staffRef}/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'PRINCIPAL')")
    public ResponseEntity<List<PromotionResponseDTO>> getStaffHistory(@PathVariable String staffRef) {
        return ResponseEntity.ok(promotionService.getStaffHistory(staffRef));
    }

    private User validateAndGetAuthenticatedUser(Authentication authentication) {
        return userManagementService.findByUsername(authentication.getName());
    }
}
