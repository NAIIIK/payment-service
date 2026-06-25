package com.example.paymentservice.infrastructure.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("execution(* com.example.paymentservice.application.service..*.*(..))")
    public void servicePackagePointcut() {}

    @Pointcut("execution(* com.example.paymentservice.api.exception_handler.GlobalExceptionHandler.*(..))")
    public void executionHandlerPointcut() {}

    @Around("servicePackagePointcut()")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        String signature = method.getDeclaringClass().getSimpleName()
                + "." + method.getName() + "()";

        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        Object[] maskedArgs = new Object[args.length];
        for(int i = 0; i < args.length; i++) {
            maskedArgs[i] = parameters[i].isAnnotationPresent(Sensitive.class)
                    ? "***"
                    : args[i];
        }


        log.info("Enter: {} with argument(s) = {}",
                signature, Arrays.toString(maskedArgs));

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long executionTime = System.currentTimeMillis() - startTime;

        boolean sensitiveResult = method.isAnnotationPresent(SensitiveResult.class);

        log.info("Exit: {} executed in {} ms with result = {}",
                signature, executionTime, sensitiveResult ? "***" : result);

        return result;
    }

    @Around("executionHandlerPointcut()")
    public Object logExceptionHandler(ProceedingJoinPoint joinPoint) throws Throwable {
        String signature = joinPoint.getTarget().getClass().getSimpleName()
                + "." + joinPoint.getSignature().getName() + "()";

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
}
