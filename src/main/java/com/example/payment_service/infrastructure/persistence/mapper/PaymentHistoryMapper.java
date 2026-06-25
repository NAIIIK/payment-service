package com.example.payment_service.infrastructure.persistence.mapper;

import com.example.payment_service.domain.payment_history.PaymentHistory;
import com.example.payment_service.infrastructure.persistence.entity.PaymentHistoryJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentHistoryMapper {

    public PaymentHistoryJpaEntity toJpa(PaymentHistory paymentHistory) {
        PaymentHistoryJpaEntity entity = new PaymentHistoryJpaEntity();

        entity.setId(paymentHistory.id());
        entity.setPaymentId(paymentHistory.paymentId());
        entity.setOldStatus(paymentHistory.oldStatus());
        entity.setNewStatus(paymentHistory.newStatus());
        entity.setChangedAt(paymentHistory.changedAt());

        return entity;
    }

    public PaymentHistory toDomain(PaymentHistoryJpaEntity paymentHistoryJpaEntity) {
        return new PaymentHistory(
                paymentHistoryJpaEntity.getId(),
                paymentHistoryJpaEntity.getPaymentId(),
                paymentHistoryJpaEntity.getOldStatus(),
                paymentHistoryJpaEntity.getNewStatus(),
                paymentHistoryJpaEntity.getChangedAt()
        );
    }
}