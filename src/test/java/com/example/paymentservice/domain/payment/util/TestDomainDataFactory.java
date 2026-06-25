package com.example.paymentservice.domain.payment.util;

import com.example.paymentservice.domain.money.Money;
import com.example.paymentservice.domain.payment.Payment;

import java.math.BigDecimal;

public final class TestDomainDataFactory {

    private TestDomainDataFactory() {}

    public static final String POSITIVE_MONEY_AMOUNT = "100.00";
    public static final String NEGATIVE_MONEY_AMOUNT = "-100.00";
    public static final String CURRENCY = "USD";

    public static Payment createPayment() {
        Money money = new Money(new BigDecimal(POSITIVE_MONEY_AMOUNT), CURRENCY);
        return Payment.create(money, 1L, 2L);
    }
}
