package com.example.payment_service.application.service.dto;

import com.example.payment_service.domain.payment.Payment;

public record PaymentCreationResult(Payment payment, String clientSecret) {}
