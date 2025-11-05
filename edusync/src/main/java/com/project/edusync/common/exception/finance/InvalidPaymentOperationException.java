package com.project.edusync.common.exception.finance;

public class InvalidPaymentOperationException extends RuntimeException {
    public InvalidPaymentOperationException(String message) {
        super(message);
    }
}
