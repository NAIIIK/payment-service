package com.example.paymentservice.domain.payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    void save(Payment payment);
    Optional<Payment> findById(UUID id);
}
