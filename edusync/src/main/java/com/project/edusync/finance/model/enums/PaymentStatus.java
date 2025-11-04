package com.project.edusync.finance.model.enums;

/**
 * Represents the status of a payment transaction.
 * Mapped to the ENUM('SUCCESS', 'PENDING', 'FAILED') column.
 */
public enum PaymentStatus {
    SUCCESS,
    PENDING,
    FAILED
}
