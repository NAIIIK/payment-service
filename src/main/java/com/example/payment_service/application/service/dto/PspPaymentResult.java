package com.example.payment_service.application.service.dto;

public record PspPaymentResult(String paymentIntentId, String clientSecret) {}
