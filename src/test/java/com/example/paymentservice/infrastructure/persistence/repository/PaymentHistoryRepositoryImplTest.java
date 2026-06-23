package com.example.paymentservice.infrastructure.persistence.repository;

import com.example.paymentservice.BaseIntegrationTest;
import com.example.paymentservice.domain.exception.PaymentHistoryNotFoundException;
import com.example.paymentservice.domain.payment.PaymentStatus;
import com.example.paymentservice.domain.paymentHistory.PaymentHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PaymentHistoryRepositoryImplTest extends BaseIntegrationTest {

    private final PaymentHistoryRepositoryImpl repository;

    public PaymentHistoryRepositoryImplTest(@Autowired PaymentHistoryRepositoryImpl repository) {
        this.repository = repository;
    }

    @Test
    void should_save_and_find_payment_history() {
        PaymentHistory paymentHistory = new PaymentHistory(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                PaymentStatus.PENDING,
                LocalDateTime.now()
        );

        repository.save(paymentHistory);

        PaymentHistory foundPaymentHistory = repository.findById(paymentHistory.id())
                .orElseThrow(() -> new PaymentHistoryNotFoundException(paymentHistory.id()));

        assertThat(foundPaymentHistory.id()).isEqualTo(paymentHistory.id());
        assertThat(foundPaymentHistory.paymentId()).isEqualTo(paymentHistory.paymentId());
        assertThat(foundPaymentHistory.oldStatus()).isEqualTo(paymentHistory.oldStatus());
        assertThat(foundPaymentHistory.newStatus()).isEqualTo(paymentHistory.newStatus());
    }

    @Test
    void should_return_empty_when_not_found() {
        var result = repository.findById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }
}