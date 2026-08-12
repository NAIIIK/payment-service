package com.example.payment_service.infrastructure.psp;

import com.example.payment_service.application.service.PspClient;
import com.example.payment_service.application.service.dto.PspPaymentResult;
import com.example.payment_service.domain.payment.Payment;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class StripeClientImpl implements PspClient {

    @Override
    public PspPaymentResult createPayment(Payment payment) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(payment.getAmount().amount()
                            .multiply(BigDecimal.valueOf(100))
                            .longValue())
                    .setCurrency(payment.getAmount().currency().toLowerCase())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    )
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            return new PspPaymentResult(intent.getId(), intent.getClientSecret());
        } catch (StripeException e) {
            throw new RuntimeException("Stripe payment creation failed: " + e.getMessage(), e);
        }
    }
}
