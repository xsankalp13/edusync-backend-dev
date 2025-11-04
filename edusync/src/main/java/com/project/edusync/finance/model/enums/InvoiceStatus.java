package com.project.edusync.finance.model.enums;

/**
 * Represents the lifecycle state of an invoice.
 * Mapped to the ENUM('DRAFT', 'PENDING', 'PAID', 'OVERDUE', 'CANCELLED') column.
 */
public enum InvoiceStatus {
    DRAFT,
    PENDING,
    PAID,
    OVERDUE,
    CANCELLED
}
