package com.project.edusync.ams.model.exception;

import com.project.edusync.common.exception.EdusyncException;
import org.springframework.http.HttpStatus;

/**
 * Custom exception thrown when a specific AttendanceType (by ID or ShortCode) cannot be found.
 */
public class AttendanceTypeNotFoundException extends EdusyncException {
    public AttendanceTypeNotFoundException(String identifier) {
        super("Attendance Type not found with identifier: " + identifier, HttpStatus.NOT_FOUND);
    }
}