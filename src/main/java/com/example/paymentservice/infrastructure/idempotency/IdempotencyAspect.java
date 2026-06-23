package com.example.paymentservice.infrastructure.idempotency;

import com.example.paymentservice.api.PaymentResponse;
import com.example.paymentservice.application.service.IdempotencyService;
import com.example.paymentservice.domain.exception.MissingRequiredHeaderException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyService idempotencyService;
    private final HttpServletRequest request;

    @Around("@annotation(idempotentConfig)")
    public Object handleIdempotencyForPaymentResponse(ProceedingJoinPoint joinPoint, Idempotent idempotentConfig) throws Throwable {
        String headerName = idempotentConfig.headerName();
        String key = request.getHeader(headerName);

        if (key == null || key.isBlank()) throw new MissingRequiredHeaderException(headerName);

        Optional<PaymentResponse> cachedResponse = idempotencyService.get(key);
        if (cachedResponse.isPresent()) return cachedResponse.get();

        PaymentResponse result = (PaymentResponse) joinPoint.proceed();
        idempotencyService.save(key, result);

        return result;
    }
}
