package com.example.paymentservice.infrastructure.persistence.repository;

import com.example.paymentservice.domain.payment.Payment;
import com.example.paymentservice.domain.payment.PaymentRepository;
import com.example.paymentservice.infrastructure.persistence.entity.PaymentJpaEntity;
import com.example.paymentservice.infrastructure.persistence.mapper.PaymentMapper;
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
                    existing.setStatus(payment.getStatus());
                    existing.setAmount(payment.getAmount().amount());
                    existing.setCurrency(payment.getAmount().currency());
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
}
