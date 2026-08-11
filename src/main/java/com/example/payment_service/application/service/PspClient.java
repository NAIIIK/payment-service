package com.example.payment_service.application.service;

import com.example.payment_service.application.service.dto.PspPaymentResult;
import com.example.payment_service.domain.payment.Payment;

public interface PspClient {
    PspPaymentResult createPayment(Payment payment);
}