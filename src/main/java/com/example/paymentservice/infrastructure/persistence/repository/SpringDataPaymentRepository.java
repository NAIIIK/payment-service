package com.example.paymentservice.infrastructure.persistence.repository;

import com.example.paymentservice.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataPaymentRepository extends JpaRepository<PaymentJpaEntity, UUID> {
}
