package com.project.edusync.finance.service.implementation;

import com.project.edusync.finance.dto.misc.MiscellaneousReceiptRequestDTO;
import com.project.edusync.finance.dto.misc.MiscellaneousReceiptResponseDTO;
import com.project.edusync.finance.model.entity.*;
import com.project.edusync.finance.model.enums.JournalReferenceType;
import com.project.edusync.finance.repository.AccountRepository;
import com.project.edusync.finance.repository.MiscellaneousReceiptRepository;
import com.project.edusync.finance.service.GeneralLedgerService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MiscellaneousReceiptServiceImpl {

    private final MiscellaneousReceiptRepository receiptRepository;
    private final AccountRepository accountRepository;
    private final GeneralLedgerService glService;
    private final FinanceAuditServiceImpl auditService;

    public MiscellaneousReceiptResponseDTO recordReceipt(MiscellaneousReceiptRequestDTO dto, Long schoolId) {
        Account incomeAccount = accountRepository.findById(dto.incomeAccountId())
            .orElseThrow(() -> new EntityNotFoundException("Income account not found"));
        Account depositAccount = accountRepository.findById(dto.depositAccountId())
            .orElseThrow(() -> new EntityNotFoundException("Deposit account not found"));

        MiscellaneousReceipt receipt = new MiscellaneousReceipt();
        receipt.setReceiptDate(dto.receiptDate());
        receipt.setReceivedFrom(dto.receivedFrom());
        receipt.setDescription(dto.description());
        receipt.setAmount(dto.amount());
        receipt.setPaymentMode(dto.paymentMode());
        receipt.setReferenceNumber(dto.referenceNumber());
        receipt.setIncomeAccount(incomeAccount);
        receipt.setDepositAccount(depositAccount);
        receipt.setSchoolId(schoolId);
        
        // Generate Receipt Number
        String yearPrefix = "REC-" + LocalDate.now().getYear() + "-";
        Long count = receiptRepository.countByPrefix(schoolId, yearPrefix);
        receipt.setReceiptNumber(yearPrefix + String.format("%04d", count + 1));
        
        MiscellaneousReceipt saved = receiptRepository.save(receipt);

        // Generate GL Entry
        // Dr Deposit Account (Asset)
        // Cr Income Account (Revenue)
        JournalLine drLine = new JournalLine();
        drLine.setAccount(depositAccount);
        drLine.setDebitAmount(dto.amount());
        drLine.setCreditAmount(BigDecimal.ZERO);
        drLine.setNarration(dto.description() + " (Deposit)");

        JournalLine crLine = new JournalLine();
        crLine.setAccount(incomeAccount);
        crLine.setDebitAmount(BigDecimal.ZERO);
        crLine.setCreditAmount(dto.amount());
        crLine.setNarration(dto.description() + " (Income)");

        JournalEntry glEntry = glService.createJournalEntry(
            dto.receiptDate(),
            "Misc Receipt: " + saved.getReceiptNumber(),
            JournalReferenceType.MANUAL, // Using manual as fallback for misc
            saved.getId(),
            List.of(drLine, crLine),
            schoolId
        );

        saved.setGlEntryId(glEntry.getId());
        
        auditService.logAction("MISC_RECEIPT_CREATED", "MiscellaneousReceipt", saved.getId(), "Created Misc Receipt " + saved.getReceiptNumber() + " for " + dto.amount(), schoolId);

        return toDTO(receiptRepository.save(saved));
    }

    @Transactional(readOnly = true)
    public List<MiscellaneousReceiptResponseDTO> getAllReceipts(Long schoolId) {
        return receiptRepository.findBySchoolIdOrderByReceiptDateDesc(schoolId)
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MiscellaneousReceiptResponseDTO getReceiptById(Long id, Long schoolId) {
        return receiptRepository.findByIdAndSchoolId(id, schoolId)
            .map(this::toDTO)
            .orElseThrow(() -> new EntityNotFoundException("Miscellaneous receipt not found"));
    }

    private MiscellaneousReceiptResponseDTO toDTO(MiscellaneousReceipt r) {
        String createdBy = null;
        try {
            createdBy = SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {}
        
        return new MiscellaneousReceiptResponseDTO(
            r.getId(), r.getReceiptNumber(), r.getReceiptDate(), r.getReceivedFrom(),
            r.getDescription(), r.getAmount(), r.getPaymentMode(), r.getReferenceNumber(),
            r.getIncomeAccount().getId(), r.getIncomeAccount().getCode(), r.getIncomeAccount().getName(),
            r.getDepositAccount().getId(), r.getDepositAccount().getCode(), r.getDepositAccount().getName(),
            r.getGlEntryId(), r.getCreatedAt(), createdBy
        );
    }
}
