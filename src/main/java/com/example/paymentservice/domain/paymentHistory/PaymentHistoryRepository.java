package com.example.paymentservice.domain.paymentHistory;

import java.util.Optional;
import java.util.UUID;

public interface PaymentHistoryRepository {
    void save(PaymentHistory paymentHistory);
    Optional<PaymentHistory> findById(UUID id);
}
