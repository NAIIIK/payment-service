package com.example.paymentservice.infrastructure.persistence;

import com.example.paymentservice.domain.payment.Money;
import com.example.paymentservice.domain.payment.Payment;
import org.springframework.stereotype.Component;

@Component
public final class PaymentMapper {

    public PaymentJpaEntity toJpa(Payment payment) {
        PaymentJpaEntity entity = new PaymentJpaEntity();

        entity.setId(payment.getId());
        entity.setSenderId(payment.getSenderId());
        entity.setRecipientId(payment.getRecipientId());
        entity.setAmount(payment.getAmount().amount());
        entity.setCurrency(payment.getAmount().currency());
        entity.setStatus(payment.getStatus());
        entity.setCreatedAt(payment.getCreatedAt());

        return entity;
    }

    public Payment toDomain(PaymentJpaEntity entity) {
        Money money = new Money(entity.getAmount(), entity.getCurrency());

        return Payment.restore(
                entity.getId(),
                entity.getSenderId(),
                entity.getRecipientId(),
                money,
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
