package com.example.payment_service.domain.payment_history;

import com.example.payment_service.domain.payment.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentHistory(
        UUID id, UUID paymentId,
        PaymentStatus oldStatus,
        PaymentStatus newStatus,
        LocalDateTime changedAt
) {}
