package com.tfm.tennis_platform.infrastructure.observability;

import com.tfm.tennis_platform.domain.exceptions.BaseException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceObservabilityAspect {

    private static final String SERVICE_TIMER = "backend.service.execution";
    private static final String SERVICE_FAILURE_COUNTER = "backend.service.failures";
    private static final String OBSERVATION_NAME = "backend.service";

    private final MeterRegistry meterRegistry;
    private final ObservationRegistry observationRegistry;

    @Around("within(com.tfm.tennis_platform.application.services..*) || " +
            "@within(org.springframework.stereotype.Service)")
    public Object observeServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String serviceName = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();
        String operation = serviceName + "." + methodName;

        Observation observation = Observation.start(OBSERVATION_NAME, observationRegistry)
                .lowCardinalityKeyValue("service", serviceName)
                .lowCardinalityKeyValue("method", methodName);
        Timer.Sample sample = Timer.start(meterRegistry);
        long startNanos = System.nanoTime();

        log.info(
                "service.start operation={} args={}",
                operation,
                summarizeArguments(joinPoint.getArgs())
        );

        try (Observation.Scope ignored = observation.openScope()) {
            Object result = joinPoint.proceed();
            long durationMillis = elapsedMillis(startNanos);
            recordTimer(sample, serviceName, methodName, "success", "None");
            observation.lowCardinalityKeyValue("outcome", "success");

            log.info(
                    "service.success operation={} durationMs={} result={}",
                    operation,
                    durationMillis,
                    summarizeValue(result)
            );

            return result;
        } catch (Throwable ex) {
            long durationMillis = elapsedMillis(startNanos);
            String exceptionName = ex.getClass().getSimpleName();
            recordTimer(sample, serviceName, methodName, "failure", exceptionName);
            recordFailure(serviceName, methodName, exceptionName);
            observation.error(ex)
                    .lowCardinalityKeyValue("outcome", "failure")
                    .lowCardinalityKeyValue("exception", exceptionName);

            if (ex instanceof BaseException) {
                log.warn(
                        "service.failure operation={} durationMs={} exception={} message={}",
                        operation,
                        durationMillis,
                        exceptionName,
                        ex.getMessage()
                );
            } else {
                log.error(
                        "service.failure operation={} durationMs={} exception={}",
                        operation,
                        durationMillis,
                        exceptionName,
                        ex
                );
            }

            throw ex;
        } finally {
            observation.stop();
        }
    }

    private void recordTimer(Timer.Sample sample, String serviceName, String methodName, String outcome, String exceptionName) {
        sample.stop(Timer.builder(SERVICE_TIMER)
                .description("Application service method execution time")
                .tag("service", serviceName)
                .tag("method", methodName)
                .tag("outcome", outcome)
                .tag("exception", exceptionName)
                .publishPercentileHistogram()
                .serviceLevelObjectives(
                        Duration.ofMillis(50),
                        Duration.ofMillis(100),
                        Duration.ofMillis(250),
                        Duration.ofMillis(500),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(5)
                )
                .register(meterRegistry));
    }

    private void recordFailure(String serviceName, String methodName, String exceptionName) {
        Counter.builder(SERVICE_FAILURE_COUNTER)
                .description("Application service method failures")
                .tag("service", serviceName)
                .tag("method", methodName)
                .tag("exception", exceptionName)
                .register(meterRegistry)
                .increment();
    }

    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private String summarizeArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        return Arrays.stream(args)
                .map(this::summarizeValue)
                .toList()
                .toString();
    }

    private String summarizeValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof CharSequence text) {
            return textSummary(text);
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Enum<?> || value instanceof UUID) {
            return value.toString();
        }
        if (value instanceof Optional<?> optional) {
            return "Optional(present=" + optional.isPresent() + ")";
        }
        if (value instanceof Collection<?> collection) {
            return value.getClass().getSimpleName() + "(size=" + collection.size() + ")";
        }
        if (value instanceof Map<?, ?> map) {
            return value.getClass().getSimpleName() + "(size=" + map.size() + ")";
        }

        Object id = extractId(value);
        if (id != null) {
            return value.getClass().getSimpleName() + "(id=" + id + ")";
        }

        return value.getClass().getSimpleName();
    }

    private String textSummary(CharSequence text) {
        if (text.toString().contains("@")) {
            return "String(emailHash=" + Integer.toHexString(text.toString().toLowerCase().hashCode()) + ")";
        }
        return "String(length=" + text.length() + ")";
    }

    private Object extractId(Object value) {
        try {
            Method getId = value.getClass().getMethod("getId");
            return getId.invoke(value);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }
}
