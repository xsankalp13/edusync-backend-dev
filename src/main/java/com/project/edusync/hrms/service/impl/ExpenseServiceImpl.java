package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.document.DocumentUploadInitResponseDTO;
import com.project.edusync.hrms.dto.expense.ExpenseDTOs.*;
import com.project.edusync.hrms.model.entity.*;
import com.project.edusync.hrms.model.enums.ExpenseStatus;
import com.project.edusync.hrms.repository.*;
import com.project.edusync.uis.config.MediaUploadProperties;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service("expenseService")
@RequiredArgsConstructor
public class ExpenseServiceImpl {

    private final ExpenseClaimRepository claimRepo;
    private final ExpenseClaimItemRepository itemRepo;
    private final StaffRepository staffRepository;
    private final MediaUploadProperties mediaUploadProperties;

    @Transactional
    public ClaimResponseDTO createClaim(ClaimCreateDTO dto) {
        Staff staff = resolveStaff(dto.staffRef());
        ExpenseClaim claim = new ExpenseClaim();
        claim.setStaff(staff);
        claim.setTitle(dto.title());
        claim.setDescription(dto.description());
        claim.setStatus(ExpenseStatus.DRAFT);
        return toResponse(claimRepo.save(claim), List.of());
    }

    @Transactional(readOnly = true)
    public List<ClaimResponseDTO> listClaims(String staffRef, ExpenseStatus status) {
        List<ExpenseClaim> claims;
        if (staffRef != null) {
            Staff staff = resolveStaff(staffRef);
            claims = status != null ? claimRepo.findByStaff_IdAndStatus(staff.getId(), status)
                    : claimRepo.findByStaff_Id(staff.getId());
        } else {
            claims = status != null ? claimRepo.findByStatus(status) : claimRepo.findAll();
        }
        return claims.stream().map(c -> toResponse(c, itemRepo.findByClaim_IdAndActiveTrue(c.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public ClaimResponseDTO getClaim(UUID uuid) {
        ExpenseClaim c = findClaim(uuid);
        return toResponse(c, itemRepo.findByClaim_IdAndActiveTrue(c.getId()));
    }

    @Transactional
    public ClaimResponseDTO updateStatus(UUID uuid, ClaimStatusUpdateDTO dto) {
        ExpenseClaim claim = findClaim(uuid);
        if (dto.status() == ExpenseStatus.SUBMITTED && claim.getStatus() != ExpenseStatus.DRAFT)
            throw new EdusyncException("Only DRAFT claims can be submitted", HttpStatus.BAD_REQUEST);
        claim.setStatus(dto.status());
        if (dto.status() == ExpenseStatus.SUBMITTED) claim.setSubmittedAt(LocalDateTime.now());
        return toResponse(claimRepo.save(claim), itemRepo.findByClaim_IdAndActiveTrue(claim.getId()));
    }

    @Transactional
    public ClaimItemResponseDTO addItem(UUID claimUuid, ClaimItemCreateDTO dto) {
        ExpenseClaim claim = findClaim(claimUuid);
        if (claim.getStatus() != ExpenseStatus.DRAFT)
            throw new EdusyncException("Items can only be added to DRAFT claims", HttpStatus.BAD_REQUEST);
        ExpenseClaimItem item = new ExpenseClaimItem();
        item.setClaim(claim);
        item.setCategory(dto.category());
        item.setDescription(dto.description());
        item.setAmount(dto.amount());
        item.setReceiptUrl(dto.receiptUrl());
        item.setExpenseDate(dto.expenseDate());
        ExpenseClaimItem saved = itemRepo.save(item);
        recalcTotal(claim);
        return toItemResponse(saved);
    }

    @Transactional
    public ClaimItemResponseDTO updateItem(UUID claimUuid, Long itemId, ClaimItemUpdateDTO dto) {
        ExpenseClaim claim = findClaim(claimUuid);
        ExpenseClaimItem item = itemRepo.findByIdAndClaim_Id(itemId, claim.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + itemId));
        if (dto.category() != null) item.setCategory(dto.category());
        if (dto.description() != null) item.setDescription(dto.description());
        if (dto.amount() != null) item.setAmount(dto.amount());
        if (dto.receiptUrl() != null) item.setReceiptUrl(dto.receiptUrl());
        if (dto.expenseDate() != null) item.setExpenseDate(dto.expenseDate());
        ExpenseClaimItem saved = itemRepo.save(item);
        recalcTotal(claim);
        return toItemResponse(saved);
    }

    @Transactional
    public void deleteItem(UUID claimUuid, Long itemId) {
        ExpenseClaim claim = findClaim(claimUuid);
        ExpenseClaimItem item = itemRepo.findByIdAndClaim_Id(itemId, claim.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + itemId));
        item.setActive(false);
        itemRepo.save(item);
        recalcTotal(claim);
    }

    private ExpenseClaim findClaim(UUID uuid) {
        return claimRepo.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + uuid));
    }

    public DocumentUploadInitResponseDTO initiateReceiptUpload(ReceiptUploadInitRequestDTO dto) {
        String objectKey = "expense-receipts/" + UUID.randomUUID() + "_" + dto.fileName();
        Instant expiresAt = Instant.now().plusSeconds(mediaUploadProperties.getUploadInitTtlSeconds());
        String provider = normalizeProvider(mediaUploadProperties.getProvider());

        if ("cloudinary".equals(provider)) {
            MediaUploadProperties.Cloudinary cfg = mediaUploadProperties.getCloudinary();
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String folder = "expense-receipts";
            String signatureBase = "folder=" + folder + "&public_id=" + objectKey + "&timestamp=" + timestamp;
            String signature = sha1Hex(signatureBase + cfg.getApiSecret());
            Map<String, String> fields = new HashMap<>();
            fields.put("api_key", cfg.getApiKey());
            fields.put("timestamp", timestamp);
            fields.put("signature", signature);
            fields.put("folder", folder);
            fields.put("public_id", objectKey);
            return DocumentUploadInitResponseDTO.builder()
                    .provider("cloudinary").method("POST")
                    .uploadUrl("https://api.cloudinary.com/v1_1/" + cfg.getCloudName() + "/raw/upload")
                    .objectKey(objectKey).expiresAt(expiresAt).fields(fields).headers(Map.of()).build();
        }

        if ("s3".equals(provider)) {
            String template = mediaUploadProperties.getS3().getUploadUrlTemplate();
            if (!StringUtils.hasText(template))
                throw new EdusyncException("S3 upload URL template not configured.", HttpStatus.INTERNAL_SERVER_ERROR);
            return DocumentUploadInitResponseDTO.builder()
                    .provider("s3").method("PUT")
                    .uploadUrl(template.replace("{objectKey}", objectKey))
                    .objectKey(objectKey).expiresAt(expiresAt).fields(Map.of())
                    .headers(Map.of("Content-Type", dto.contentType())).build();
        }
        throw new EdusyncException("Unsupported media provider: " + provider, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String normalizeProvider(String provider) {
        return provider == null || provider.isBlank() ? "cloudinary" : provider.trim().toLowerCase();
    }

    private String sha1Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new EdusyncException("SHA-1 not available", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void recalcTotal(ExpenseClaim claim) {
        BigDecimal total = itemRepo.findByClaim_IdAndActiveTrue(claim.getId())
                .stream().map(ExpenseClaimItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        claim.setTotalAmount(total);
        claimRepo.save(claim);
    }

    private Staff resolveStaff(String ref) {
        return PublicIdentifierResolver.resolve(ref, staffRepository::findByUuid, staffRepository::findById, "Staff");
    }

    private String name(Staff s) {
        return s.getUserProfile() != null ? (s.getUserProfile().getFirstName() + " " + s.getUserProfile().getLastName()).trim() : "";
    }

    private ClaimItemResponseDTO toItemResponse(ExpenseClaimItem i) {
        return new ClaimItemResponseDTO(i.getId(), i.getUuid(), i.getCategory(), i.getDescription(),
                i.getAmount(), i.getReceiptUrl(), i.getExpenseDate());
    }

    private ClaimResponseDTO toResponse(ExpenseClaim c, List<ExpenseClaimItem> items) {
        return new ClaimResponseDTO(c.getUuid(), c.getStaff().getUuid(), name(c.getStaff()),
                c.getTitle(), c.getDescription(), c.getTotalAmount(), c.getStatus(),
                c.getSubmittedAt(), c.getCreatedAt(),
                items.stream().map(this::toItemResponse).toList());
    }
}

