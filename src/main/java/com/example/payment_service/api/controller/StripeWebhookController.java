package com.example.payment_service.api.controller;

import com.example.payment_service.application.service.PaymentService;
import com.example.payment_service.domain.exception.InvalidPaymentStatusException;
import com.example.payment_service.infrastructure.config.StripeConfig;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/webhooks/stripe")
public class StripeWebhookController {

    private final PaymentService paymentService;
    private final StripeConfig stripeConfig;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().build();
        }

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isEmpty()) {
            return ResponseEntity.ok().build();
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> handleSucceeded((PaymentIntent) deserializer.getObject().get());
            case "payment_intent.payment_failed" -> handleFailed((PaymentIntent) deserializer.getObject().get());
        }

        return ResponseEntity.ok().build();
    }

    private void handleSucceeded(PaymentIntent intent) {
        try {
            paymentService.completeByStripePaymentIntentId(intent.getId());
        } catch (InvalidPaymentStatusException e) {
            log.info("Ignoring payment_intent.succeeded for {} : {}", intent.getId(), e.getMessage());
        }
    }

    private void handleFailed(PaymentIntent intent) {
        try {
            paymentService.failByStripePaymentIntentId(intent.getId());
        } catch (InvalidPaymentStatusException e) {
            log.info("Ignoring payment_intent.payment_failed for {} : {}", intent.getId(), e.getMessage());
        }
    }
}
