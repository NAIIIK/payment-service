package com.example.payment_service.api.controller;

import com.example.payment_service.api.dto.PaymentCreationRequest;
import com.example.payment_service.api.dto.PaymentResponse;
import com.example.payment_service.application.service.PaymentService;
import com.example.payment_service.infrastructure.idempotency.Idempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{id}")
    public PaymentResponse getPaymentById(@PathVariable UUID id){
        return PaymentResponse.from(paymentService.findById(id));
    }

    @PostMapping
    @Idempotent
    public PaymentResponse createPayment(@Valid @RequestBody PaymentCreationRequest request) {
        return PaymentResponse.from(paymentService.create(request));
    }

    @PatchMapping("/{id}/process")
    public PaymentResponse processPayment(@PathVariable UUID id) {
        return PaymentResponse.from(paymentService.process(id));
    }

    @PatchMapping("/{id}/complete")
    public PaymentResponse completePayment(@PathVariable UUID id) {
        return PaymentResponse.from(paymentService.complete(id));
    }

    @PatchMapping("/{id}/fail")
    public PaymentResponse failPayment(@PathVariable UUID id) {
        return PaymentResponse.from(paymentService.fail(id));
    }
}
