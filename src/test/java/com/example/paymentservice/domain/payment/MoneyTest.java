package com.example.paymentservice.domain.payment;

import com.example.paymentservice.domain.money.Money;
import com.example.paymentservice.domain.payment.util.TestDomainDataFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void should_fail_when_amount_is_negative() {
        assertThatThrownBy(() -> new Money(new BigDecimal(TestDomainDataFactory.NEGATIVE_MONEY_AMOUNT), TestDomainDataFactory.CURRENCY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_fail_when_amount_is_null() {
        assertThatThrownBy(() -> new Money(null, TestDomainDataFactory.CURRENCY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_fail_when_amount_is_zero() {
        assertThatThrownBy(() -> new Money(new BigDecimal("0.0"), TestDomainDataFactory.CURRENCY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_fail_when_currency_is_blank() {
        assertThatThrownBy(() -> new Money(new BigDecimal(TestDomainDataFactory.POSITIVE_MONEY_AMOUNT), " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_fail_when_currency_is_null() {
        assertThatThrownBy(() -> new Money(new BigDecimal(TestDomainDataFactory.POSITIVE_MONEY_AMOUNT), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_create_money_with_valid_arguments() {
        Money money = new Money(new BigDecimal(TestDomainDataFactory.POSITIVE_MONEY_AMOUNT), TestDomainDataFactory.CURRENCY);

        assertThat(money.amount()).isEqualByComparingTo(new BigDecimal(TestDomainDataFactory.POSITIVE_MONEY_AMOUNT));
        assertThat(money.currency()).isEqualTo(TestDomainDataFactory.CURRENCY);
    }
}