package com.example.paymentservice.domain.payment;

import com.example.paymentservice.domain.money.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void should_fail_when_amount_is_negative() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-1.0"), "USD"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_fail_when_amount_is_null() {
        assertThatThrownBy(() -> new Money(null, "USD"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_fail_when_amount_is_zero() {
        assertThatThrownBy(() -> new Money(new BigDecimal("0.0"), "USD"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_fail_when_currency_is_blank() {
        assertThatThrownBy(() -> new Money(new BigDecimal("10.0"), " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_fail_when_currency_is_null() {
        assertThatThrownBy(() -> new Money(new BigDecimal("10.0"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}