package com.example.paymentservice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataPaymentRepository extends JpaRepository<PaymentJpaEntity, UUID> {
}
