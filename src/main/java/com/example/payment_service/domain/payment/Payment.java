package com.example.payment_service.domain.payment;

import com.example.payment_service.domain.exception.InvalidPaymentStatusException;
import com.example.payment_service.domain.money.Money;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@ToString(exclude = {"senderId", "recipientId"})
public class Payment {

    private UUID id;
    private UUID senderId;
    private UUID recipientId;
    private Money amount;
    private PaymentStatus status;
    private String stripePaymentIntentId;
    private LocalDateTime createdAt;

    private Payment() {}

    public static Payment create(Money amount, UUID senderId, UUID recipientId) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID();
        payment.senderId = senderId;
        payment.recipientId = recipientId;
        payment.amount = amount;
        payment.status = PaymentStatus.PENDING;
        payment.createdAt = LocalDateTime.now();

        return payment;
    }

    public static Payment restore(UUID id, UUID senderId,
                                  UUID recipientId, Money amount,
                                  PaymentStatus status,
                                  String stripePaymentIntentId,
                                  LocalDateTime createdAt) {
        Payment payment = new Payment();
        payment.id = id;
        payment.senderId = senderId;
        payment.recipientId = recipientId;
        payment.amount = amount;
        payment.status = status;
        payment.stripePaymentIntentId = stripePaymentIntentId;
        payment.createdAt = createdAt;

        return payment;
    }

    public void assignStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public void complete() {
        validatePendingStatus();
        this.status = PaymentStatus.COMPLETED;
    }

    public void fail() {
        validatePendingStatus();
        this.status = PaymentStatus.FAILED;
    }

    private void validatePendingStatus() {
        if (status != PaymentStatus.PENDING) {
            throw new InvalidPaymentStatusException(status);
        }
    }
}
