package com.project.edusync.common.exception.finance;

//import com.project.edusync.common.exception.EdusyncException;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
