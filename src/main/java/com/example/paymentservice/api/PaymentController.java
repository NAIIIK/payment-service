package com.example.paymentservice.api;

import com.example.paymentservice.application.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService service;

    @GetMapping("/{id}")
    public PaymentResponse getPaymentById(@PathVariable UUID id){
        return PaymentResponse.from(service.findById(id));
    }

    @PostMapping
    public PaymentResponse createPayment(@Valid @RequestBody PaymentCreationRequest request) {
        return PaymentResponse.from(service.create(request));
    }
}
