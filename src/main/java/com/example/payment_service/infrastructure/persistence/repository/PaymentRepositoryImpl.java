package com.example.payment_service.infrastructure.persistence.repository;

import com.example.payment_service.domain.payment.Payment;
import com.example.payment_service.domain.payment.PaymentRepository;
import com.example.payment_service.infrastructure.persistence.entity.PaymentJpaEntity;
import com.example.payment_service.infrastructure.persistence.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final SpringDataPaymentRepository jpaRepository;
    private final PaymentMapper mapper;

    @Override
    public void save(Payment payment) {
        PaymentJpaEntity entity = jpaRepository.findById(payment.getId())
                .map(existing -> {
                    mapper.updateJpa(existing, payment);
                    return existing;
                })
                .orElse(mapper.toJpa(payment));

        jpaRepository.save(entity);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId) {
        return jpaRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .map(mapper::toDomain);
    }
}
