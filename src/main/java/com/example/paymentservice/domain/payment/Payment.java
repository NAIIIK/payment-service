package com.example.paymentservice.domain.payment;

import com.example.paymentservice.domain.exception.InvalidPaymentStatusException;
import com.example.paymentservice.domain.money.Money;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@ToString(exclude = {"senderId", "recipientId"})
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
        validateStatus(PaymentStatus.PENDING);
        this.status = PaymentStatus.PROCESSING;
    }

    public void complete() {
        validateStatus(PaymentStatus.PROCESSING);
        this.status = PaymentStatus.COMPLETED;
    }

    public void fail() {
        validateStatus(PaymentStatus.PROCESSING);
        this.status = PaymentStatus.FAILED;
    }

    private void validateStatus(PaymentStatus expected) {
        if (status != expected) {
            throw new InvalidPaymentStatusException(expected, status);
        }
    }
}
