package com.example.payment_service.api.controller;

import com.example.payment_service.application.service.PaymentService;
import com.example.payment_service.domain.exception.InvalidPaymentStatusException;
import com.example.payment_service.infrastructure.config.StripeConfig;
import com.example.payment_service.infrastructure.psp.StripeEventTypes;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
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

        if (!isPaymentIntentEvent(event.getType())) {
            log.debug("Ignoring unhandled Stripe event type {}", event.getType());
            return ResponseEntity.ok().build();
        }

        PaymentIntent intent = extractPaymentIntent(event);
        if (intent == null) {
            return ResponseEntity.internalServerError().build();
        }

        switch (event.getType()) {
            case StripeEventTypes.PAYMENT_INTENT_SUCCEEDED -> handleSucceeded(intent);
            case StripeEventTypes.PAYMENT_INTENT_PAYMENT_FAILED -> handleFailed(intent);
        }

        return ResponseEntity.ok().build();
    }

    private PaymentIntent extractPaymentIntent(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        if (deserializer.getObject().isPresent()) {
            return (PaymentIntent) deserializer.getObject().get();
        }

        log.warn("API version mismatch for event {}, falling back to deserializeUnsafe()", event.getId());
        try {
            StripeObject stripeObject = deserializer.deserializeUnsafe();
            if (stripeObject instanceof PaymentIntent intent) {
                return intent;
            }
            log.error("Unsafe deserialization of event {} produced unexpected type: {}",
                    event.getId(), stripeObject.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Critical deserialization failure for event {}: {}", event.getId(), e.getMessage(), e);
        }

        return null;
    }

    private boolean isPaymentIntentEvent(String eventType) {
        return StripeEventTypes.PAYMENT_INTENT_EVENTS.contains(eventType);
    }

    private void handleSucceeded(PaymentIntent intent) {
        try {
            paymentService.completeByStripePaymentIntentId(intent.getId());
        } catch (InvalidPaymentStatusException e) {
            log.debug("Ignoring payment_intent.succeeded for {} : {}", intent.getId(), e.getMessage());
        }
    }

    private void handleFailed(PaymentIntent intent) {
        try {
            paymentService.failByStripePaymentIntentId(intent.getId());
        } catch (InvalidPaymentStatusException e) {
            log.debug("Ignoring payment_intent.payment_failed for {} : {}", intent.getId(), e.getMessage());
        }
    }
}
