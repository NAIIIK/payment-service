package com.example.paymentservice.api.exceptionHandler;

import com.example.paymentservice.domain.exception.InvalidPaymentStatusException;
import com.example.paymentservice.domain.exception.MissingRequiredHeaderException;
import com.example.paymentservice.domain.exception.PaymentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");

        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getField(),
                        e -> e.getDefaultMessage()
                ));

        problem.setProperty("errors", errors);

        return problem;
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ProblemDetail handlePaymentNotFoundException(PaymentNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Payment not found", ex);
    }

    @ExceptionHandler(MissingRequiredHeaderException.class)
    public ProblemDetail handleMissingRequiredHeaderException(MissingRequiredHeaderException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Missing required header", ex);
    }

    @ExceptionHandler(InvalidPaymentStatusException.class)
    public ProblemDetail handleInvalidPaymentStatusException(InvalidPaymentStatusException ex) {
        return problem(HttpStatus.CONFLICT, "Invalid payment status", ex);
    }

    private ProblemDetail problem(HttpStatus status, String title, Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(ex.getMessage());
        return problem;
    }
}
