package com.example.payment_service.api.dto;

import com.example.payment_service.domain.payment.Payment;
import com.example.payment_service.domain.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID senderId,
        UUID recipientId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String stripePaymentIntentId,
        LocalDateTime createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getSenderId(),
                payment.getRecipientId(),
                payment.getAmount().amount(),
                payment.getAmount().currency(),
                payment.getStatus(),
                payment.getStripePaymentIntentId(),
                payment.getCreatedAt()
        );
    }
}
