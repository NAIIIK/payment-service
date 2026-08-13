package com.example.payment_service.application.service;

import com.example.payment_service.api.dto.PaymentCreationRequest;
import com.example.payment_service.application.service.dto.PaymentCreationResult;
import com.example.payment_service.application.service.dto.PspPaymentResult;
import com.example.payment_service.domain.exception.InvalidPaymentStatusException;
import com.example.payment_service.domain.exception.PaymentNotFoundException;
import com.example.payment_service.domain.money.Money;
import com.example.payment_service.domain.payment.Payment;
import com.example.payment_service.domain.payment.PaymentRepository;
import com.example.payment_service.domain.payment.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    private static final String STRIPE_PAYMENT_INTENT_ID = "pi_test_123";
    private static final String CLIENT_SECRET = "secret_test_123";

    private static final String MONEY_CURRENCY = "USD";
    private static final BigDecimal MONEY_AMOUNT = BigDecimal.TEN;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private PspClient pspClient;

    private PaymentService paymentService;
    private Money money;

    private final UUID senderId = UUID.randomUUID();
    private final UUID recipientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, auditService, pspClient);
        money = new Money(MONEY_AMOUNT, MONEY_CURRENCY);
    }

    @Test
    void should_create_payment_and_assign_stripe_data() {
        PaymentCreationRequest creationRequest =
                new PaymentCreationRequest(recipientId, MONEY_AMOUNT, MONEY_CURRENCY);

        when(pspClient.createPayment(any(Payment.class)))
                .thenReturn(new PspPaymentResult(STRIPE_PAYMENT_INTENT_ID, CLIENT_SECRET));

        PaymentCreationResult creationResult = paymentService.create(creationRequest, senderId);

        assertThat(creationResult.clientSecret()).isEqualTo(CLIENT_SECRET);
        Payment payment = creationResult.payment();
        assertThat(payment.getSenderId()).isEqualTo(senderId);
        assertThat(payment.getRecipientId()).isEqualTo(recipientId);
        assertThat(payment.getAmount()).isEqualTo(money);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getStripePaymentIntentId()).isEqualTo(STRIPE_PAYMENT_INTENT_ID);

        verify(paymentRepository).save(payment);
        verify(auditService).record(payment.getId(), null, PaymentStatus.PENDING);
    }

    @Test
    void should_find_payment_by_id() {
        Payment payment = pendingPayment();
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        Payment found = paymentService.findById(payment.getId());

        assertThat(found).isEqualTo(payment);
    }

    @Test
    void should_throw_when_payment_not_found_by_id() {
        UUID id =  UUID.randomUUID();
        when(paymentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.findById(id))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void should_complete_payment_by_stripe_payment_intent_id() {
        Payment payment = pendingPaymentWithStripeId();
        when(paymentRepository.findByStripePaymentIntentId(STRIPE_PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(payment));

        Payment result = paymentService.completeByStripePaymentIntentId(STRIPE_PAYMENT_INTENT_ID);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentRepository).save(payment);
        verify(auditService).record(payment.getId(), PaymentStatus.PENDING, PaymentStatus.COMPLETED);
    }

    @Test
    void should_be_no_op_when_completing_already_completed_payment_by_stripe_id() {
        Payment payment = completedPaymentWithStripeId();
        when(paymentRepository.findByStripePaymentIntentId(STRIPE_PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(payment));

        Payment result = paymentService.completeByStripePaymentIntentId(STRIPE_PAYMENT_INTENT_ID);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentRepository, never()).save(any());
        verify(auditService, never()).record(any(), any(), any());
    }

    @Test
    void should_throw_invalid_status_when_completing_failed_payment_by_stripe_id() {
        Payment payment = failedPaymentWithStripeId();
        when(paymentRepository.findByStripePaymentIntentId(STRIPE_PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.completeByStripePaymentIntentId(STRIPE_PAYMENT_INTENT_ID))
                .isInstanceOf(InvalidPaymentStatusException.class);
    }

    @Test
    void should_throw_when_completing_by_unknown_stripe_payment_intent_id() {
        when(paymentRepository.findByStripePaymentIntentId(STRIPE_PAYMENT_INTENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.completeByStripePaymentIntentId(STRIPE_PAYMENT_INTENT_ID))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void should_fail_payment_by_stripe_payment_intent_id() {
        Payment payment = pendingPaymentWithStripeId();
        when(paymentRepository.findByStripePaymentIntentId(STRIPE_PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(payment));

        Payment result = paymentService.failByStripePaymentIntentId(STRIPE_PAYMENT_INTENT_ID);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(payment);
        verify(auditService).record(payment.getId(), PaymentStatus.PENDING, PaymentStatus.FAILED);
    }

    @Test
    void should_be_no_op_when_failing_already_failed_payment_by_stripe_id() {
        Payment payment = failedPaymentWithStripeId();
        when(paymentRepository.findByStripePaymentIntentId(STRIPE_PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(payment));

        Payment result = paymentService.failByStripePaymentIntentId(STRIPE_PAYMENT_INTENT_ID);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository, never()).save(any());
        verify(auditService, never()).record(any(), any(), any());
    }

    @Test
    void should_throw_when_failing_by_unknown_stripe_payment_intent_id() {
        when(paymentRepository.findByStripePaymentIntentId(STRIPE_PAYMENT_INTENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.failByStripePaymentIntentId(STRIPE_PAYMENT_INTENT_ID))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void should_complete_payment_by_id() {
        Payment payment = pendingPayment();
        when(paymentRepository.findById(payment.getId()))
                .thenReturn(Optional.of(payment));

        Payment result = paymentService.complete(payment.getId());

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(auditService).record(payment.getId(), PaymentStatus.PENDING, PaymentStatus.COMPLETED);
    }

    @Test
    void should_fail_payment_by_id() {
        Payment payment = pendingPayment();
        when(paymentRepository.findById(payment.getId()))
                .thenReturn(Optional.of(payment));

        Payment result = paymentService.fail(payment.getId());

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(auditService).record(payment.getId(), PaymentStatus.PENDING, PaymentStatus.FAILED);
    }

    @Test
    void should_throw_invalid_status_when_completing_already_completed_payment_by_id() {
        Payment payment = completedPaymentWithStripeId();
        when(paymentRepository.findById(payment.getId()))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.complete(payment.getId()))
                .isInstanceOf(InvalidPaymentStatusException.class);

        verify(paymentRepository, never()).save(any());
        verify(auditService, never()).record(any(), any(), any());
    }

    @Test
    void should_throw_when_completing_unknown_payment_by_id() {
        UUID id = UUID.randomUUID();
        when(paymentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.complete(id))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    private Payment pendingPayment() {
        return Payment.restore(
                UUID.randomUUID(), senderId, recipientId, money,
                PaymentStatus.PENDING, null,
                LocalDateTime.now()
        );
    }

    private Payment completedPayment() {
        return Payment.restore(
                UUID.randomUUID(), senderId, recipientId, money,
                PaymentStatus.COMPLETED, null,
                LocalDateTime.now()
        );
    }

    private Payment pendingPaymentWithStripeId() {
        return Payment.restore(
                UUID.randomUUID(), senderId, recipientId, money,
                PaymentStatus.PENDING, STRIPE_PAYMENT_INTENT_ID,
                LocalDateTime.now()
        );
    }

    private Payment completedPaymentWithStripeId() {
        return Payment.restore(
                UUID.randomUUID(), senderId, recipientId, money,
                PaymentStatus.COMPLETED, STRIPE_PAYMENT_INTENT_ID,
                LocalDateTime.now()
        );
    }

    private Payment failedPaymentWithStripeId() {
        return Payment.restore(
                UUID.randomUUID(), senderId, recipientId, money,
                PaymentStatus.FAILED, STRIPE_PAYMENT_INTENT_ID,
                LocalDateTime.now()
        );
    }
}
