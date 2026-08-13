package com.example.payment_service.infrastructure.persistence.repository;

import com.example.payment_service.BaseIntegrationTest;
import com.example.payment_service.domain.money.Money;
import com.example.payment_service.domain.payment.Payment;
import com.example.payment_service.domain.exception.PaymentNotFoundException;
import com.example.payment_service.domain.payment.PaymentStatus;
import com.example.payment_service.domain.payment.util.TestDomainDataFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PaymentRepositoryImplTest extends BaseIntegrationTest {

    private static final String STRIPE_PAYMENT_INTENT_ID = "pi_test_" + UUID.randomUUID();
    private static final String CURRENCY = "USD";

    private Money money;

    private final PaymentRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        money = new Money(BigDecimal.TEN, CURRENCY);
    }

    public PaymentRepositoryImplTest(@Autowired PaymentRepositoryImpl repository) {
        this.repository = repository;
    }

    @Test
    void should_save_and_find_payment_by_id() {
        Payment payment = newPendingPayment();

        repository.save(payment);
        Optional<Payment> found = repository.findById(payment.getId());

        assertThat(found).isPresent();
        Payment loaded = found.get();
        assertThat(loaded.getId()).isEqualTo(payment.getId());
        assertThat(loaded.getSenderId()).isEqualTo(payment.getSenderId());
        assertThat(loaded.getRecipientId()).isEqualTo(payment.getRecipientId());
        assertThat(loaded.getAmount().amount()).isEqualByComparingTo(payment.getAmount().amount());
        assertThat(loaded.getAmount().currency()).isEqualTo(payment.getAmount().currency());
        assertThat(loaded.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(loaded.getStripePaymentIntentId()).isNull();
    }

    @Test
    void should_return_empty_when_payment_not_found_by_id() {
        Optional<Payment> found = repository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void should_update_existing_payment_on_save() {
        Payment payment = newPendingPayment();
        repository.save(payment);

        payment.assignStripePaymentIntentId(STRIPE_PAYMENT_INTENT_ID);
        payment.complete();
        repository.save(payment);

        Payment reloaded = repository.findById(payment.getId()).orElseThrow();

        assertThat(reloaded.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(reloaded.getStripePaymentIntentId()).isEqualTo(STRIPE_PAYMENT_INTENT_ID);
    }

    @Test
    void should_not_create_duplicate_row_when_save_same_payment_twice() {
        Payment payment = newPendingPayment();

        repository.save(payment);
        repository.save(payment);

        Optional<Payment> found = repository.findById(payment.getId());
        assertThat(found).isPresent();
    }

    @Test
    void should_find_payment_by_stripe_payment_intent_id() {
        Payment payment = newPendingPayment();
        payment.assignStripePaymentIntentId(STRIPE_PAYMENT_INTENT_ID);
        repository.save(payment);

        Optional<Payment> found = repository.findByStripePaymentIntentId(STRIPE_PAYMENT_INTENT_ID);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(payment.getId());
    }

    @Test
    void should_return_empty_when_stripe_payment_intent_id_not_found() {
        Optional<Payment> found =
                repository.findByStripePaymentIntentId("pi_nonexistent_" + UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    private Payment newPendingPayment() {
        return Payment.create(
                money,
                UUID.randomUUID(),
                UUID.randomUUID()
        );
    }
}