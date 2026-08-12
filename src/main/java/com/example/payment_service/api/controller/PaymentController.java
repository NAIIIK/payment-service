package com.example.payment_service.api.controller;

import com.example.payment_service.api.dto.PaymentCreationRequest;
import com.example.payment_service.api.dto.PaymentResponse;
import com.example.payment_service.application.service.PaymentService;
import com.example.payment_service.application.service.dto.PaymentCreationResult;
import com.example.payment_service.infrastructure.idempotency.Idempotent;
import com.example.payment_service.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public PaymentResponse createPayment(@Valid @RequestBody PaymentCreationRequest request,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        PaymentCreationResult result = paymentService.create(request, principal.getId());

        return PaymentResponse.from(result.payment(), result.clientSecret());
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse completePayment(@PathVariable UUID id) {
        return PaymentResponse.from(paymentService.complete(id));
    }

    @PatchMapping("/{id}/fail")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse failPayment(@PathVariable UUID id) {
        return PaymentResponse.from(paymentService.fail(id));
    }
}
