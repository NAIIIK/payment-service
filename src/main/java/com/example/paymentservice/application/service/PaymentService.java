package com.example.paymentservice.application.service;

import com.example.paymentservice.api.PaymentCreationRequest;
import com.example.paymentservice.domain.money.Money;
import com.example.paymentservice.domain.payment.Payment;
import com.example.paymentservice.domain.exception.PaymentNotFoundException;
import com.example.paymentservice.domain.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repository;

    @Transactional
    public Payment create(PaymentCreationRequest request) {
        Money amount = new Money(request.amount(), request.currency());
        Payment payment = Payment.create(amount, request.senderId(), request.recipientId());

        repository.save(payment);
        return payment;
    }

    @Transactional(readOnly = true)
    public Payment findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }
}
