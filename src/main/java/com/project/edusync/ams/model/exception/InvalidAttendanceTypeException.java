package com.project.edusync.ams.model.exception;

import com.project.edusync.common.exception.EdusyncException;
import org.springframework.http.HttpStatus;

/**
 * Custom exception thrown when a submitted attendance short code (e.g., 'X') does not map to a valid AttendanceType in the database.
 */
public class InvalidAttendanceTypeException extends EdusyncException {
    public InvalidAttendanceTypeException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}