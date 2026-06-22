package com.example.paymentservice.infrastructure.persistence;

import com.example.paymentservice.BaseIntegrationTest;
import com.example.paymentservice.domain.payment.Money;
import com.example.paymentservice.domain.payment.Payment;
import com.example.paymentservice.domain.payment.PaymentNotFoundException;
import com.example.paymentservice.domain.payment.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRepositoryImplTest extends BaseIntegrationTest {

    @Autowired
    private PaymentRepositoryImpl repository;

    @Test
    void should_save_and_find_payment() {
        Money amount = new Money(new BigDecimal("100.00"), "USD");
        Payment payment = Payment.create(amount, 1L, 2L);

        repository.save(payment);

        Payment found = repository.findById(payment.getId())
                .orElseThrow(() -> new PaymentNotFoundException(payment.getId()));

        assertThat(found.getId()).isEqualTo(payment.getId());
        assertThat(found.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(found.getAmount().amount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(found.getAmount().currency()).isEqualTo("USD");
    }

    @Test
    void should_return_empty_when_payment_not_found() {
        var result = repository.findById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }
}