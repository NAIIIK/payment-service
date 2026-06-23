package com.example.paymentservice.domain.paymentHistory;

import com.example.paymentservice.domain.payment.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentHistory(
        UUID id, UUID paymentId,
        PaymentStatus oldStatus,
        PaymentStatus newStatus,
        LocalDateTime changedAt
) {}
