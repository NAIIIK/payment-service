package com.example.payment_service.domain.exception;

import com.example.payment_service.domain.payment.PaymentStatus;

public class InvalidPaymentStatusException extends RuntimeException {
  public InvalidPaymentStatusException(PaymentStatus expected, PaymentStatus current) {
    super("Payment status must be " + expected + ". Current status is " + current);
  }
}
