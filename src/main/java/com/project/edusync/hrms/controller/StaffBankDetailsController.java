package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.bank.BankDetailsBulkImportResultDTO;
import com.project.edusync.hrms.dto.bank.BankDetailsUpdateDTO;
import com.project.edusync.hrms.dto.bank.StaffBankStatusDTO;
import com.project.edusync.hrms.service.StaffBankDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/hrms/bank-details")
@RequiredArgsConstructor
@Tag(name = "HRMS Bank Details", description = "Staff bank account management APIs")
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
public class StaffBankDetailsController {

    private final StaffBankDetailsService bankDetailsService;

    @GetMapping
    @Operation(summary = "List all active staff with their bank status")
    public ResponseEntity<Page<StaffBankStatusDTO>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(bankDetailsService.listAll(
                PageRequest.of(page, size, Sort.by("employeeId").ascending())));
    }

    @GetMapping("/missing")
    @Operation(summary = "List salary-mapped staff missing bank/IFSC details")
    public ResponseEntity<List<StaffBankStatusDTO>> listMissing() {
        return ResponseEntity.ok(bankDetailsService.listMissing());
    }

    @GetMapping("/template")
    @Operation(summary = "Download pre-filled CSV template for bulk import")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] csv = bankDetailsService.exportCsvTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bank-details-template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @PostMapping(value = "/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Bulk import bank details from CSV file")
    public ResponseEntity<BankDetailsBulkImportResultDTO> bulkImport(
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(bankDetailsService.bulkImport(file));
    }

    @GetMapping("/{staffRef}")
    @Operation(summary = "Get bank details for a specific staff member")
    public ResponseEntity<StaffBankStatusDTO> getByRef(@PathVariable String staffRef) {
        return ResponseEntity.ok(bankDetailsService.getByStaffRef(staffRef));
    }

    @PutMapping("/{staffRef}")
    @Operation(summary = "Create or update bank details for a staff member")
    public ResponseEntity<StaffBankStatusDTO> upsert(
            @PathVariable String staffRef,
            @Valid @RequestBody BankDetailsUpdateDTO dto) {
        return ResponseEntity.ok(bankDetailsService.upsert(staffRef, dto));
    }

    @DeleteMapping("/{staffRef}/bank")
    @Operation(summary = "Clear all bank details for a staff member")
    public ResponseEntity<Void> clearBankDetails(@PathVariable String staffRef) {
        bankDetailsService.clearBankDetails(staffRef);
        return ResponseEntity.noContent().build();
    }
}
