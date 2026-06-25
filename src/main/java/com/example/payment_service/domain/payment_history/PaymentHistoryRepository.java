package com.example.payment_service.domain.payment_history;

import java.util.Optional;
import java.util.UUID;

public interface PaymentHistoryRepository {
    void save(PaymentHistory paymentHistory);
    Optional<PaymentHistory> findById(UUID id);
}
