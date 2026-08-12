package com.example.payment_service.application.service;

import com.example.payment_service.api.dto.PaymentCreationRequest;
import com.example.payment_service.application.service.dto.PaymentCreationResult;
import com.example.payment_service.application.service.dto.PspPaymentResult;
import com.example.payment_service.domain.money.Money;
import com.example.payment_service.domain.payment.Payment;
import com.example.payment_service.domain.exception.PaymentNotFoundException;
import com.example.payment_service.domain.payment.PaymentRepository;
import com.example.payment_service.domain.payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository repository;
    private final AuditService auditService;
    private final PspClient pspClient;

    public PaymentCreationResult create(PaymentCreationRequest request, UUID senderId) {
        Money amount = new Money(request.amount(), request.currency());
        Payment payment = Payment.create(amount, senderId, request.recipientId());

        PspPaymentResult pspResult = pspClient.createPayment(payment);
        payment.assignStripePaymentIntentId(pspResult.paymentIntentId());

        repository.save(payment);

        auditService.record(payment.getId(), null, payment.getStatus());

        return new PaymentCreationResult(payment, pspResult.clientSecret());
    }

    @Transactional(readOnly = true)
    public Payment findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    public Payment completeByStripePaymentIntentId(String stripePaymentIntentId) {
        return changeStatusByStripePaymentIntentId(stripePaymentIntentId, PaymentStatus.COMPLETED, Payment::complete);
    }

    public Payment failByStripePaymentIntentId(String stripePaymentIntentId) {
        return changeStatusByStripePaymentIntentId(stripePaymentIntentId, PaymentStatus.FAILED, Payment::fail);
    }

    private Payment changeStatusByStripePaymentIntentId(String stripePaymentIntentId,
                                                        PaymentStatus alreadyTerminalStatus,
                                                        Consumer<Payment> statusChange) {

        Payment payment = repository.findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new PaymentNotFoundException(stripePaymentIntentId));

        if (payment.getStatus() == alreadyTerminalStatus) {
            return payment;
        }

        PaymentStatus oldStatus = payment.getStatus();
        statusChange.accept(payment);
        repository.save(payment);
        auditService.record(payment.getId(), oldStatus, payment.getStatus());

        return payment;
    }

    public Payment complete(UUID id) {
        return changeStatus(id, Payment::complete);
    }

    public Payment fail(UUID id) {
        return changeStatus(id, Payment::fail);
    }

    private Payment changeStatus(UUID id, Consumer<Payment> statusChange) {
        Payment payment = repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        PaymentStatus oldStatus = payment.getStatus();
        statusChange.accept(payment);
        repository.save(payment);
        auditService.record(payment.getId(), oldStatus, payment.getStatus());

        return payment;
    }
}
