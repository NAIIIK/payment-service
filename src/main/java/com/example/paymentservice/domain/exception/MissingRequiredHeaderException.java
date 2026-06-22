package com.example.paymentservice.domain.exception;

public class MissingRequiredHeaderException extends RuntimeException {
    public MissingRequiredHeaderException(String headerName) {
        super("Missing required header: " + headerName);
    }
}
