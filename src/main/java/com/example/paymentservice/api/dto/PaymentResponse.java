package com.example.paymentservice.api.dto;

import com.example.paymentservice.domain.payment.Payment;
import com.example.paymentservice.domain.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        Long senderId,
        Long recipientId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
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
                payment.getCreatedAt()
        );
    }
}
