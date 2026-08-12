package com.example.payment_service.infrastructure.psp;

import java.util.List;

public final class StripeEventTypes {

    private StripeEventTypes() {}

    public static final String PAYMENT_INTENT_SUCCEEDED = "payment_intent.succeeded";
    public static final String PAYMENT_INTENT_PAYMENT_FAILED = "payment_intent.payment_failed";

    public static final List<String> PAYMENT_INTENT_EVENTS = List.of(
            PAYMENT_INTENT_SUCCEEDED,
            PAYMENT_INTENT_PAYMENT_FAILED
    );
}
