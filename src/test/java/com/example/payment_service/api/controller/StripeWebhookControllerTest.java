package com.example.payment_service.api.controller;

import com.example.payment_service.BaseIntegrationTest;
import com.example.payment_service.application.service.PaymentService;
import com.example.payment_service.domain.exception.InvalidPaymentStatusException;
import com.example.payment_service.domain.exception.PaymentNotFoundException;
import com.example.payment_service.domain.payment.PaymentStatus;
import com.stripe.Stripe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;
import java.util.UUID;

import static com.example.payment_service.api.controller.util.TestControllerDataFactory.PAYMENT_INTENT_ID;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class StripeWebhookControllerTest extends BaseIntegrationTest {

    private static final String URI_TEMPLATE = "/api/v1/webhooks/stripe";
    private static final String CRYPTO_CODE = "HmacSHA256";
    private static final String PAYMENT_INTENT_SUCCEEDED_EVENT_TYPE = "payment_intent.succeeded";
    private static final String PAYMENT_INTENT_PAYMENT_FAILED_EVENT_TYPE = "payment_intent.payment_failed";
    private static final String STRIPE_SIGNATURE_HEADER = "Stripe-Signature";

    private final MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    StripeWebhookControllerTest(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void should_return_200_and_complete_payment_on_valid_succeeded_event() throws Exception {
        String payload = paymentIntentEventPayload(PAYMENT_INTENT_SUCCEEDED_EVENT_TYPE, PAYMENT_INTENT_ID);

        postSignedPayload(payload)
                .andExpect(status().isOk());

        verify(paymentService).completeByStripePaymentIntentId(PAYMENT_INTENT_ID);
        verify(paymentService, never()).failByStripePaymentIntentId(anyString());
    }

    @Test
    void should_return_200_and_fail_payment_on_valid_failed_event() throws Exception {
        String payload = paymentIntentEventPayload(PAYMENT_INTENT_PAYMENT_FAILED_EVENT_TYPE, PAYMENT_INTENT_ID);

        postSignedPayload(payload)
                .andExpect(status().isOk());

        verify(paymentService).failByStripePaymentIntentId(PAYMENT_INTENT_ID);
        verify(paymentService, never()).completeByStripePaymentIntentId(anyString());
    }

    @Test
    void should_return_400_when_signature_is_invalid() throws Exception {
        String payload = paymentIntentEventPayload(PAYMENT_INTENT_SUCCEEDED_EVENT_TYPE, PAYMENT_INTENT_ID);

        String invalidSignature = "t=1234567890,v1=invalidsignature";

        mockMvc.perform(post(URI_TEMPLATE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(STRIPE_SIGNATURE_HEADER, invalidSignature)
                        .content(payload))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    void should_return_200_and_ignore_unknown_event_type() throws Exception {
        String payload = paymentIntentEventPayload("charge.succeeded", PAYMENT_INTENT_ID);

        postSignedPayload(payload)
                .andExpect(status().isOk());

        verifyNoInteractions(paymentService);
    }

    @Test
    void should_propagate_404_when_payment_not_found_for_succeeded_event() throws Exception {
        doThrow(new PaymentNotFoundException(PAYMENT_INTENT_ID))
                .when(paymentService).completeByStripePaymentIntentId(PAYMENT_INTENT_ID);

        String payload = paymentIntentEventPayload(PAYMENT_INTENT_SUCCEEDED_EVENT_TYPE, PAYMENT_INTENT_ID);

        postSignedPayload(payload)
                .andExpect(status().isNotFound());
    }

    @Test
    void should_swallow_invalid_status_transition_on_succeeded_event() throws Exception {
        doThrow(new InvalidPaymentStatusException(PaymentStatus.COMPLETED))
                .when(paymentService).completeByStripePaymentIntentId(PAYMENT_INTENT_ID);

        String payload = paymentIntentEventPayload(PAYMENT_INTENT_SUCCEEDED_EVENT_TYPE, PAYMENT_INTENT_ID);

        postSignedPayload(payload)
                .andExpect(status().isOk());
    }

    @Test
    void should_swallow_invalid_status_transition_on_failed_event() throws Exception {
        doThrow(new InvalidPaymentStatusException(PaymentStatus.FAILED))
                .when(paymentService).failByStripePaymentIntentId(PAYMENT_INTENT_ID);

        String payload = paymentIntentEventPayload(PAYMENT_INTENT_PAYMENT_FAILED_EVENT_TYPE, PAYMENT_INTENT_ID);

        postSignedPayload(payload)
                .andExpect(status().isOk());
    }

    @Test
    void should_do_no_op_on_duplicate_webhook_delivery_for_already_completed_payment() throws Exception {
        String payload = paymentIntentEventPayload(PAYMENT_INTENT_SUCCEEDED_EVENT_TYPE, PAYMENT_INTENT_ID);

        postSignedPayload(payload).andExpect(status().isOk());
        postSignedPayload(payload).andExpect(status().isOk());

        verify(paymentService, times(2)).completeByStripePaymentIntentId(PAYMENT_INTENT_ID);
    }

    private ResultActions postSignedPayload(String payload) throws Exception {
        return mockMvc.perform(post(URI_TEMPLATE)
                .contentType(MediaType.APPLICATION_JSON)
                .header(STRIPE_SIGNATURE_HEADER, signPayload(payload))
                .content(payload));
    }

    private String signPayload(String payload) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        String signedPayload = timestamp + "." + payload;

        Mac mac = Mac.getInstance(CRYPTO_CODE);
        mac.init(new SecretKeySpec(STRIPE_WEBHOOK_SECRET.getBytes(), CRYPTO_CODE));
        byte[] hmacBytes = mac.doFinal(signedPayload.getBytes());

        String hexSignature = HexFormat.of().formatHex(hmacBytes);
        return "t=" + timestamp + ",v1=" + hexSignature;
    }

    private String paymentIntentEventPayload(String eventType, String paymentIntentId) {
        return """
                {
                    "id": "evt_test_%s",
                    "object": "event",
                    "api_version": "%s",
                    "type": "%s",
                    "data": {
                        "object": {
                            "id": "%s",
                            "object": "payment_intent",
                            "status": "%s"
                        }
                    }
                }
                """.formatted(
                UUID.randomUUID(),
                Stripe.API_VERSION,
                eventType,
                paymentIntentId,
                eventType.equals("payment_intent.succeeded") ? "succeeded" : "requires_payment_method"
        );
    }
}
