package com.project.edusync.ams.model.exception;

import com.project.edusync.common.exception.EdusyncException;
import org.springframework.http.HttpStatus;

/**
 * Custom exception thrown when attempting to 'soft delete' an AttendanceType
 * that is still referenced by active attendance records, ensuring data integrity.
 */
public class AttendanceTypeInUseException extends EdusyncException {
    public AttendanceTypeInUseException(Long typeId) {
        super("Cannot delete Attendance Type ID " + typeId + ". It is currently in use by attendance records.", HttpStatus.CONFLICT);
    }
}