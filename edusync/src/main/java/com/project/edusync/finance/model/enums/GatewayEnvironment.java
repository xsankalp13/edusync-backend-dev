package com.project.edusync.finance.model.enums;

/**
 * Represents the environment for a payment gateway (testing vs. live).
 * Note: Corrected 'SANDANDBOX' typo from CSV to 'SANDBOX'.
 * Mapped to the ENUM('SANDBOX', 'PRODUCTION') column.
 */
public enum GatewayEnvironment {
    SANDBOX,
    PRODUCTION
}