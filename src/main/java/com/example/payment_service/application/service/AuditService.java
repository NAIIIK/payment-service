package com.example.payment_service.application.service;

import com.example.payment_service.domain.payment.PaymentStatus;
import com.example.payment_service.domain.payment_history.PaymentHistory;
import com.example.payment_service.domain.payment_history.PaymentHistoryRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final PaymentHistoryRepository repository;

    public void record(UUID paymentId, PaymentStatus oldStatus, PaymentStatus newStatus) {
        repository.save(new PaymentHistory(
                UUID.randomUUID(),
                paymentId,
                oldStatus,
                newStatus,
                LocalDateTime.now()
        ));
    }
}
