package com.example.paymentservice.application.service;

import com.example.paymentservice.domain.payment.PaymentStatus;
import com.example.paymentservice.domain.paymentHistory.PaymentHistory;
import com.example.paymentservice.domain.paymentHistory.PaymentHistoryRepository;

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
