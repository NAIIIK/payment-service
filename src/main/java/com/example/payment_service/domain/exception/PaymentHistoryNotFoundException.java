package com.example.payment_service.domain.exception;

import java.util.UUID;

public class PaymentHistoryNotFoundException extends RuntimeException {
    public PaymentHistoryNotFoundException(UUID id) {
        super("Payment history not found: " + id);
    }
}
