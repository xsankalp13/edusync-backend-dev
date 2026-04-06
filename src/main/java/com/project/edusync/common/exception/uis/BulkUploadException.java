package com.project.edusync.common.exception.uis;

import com.project.edusync.common.exception.EdusyncException;
import org.springframework.http.HttpStatus;

/**
 * Custom exception for addressing failures during bulk zip 
 * import and Cloudinary uploads.
 */
public class BulkUploadException extends EdusyncException {

    public BulkUploadException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }

    public BulkUploadException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, httpStatus, cause);
    }
}
