package com.food_delivery.backend;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    // Target all controller classes
    @Pointcut("within(com.food_delivery.backend.controller..*)")
    public void controllerLayer() {}

    // ============================================================
    // Around Advice (Main Logging)
    // ============================================================

    @Around("controllerLayer()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {

        long startTime = System.currentTimeMillis();

        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("➡️  ENTER: {}.{} | Args: {}", className, methodName, sanitizeArgs(args));

        Object result;

        try {
            result = joinPoint.proceed();
        } catch (Exception ex) {
            log.error("❌ EXCEPTION in {}.{} | Message: {}",
                    className,
                    methodName,
                    ex.getMessage(),
                    ex
            );
            throw ex;
        }

        long executionTime = System.currentTimeMillis() - startTime;

        log.info("⬅️  EXIT: {}.{} | Time: {} ms",
                className,
                methodName,
                executionTime
        );
        System.out.println();
        return result;
    }

    // ============================================================
    // Helper: Avoid logging sensitive data
    // ============================================================

    private Object[] sanitizeArgs(Object[] args) {
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg == null) return null;

                    String str = arg.toString().toLowerCase();

                    // Avoid logging passwords/tokens
                    if (str.contains("password") || str.contains("token")) {
                        return "*****";
                    }

                    return arg;
                })
                .toArray();
    }
}