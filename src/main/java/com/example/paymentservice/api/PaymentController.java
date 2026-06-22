package com.example.paymentservice.api;

import com.example.paymentservice.application.service.IdempotencyService;
import com.example.paymentservice.application.service.PaymentService;
import com.example.paymentservice.infrastructure.idempotency.Idempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    @GetMapping("/{id}")
    public PaymentResponse getPaymentById(@PathVariable UUID id){
        return PaymentResponse.from(paymentService.findById(id));
    }

    @PostMapping
    @Idempotent
    public PaymentResponse createPayment(@Valid @RequestBody PaymentCreationRequest request) {
        return PaymentResponse.from(paymentService.create(request));
    }
}
