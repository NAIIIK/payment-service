package com.example.paymentservice.application.service;

import com.example.paymentservice.api.record.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Duration TTL = Duration.ofHours(24);
    private static final String PREFIX = "idempotency:";

    private final RedisTemplate<String, PaymentResponse> redisTemplate;

    public Optional<PaymentResponse> get(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(PREFIX + key));
    }

    public void save(String key, PaymentResponse response) {
        redisTemplate.opsForValue().set(PREFIX + key, response, TTL);
    }
}
