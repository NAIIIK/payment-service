package com.example.paymentservice.infrastructure.persistence.repository;

import com.example.paymentservice.infrastructure.persistence.entity.PaymentHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataPaymentHistoryRepository
        extends JpaRepository<PaymentHistoryJpaEntity, UUID> {
}
