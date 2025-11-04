package com.project.edusync.finance.model.enums;

/**
 * Represents the method used for a payment.
 * Mapped to the ENUM('ONLINE', 'CASH', 'CHECK', 'BANK_TRANSFER') column.
 */
public enum PaymentMethod {
    ONLINE,
    CASH,
    CHECK,
    BANK_TRANSFER
}
