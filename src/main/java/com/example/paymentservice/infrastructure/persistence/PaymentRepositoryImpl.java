package com.example.paymentservice.infrastructure.persistence;

import com.example.paymentservice.domain.payment.Payment;
import com.example.paymentservice.domain.payment.PaymentRepository;
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
        jpaRepository.save(mapper.toJpa(payment));
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }
}
