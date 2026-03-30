package com.project.edusync.enrollment.service;

import com.project.edusync.enrollment.model.dto.BulkImportReportDTO;
import com.project.edusync.enrollment.model.dto.BulkRoomImportReportDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface BulkImportService {

    /**
     * Parses a CSV file and imports users based on the userType.
     * Emits real-time SSE progress events to the client identified by sessionId.
     *
     * @param file      The CSV file.
     * @param userType  "students" or "staff".
     * @param sessionId Unique ID for the import session; used to locate the SSE emitter.
     *                  Pass null to skip SSE emission (backwards-compatible).
     * @return A DTO containing the import results.
     * @throws IOException              if there is an issue reading the file.
     * @throws IllegalArgumentException if userType is invalid.
     */
    BulkImportReportDTO importUsers(MultipartFile file, String userType, String sessionId) throws IOException, IllegalArgumentException;

    /**
     * Imports students and guardians from two separate CSV files.
     * Guardians are matched by student enrollment number and linked after each student row is created.
     */
    BulkImportReportDTO importStudentsWithGuardians(MultipartFile studentsFile,
                                                    MultipartFile guardiansFile,
                                                    String sessionId) throws IOException;

    BulkRoomImportReportDTO importRooms(MultipartFile file, String sessionId) throws IOException;
}