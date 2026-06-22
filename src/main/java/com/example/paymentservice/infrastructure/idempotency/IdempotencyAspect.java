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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyService idempotencyService;

    @Around("@annotation(idempotentConfig)")
    public PaymentResponse handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotentConfig) throws Throwable {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder
                        .currentRequestAttributes())
                            .getRequest();

        String headerName = idempotentConfig.headerName();
        String key = request.getHeader(headerName);

        if (key == null || key.isBlank()) throw new MissingRequiredHeaderException(headerName);

        Object cachedResponse = idempotencyService.get(key);

        if (cachedResponse instanceof PaymentResponse paymentResponse) return paymentResponse;

        PaymentResponse result = (PaymentResponse) joinPoint.proceed();

        idempotencyService.save(key, result);

        return result;
    }
}
