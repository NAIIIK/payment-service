package com.example.paymentservice.domain.payment;

import com.example.paymentservice.domain.money.Money;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Payment {

    private UUID id;
    private Long senderId;
    private Long recipientId;
    private Money amount;
    private PaymentStatus status;
    private LocalDateTime createdAt;

    private Payment() {}

    public static Payment create(Money amount, Long senderId, Long recipientId) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID();
        payment.senderId = senderId;
        payment.recipientId = recipientId;
        payment.amount = amount;
        payment.status = PaymentStatus.PENDING;
        payment.createdAt = LocalDateTime.now();

        return payment;
    }

    public static Payment restore(UUID id, Long senderId, Long recipientId,
                                  Money amount, PaymentStatus status, LocalDateTime createdAt) {
        Payment payment = new Payment();
        payment.id = id;
        payment.senderId = senderId;
        payment.recipientId = recipientId;
        payment.amount = amount;
        payment.status = status;
        payment.createdAt = createdAt;

        return payment;
    }

    public void process() {
        if (status != PaymentStatus.PENDING)
            throw new IllegalStateException(
                "Payment status must be PENDING. Current status is " + status
        );
        this.status = PaymentStatus.PROCESSING;
    }

    public void complete() {
        if (status != PaymentStatus.PROCESSING)
            throw new IllegalStateException(
                    "Payment status must be PROCESSING. Current status is " + status
            );
        this.status = PaymentStatus.COMPLETED;
    }

    public void fail() {
        if (status != PaymentStatus.PROCESSING) throw new IllegalStateException(
                "Payment status must be PROCESSING. Current status is " + status
        );
        this.status = PaymentStatus.FAILED;
    }
}
