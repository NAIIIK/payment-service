package com.example.paymentservice.application.service;

import com.example.paymentservice.api.dto.PaymentCreationRequest;
import com.example.paymentservice.domain.money.Money;
import com.example.paymentservice.domain.payment.Payment;
import com.example.paymentservice.domain.exception.PaymentNotFoundException;
import com.example.paymentservice.domain.payment.PaymentRepository;
import com.example.paymentservice.domain.payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repository;
    private final AuditService auditService;

    @Transactional
    public Payment create(PaymentCreationRequest request) {
        Money amount = new Money(request.amount(), request.currency());
        Payment payment = Payment.create(amount, request.senderId(), request.recipientId());

        repository.save(payment);
        auditService.record(payment.getId(), null, payment.getStatus());
        return payment;
    }

    @Transactional(readOnly = true)
    public Payment findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    @Transactional
    public Payment process(UUID id) {
        return changeStatus(id, Payment::process);
    }

    @Transactional
    public Payment complete(UUID id) {
        return changeStatus(id, Payment::complete);
    }

    @Transactional
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
