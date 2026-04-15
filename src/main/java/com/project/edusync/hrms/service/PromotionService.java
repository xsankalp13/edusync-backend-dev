package com.project.edusync.hrms.service;

import com.project.edusync.hrms.dto.promotion.PromotionCreateDTO;
import com.project.edusync.hrms.dto.promotion.PromotionResponseDTO;
import com.project.edusync.hrms.dto.promotion.PromotionReviewDTO;
import com.project.edusync.hrms.model.enums.PromotionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PromotionService {
    Page<PromotionResponseDTO> list(PromotionStatus status, Pageable pageable);
    PromotionResponseDTO getByIdentifier(String identifier);
    PromotionResponseDTO initiate(PromotionCreateDTO dto, Long initiatedByUserId);
    PromotionResponseDTO approve(String identifier, Long approverUserId, PromotionReviewDTO dto);
    PromotionResponseDTO reject(String identifier, Long approverUserId, PromotionReviewDTO dto);
    List<PromotionResponseDTO> getStaffHistory(String staffRef);
}
