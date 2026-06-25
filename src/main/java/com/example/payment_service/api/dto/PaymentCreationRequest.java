package com.example.payment_service.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCreationRequest(
        @NotNull(message = "Sender ID is required") UUID senderId,
        @NotNull(message = "Recipient ID is required") UUID recipientId,
        @NotNull @DecimalMin(value = "0.01",
                message = "Amount must be greater than 0.01") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3,
                message = "Currency must be exactly 3 characters") String currency
) {}
