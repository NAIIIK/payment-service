package com.example.paymentservice.infrastructure.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("execution(* com.example.paymentservice.application.service..*.*(..))")
    public void servicePackagePointcut() {}

    @Pointcut("execution(* com.example.paymentservice.api.exceptionHandler.GlobalExceptionHandler.*(..))")
    public void executionHandlerPointcut() {}

    @Around("servicePackagePointcut()")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String signature = getMethodSignature(joinPoint);
        Object[] args = joinPoint.getArgs();

        log.info("Enter: {} with argument(s) = {}",
                signature, Arrays.toString(args));

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long executionTime = System.currentTimeMillis() - startTime;

        log.info("Exit: {} executed in {} ms with result = {}",
                signature, executionTime, result);

        return result;
    }

    @Around("executionHandlerPointcut()")
    public Object logExceptionHandler(ProceedingJoinPoint joinPoint) throws Throwable {
        String signature = getMethodSignature(joinPoint);
        Object[] args = joinPoint.getArgs();

        String exceptionMessage = Arrays.stream(args)
                .filter(a -> a instanceof Exception)
                .map(a -> {
                    if (a instanceof MethodArgumentNotValidException ex) {
                        return ex.getBindingResult().getFieldErrors().stream()
                                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                                .toList()
                                .toString();
                    }
                    return ((Exception) a).getMessage();
                })
                .findFirst()
                .orElse("unknown");

        log.warn("Exception handled in {} — {}", signature, exceptionMessage);

        return joinPoint.proceed();
    }

    private String getMethodSignature(ProceedingJoinPoint joinPoint) {
        return joinPoint.getTarget().getClass().getSimpleName()
                + "." + joinPoint.getSignature().getName() + "()";
    }
}
