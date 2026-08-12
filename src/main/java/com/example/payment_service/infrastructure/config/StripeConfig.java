package com.example.payment_service.infrastructure.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Getter
    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }
}
