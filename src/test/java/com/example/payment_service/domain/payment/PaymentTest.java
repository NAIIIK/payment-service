package com.example.payment_service.domain.payment;

import com.example.payment_service.domain.exception.InvalidPaymentStatusException;
import com.example.payment_service.domain.payment.util.TestDomainDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = TestDomainDataFactory.createPayment();
    }

    @Test
    void should_create_payment_with_pending_status() {
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getId()).isNotNull();
    }

    @Test
    void should_fail_when_completing_pending_payment() {
        assertThatThrownBy(payment::complete)
                .isInstanceOf(InvalidPaymentStatusException.class);
    }

    @Test
    void should_process_and_complete_payment() {
        payment.process();
        payment.complete();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void should_fail_payment() {
        payment.process();
        payment.fail();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void should_throw_when_processing_already_processing_payment() {
        payment.process();

        assertThatThrownBy(payment::process)
                .isInstanceOf(InvalidPaymentStatusException.class);
    }

    @Test
    void should_throw_when_completing_already_completed_payment() {
        payment.process();
        payment.complete();

        assertThatThrownBy(payment::complete)
                .isInstanceOf(InvalidPaymentStatusException.class);
    }
}