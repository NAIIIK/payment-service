package com.example.payment_service.infrastructure.persistence.repository;

import com.example.payment_service.BaseIntegrationTest;
import com.example.payment_service.domain.payment.Payment;
import com.example.payment_service.domain.exception.PaymentNotFoundException;
import com.example.payment_service.domain.payment.PaymentStatus;
import com.example.payment_service.domain.payment.util.TestDomainDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PaymentRepositoryImplTest extends BaseIntegrationTest {

    private final PaymentRepositoryImpl repository;

    public PaymentRepositoryImplTest(@Autowired PaymentRepositoryImpl repository) {
        this.repository = repository;
    }

    @Test
    void should_save_and_find_payment() {
        Payment payment = TestDomainDataFactory.createPayment();

        repository.save(payment);

        Payment found = repository.findById(payment.getId())
                .orElseThrow(() -> new PaymentNotFoundException(payment.getId()));

        assertThat(found.getId()).isEqualTo(payment.getId());
        assertThat(found.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(found.getAmount().amount()).isEqualByComparingTo(new BigDecimal(TestDomainDataFactory.POSITIVE_MONEY_AMOUNT));
        assertThat(found.getAmount().currency()).isEqualTo(TestDomainDataFactory.CURRENCY);
    }

    @Test
    void should_return_empty_when_payment_not_found() {
        var result = repository.findById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }
}