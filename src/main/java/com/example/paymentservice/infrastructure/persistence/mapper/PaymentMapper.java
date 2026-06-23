package com.example.paymentservice.infrastructure.persistence.mapper;

import com.example.paymentservice.domain.money.Money;
import com.example.paymentservice.domain.payment.Payment;
import com.example.paymentservice.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

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
