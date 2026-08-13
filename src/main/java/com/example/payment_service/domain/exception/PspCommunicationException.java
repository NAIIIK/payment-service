package com.example.payment_service.domain.exception;

public class PspCommunicationException extends RuntimeException {
    public PspCommunicationException(String message) {super(message);}

    public PspCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
