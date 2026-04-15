package com.project.edusync.hrms.service;

import com.project.edusync.hrms.dto.bank.BankDetailsBulkImportResultDTO;
import com.project.edusync.hrms.dto.bank.BankDetailsUpdateDTO;
import com.project.edusync.hrms.dto.bank.StaffBankStatusDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface StaffBankDetailsService {

    /** Paginated list of all active staff with their bank status. */
    Page<StaffBankStatusDTO> listAll(Pageable pageable);

    /** List of salary-mapped staff whose bank account or IFSC is missing. */
    List<StaffBankStatusDTO> listMissing();

    /** CSV template pre-filled with employeeId + staffName for all active staff. */
    byte[] exportCsvTemplate();

    /** Parse CSV and upsert bank details for each non-blank data row. */
    BankDetailsBulkImportResultDTO bulkImport(MultipartFile file) throws IOException;

    /** Get bank status for a single staff (by UUID or numeric id). */
    StaffBankStatusDTO getByStaffRef(String staffRef);

    /** Create or update bank details for a single staff member. */
    StaffBankStatusDTO upsert(String staffRef, BankDetailsUpdateDTO dto);

    /** Clear all bank-related fields for a staff member. */
    void clearBankDetails(String staffRef);
}
