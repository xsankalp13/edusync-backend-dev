package com.project.edusync.hrms.service.impl;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.project.edusync.hrms.dto.bank.BankDetailsBulkImportResultDTO;
import com.project.edusync.hrms.dto.bank.BankDetailsUpdateDTO;
import com.project.edusync.hrms.dto.bank.StaffBankStatusDTO;
import com.project.edusync.hrms.service.StaffBankDetailsService;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.StaffSensitiveInfo;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StaffSensitiveInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffBankDetailsServiceImpl implements StaffBankDetailsService {

    private static final List<String> CSV_HEADER = Arrays.asList(
            "employeeId", "staffName", "accountHolderName", "accountNumber", "ifscCode", "bankName", "accountType"
    );
    private static final String IFSC_PATTERN = "[A-Z]{4}0[A-Z0-9]{6}";

    private final StaffRepository staffRepository;
    private final StaffSensitiveInfoRepository sensitiveInfoRepository;

    // ── helpers ─────────────────────────────────────────────────────────────

    private Staff resolveStaff(String ref) {
        try {
            long id = Long.parseLong(ref);
            return staffRepository.findById(id)
                    .filter(Staff::isActive)
                    .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + ref));
        } catch (NumberFormatException e) {
            return staffRepository.findByUuid(java.util.UUID.fromString(ref))
                    .filter(Staff::isActive)
                    .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + ref));
        }
    }

    private StaffSensitiveInfo getOrCreateInfo(Staff staff) {
        return sensitiveInfoRepository.findByStaff_Id(staff.getId())
                .orElseGet(() -> {
                    StaffSensitiveInfo info = new StaffSensitiveInfo();
                    info.setStaff(staff);
                    return info;
                });
    }

    private String maskAccount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        if (raw.length() <= 4) return "****";
        return "****" + raw.substring(raw.length() - 4);
    }

    private String staffFullName(Staff s) {
        String first = s.getUserProfile() != null && s.getUserProfile().getFirstName() != null
                ? s.getUserProfile().getFirstName() : "";
        String last = s.getUserProfile() != null && s.getUserProfile().getLastName() != null
                ? s.getUserProfile().getLastName() : "";
        return (first + " " + last).trim();
    }

    private String designationName(Staff s) {
        return s.getDesignation() != null ? s.getDesignation().getDesignationName() : null;
    }

    private StaffBankStatusDTO toDTO(Staff staff) {
        StaffSensitiveInfo info = sensitiveInfoRepository.findByStaff_Id(staff.getId()).orElse(null);
        boolean hasBankDetails = info != null
                && info.getBankAccountNumber() != null && !info.getBankAccountNumber().isBlank();
        boolean hasIfsc = info != null
                && info.getBankIfscCode() != null && !info.getBankIfscCode().isBlank();
        return new StaffBankStatusDTO(
                staff.getId(),
                staff.getUuid() != null ? staff.getUuid().toString() : null,
                staff.getEmployeeId(),
                staffFullName(staff),
                designationName(staff),
                hasBankDetails,
                hasIfsc,
                info != null ? info.getBankName() : null,
                info != null ? info.getBankIfscCode() : null,
                hasBankDetails ? maskAccount(info.getBankAccountNumber()) : null,
                info != null ? info.getBankAccountType() : null,
                info != null ? info.getAccountHolderName() : null
        );
    }

    // ── interface implementations ────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<StaffBankStatusDTO> listAll(Pageable pageable) {
        return staffRepository.findByIsActiveTrue(pageable).map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffBankStatusDTO> listMissing() {
        return staffRepository.findByIsActiveTrue()
                .stream()
                .map(this::toDTO)
                .filter(dto -> !dto.hasBankDetails() || !dto.hasIfsc())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCsvTemplate() {
        List<Staff> allStaff = staffRepository.findByIsActiveTrue();
        StringBuilder sb = new StringBuilder();
        sb.append("employeeId,staffName,accountHolderName,accountNumber,ifscCode,bankName,accountType\n");
        for (Staff s : allStaff) {
            String empId = s.getEmployeeId() != null ? s.getEmployeeId() : "";
            String name = staffFullName(s).replace(",", " ");
            sb.append(empId).append(",")
              .append(name).append(",")
              .append(",,,,\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional
    public BankDetailsBulkImportResultDTO bulkImport(MultipartFile file) throws IOException {
        List<BankDetailsBulkImportResultDTO.RowError> errors = new ArrayList<>();
        int successCount = 0;
        int skippedCount = 0;
        int rowNumber = 1;

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {

            String[] header = csvReader.readNext();
            if (header == null) {
                throw new EdusyncException("CSV file is empty", HttpStatus.BAD_REQUEST);
            }
            List<String> actualHeader = Arrays.asList(header);
            if (!actualHeader.equals(CSV_HEADER)) {
                throw new EdusyncException(
                        "Invalid CSV header. Expected: " + CSV_HEADER + ", Found: " + actualHeader,
                        HttpStatus.BAD_REQUEST);
            }

            String[] row;
            while ((row = csvReader.readNext()) != null) {
                rowNumber++;
                String employeeId = row.length > 0 ? row[0].trim() : "";

                // Skip completely blank data rows
                boolean allBlank = true;
                for (int i = 2; i < row.length; i++) {
                    if (row[i] != null && !row[i].isBlank()) {
                        allBlank = false;
                        break;
                    }
                }
                if (allBlank) {
                    skippedCount++;
                    continue;
                }

                try {
                    String accountHolderName = row.length > 2 ? row[2].trim() : "";
                    String accountNumber = row.length > 3 ? row[3].trim() : "";
                    String ifscCode = row.length > 4 ? row[4].trim().toUpperCase() : "";
                    String bankName = row.length > 5 ? row[5].trim() : "";
                    String accountType = row.length > 6 ? row[6].trim().toUpperCase() : "SAVINGS";

                    if (accountNumber.isBlank()) {
                        throw new IllegalArgumentException("accountNumber is required");
                    }
                    if (!ifscCode.isBlank() && !ifscCode.matches(IFSC_PATTERN)) {
                        throw new IllegalArgumentException("Invalid IFSC code: " + ifscCode);
                    }
                    if (!accountType.isBlank() && !accountType.equals("SAVINGS") && !accountType.equals("CURRENT")) {
                        accountType = "SAVINGS"; // default gracefully
                    }

                    Staff staff = staffRepository.findByEmployeeId(employeeId)
                            .orElseThrow(() -> new ResourceNotFoundException("Staff not found with employeeId: " + employeeId));

                    StaffSensitiveInfo info = getOrCreateInfo(staff);
                    info.setAccountHolderName(accountHolderName.isBlank() ? null : accountHolderName);
                    info.setBankAccountNumber(accountNumber);
                    info.setBankIfscCode(ifscCode.isBlank() ? null : ifscCode);
                    info.setBankName(bankName.isBlank() ? null : bankName);
                    info.setBankAccountType(accountType.isBlank() ? "SAVINGS" : accountType);
                    sensitiveInfoRepository.save(info);
                    successCount++;
                    log.info("Bank details updated for employeeId={}", employeeId);

                } catch (Exception e) {
                    errors.add(new BankDetailsBulkImportResultDTO.RowError(rowNumber - 1, employeeId, e.getMessage()));
                    log.warn("Row {} failed for employeeId={}: {}", rowNumber, employeeId, e.getMessage());
                }
            }
        } catch (CsvValidationException e) {
            throw new EdusyncException("Failed to parse CSV: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        int totalRows = rowNumber - 1;
        return new BankDetailsBulkImportResultDTO(totalRows, successCount, skippedCount, errors.size(), errors);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffBankStatusDTO getByStaffRef(String staffRef) {
        return toDTO(resolveStaff(staffRef));
    }

    @Override
    @Transactional
    public StaffBankStatusDTO upsert(String staffRef, BankDetailsUpdateDTO dto) {
        Staff staff = resolveStaff(staffRef);
        StaffSensitiveInfo info = getOrCreateInfo(staff);
        info.setAccountHolderName(dto.accountHolderName());
        info.setBankAccountNumber(dto.accountNumber());
        info.setBankIfscCode(dto.ifscCode());
        info.setBankName(dto.bankName());
        info.setBankAccountType(dto.accountType() != null ? dto.accountType() : "SAVINGS");
        sensitiveInfoRepository.save(info);
        return toDTO(staff);
    }

    @Override
    @Transactional
    public void clearBankDetails(String staffRef) {
        Staff staff = resolveStaff(staffRef);
        StaffSensitiveInfo info = getOrCreateInfo(staff);
        info.setAccountHolderName(null);
        info.setBankAccountNumber(null);
        info.setBankIfscCode(null);
        info.setBankName(null);
        info.setBankAccountType(null);
        sensitiveInfoRepository.save(info);
    }
}
