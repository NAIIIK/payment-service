package com.example.payment_service.domain.exception;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(UUID id) {
        super("Payment not found: " + id);
    }

    public PaymentNotFoundException(String stripePaymentIntentId) {
        super("Payment not found for Stripe payment intent id: " + stripePaymentIntentId);
    }
}
