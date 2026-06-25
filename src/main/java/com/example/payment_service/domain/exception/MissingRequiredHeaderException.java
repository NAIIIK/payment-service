package com.example.payment_service.domain.exception;

public class MissingRequiredHeaderException extends RuntimeException {
    public MissingRequiredHeaderException(String headerName) {
        super("Missing required header: " + headerName);
    }
}
