// File: com/project/edusync/common/exception/GlobalExceptionHandler.java
package com.project.edusync.common.exception;

import com.project.edusync.ams.model.exception.AttendanceProcessingException;
import com.project.edusync.ams.model.exception.AttendanceRecordNotFoundException;
import com.project.edusync.common.model.dto.ErrorResponse;
import com.project.edusync.common.model.dto.ValidationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * === PRIMARY EXCEPTION HANDLER ===
     * Catches the base EdusyncException and all its children.
     * It dynamically uses the HttpStatus from the exception to build the response.
     */
    @ExceptionHandler(EdusyncException.class)
    public ResponseEntity<ErrorResponse> handleEdusyncException(EdusyncException ex, HttpServletRequest request) {

        // Get the specific status (404, 401, 400, etc.) from the thrown exception
        HttpStatus status = ex.getHttpStatus();

        // Log WARN for client errors (4xx) and ERROR for server errors (5xx)
        if (status.is5xxServerError()) {
            log.error("Internal Edusync Error [{}]: {} (Path: {})", status.value(), ex.getMessage(), request.getRequestURI(), ex);
        } else {
            log.warn("Client Edusync Error [{}]: {} (Path: {})", status.value(), ex.getMessage(), request.getRequestURI());
        }

        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );

        // Return the response with the correct, dynamic status code
        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Handles DTO validation failures from @Valid.
     * Returns HTTP 400 Bad Request with a map of field errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation error: {} (Path: {})", errors, request.getRequestURI());

        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed. Please check your input.",
                request.getRequestURI(),
                Instant.now(),
                errors
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles Spring Security authorization failures (e.g., @PreAuthorize).
     * Returns HTTP 403 Forbidden.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: User attempted an action without required permissions. (Path: {})", request.getRequestURI());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Access Denied: You do not have permission to perform this action.",
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("Malformed request body: {} (Path: {})", ex.getMostSpecificCause().getMessage(), request.getRequestURI());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Malformed request payload. Please check JSON syntax and field formats.",
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        String message = String.format("Required request parameter '%s' is missing.", ex.getParameterName());
        log.warn("Missing request parameter: {} (Path: {})", message, request.getRequestURI());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        log.warn("Constraint violation: {} (Path: {})", ex.getMessage(), request.getRequestURI());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed for one or more request parameters.",
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.warn("Data integrity violation: {} (Path: {})", ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage(), request.getRequestURI());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Operation conflicts with existing data or database constraints.",
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * A final catch-all handler for any other unexpected exceptions.
     * Returns HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        // We log the full stack trace for debugging
        log.error("An unexpected internal server error occurred: (Path: {})", request.getRequestURI(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected internal server error occurred. Please contact support.",
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AttendanceRecordNotFoundException.class)
    public ResponseEntity<Object> handleAttendanceNotFound(AttendanceRecordNotFoundException ex,
                                                           HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("statusCode", HttpStatus.NOT_FOUND.value());
        body.put("message", ex.getMessage() != null ? ex.getMessage() : "Attendance record not found");
        body.put("path", request.getRequestURI());
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(AttendanceProcessingException.class)
    public ResponseEntity<Object> handleAttendanceProcessing(AttendanceProcessingException ex,
                                                             HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT; // 409 - business rule conflict

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("statusCode", status.value());
        body.put("message", ex.getMessage() != null ? ex.getMessage() : "Business rule violation");
        body.put("path", request.getRequestURI());
        body.put("timestamp", Instant.now().toString());

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {} (Path: {})", ex.getMessage(), request.getRequestURI());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "required type";
        String message;

        if (ex.getRequiredType() != null && java.util.UUID.class.equals(ex.getRequiredType())) {
            message = String.format("Invalid value '%s' for '%s'. Expected a valid UUID.", ex.getValue(), ex.getName());
        } else if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            Object[] constants = ex.getRequiredType().getEnumConstants();
            message = String.format(
                    "Invalid value '%s' for '%s'. Allowed values: %s.",
                    ex.getValue(), ex.getName(), Arrays.toString(constants));
        } else {
            message = String.format("Invalid value '%s' for '%s'. Expected %s.", ex.getValue(), ex.getName(), requiredType);
        }

        log.warn("Type mismatch: {} (Path: {})", message, request.getRequestURI());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        log.warn("Static resource not found: method={} path={}", ex.getHttpMethod(), ex.getResourcePath());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Requested resource was not found.",
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        String allowed = ex.getSupportedHttpMethods() == null || ex.getSupportedHttpMethods().isEmpty()
                ? ""
                : " Allowed methods: " + ex.getSupportedHttpMethods();
        String message = "Request method '" + ex.getMethod() + "' is not supported for this endpoint." + allowed;

        log.warn("Method not supported: {} (Path: {})", message, request.getRequestURI());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                message,
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }

}