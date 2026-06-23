package com.example.paymentservice.infrastructure.persistence.repository;

import com.example.paymentservice.domain.paymentHistory.PaymentHistory;
import com.example.paymentservice.domain.paymentHistory.PaymentHistoryRepository;
import com.example.paymentservice.infrastructure.persistence.mapper.PaymentHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentHistoryRepositoryImpl implements PaymentHistoryRepository {

    private final SpringDataPaymentHistoryRepository repository;
    private final PaymentHistoryMapper mapper;

    @Override
    public void save(PaymentHistory paymentHistory) {
        repository.save(mapper.toJpa(paymentHistory));
    }

    @Override
    public Optional<PaymentHistory> findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }
}