
package gr.aueb.cf.parser.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class ParserLoggingAspect {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ParserLoggingAspect.class
            );

    // ========================================
    // GLOBAL PARSER SERVICE LOGGING
    // ========================================

    @Around(
            "execution(public * gr.aueb.cf.parser.service..*(..))"
    )
    public Object logServiceExecution(
            ProceedingJoinPoint joinPoint
    ) throws Throwable {

        MethodSignature signature =
                (MethodSignature) joinPoint.getSignature();

        String serviceName =
                signature.getDeclaringType()
                        .getSimpleName();

        String methodName =
                signature.getName();

        long startedAt = System.nanoTime();

        try {

            Object result = joinPoint.proceed();

            long durationMs =
                    TimeUnit.NANOSECONDS.toMillis(
                            System.nanoTime() - startedAt
                    );

            log.info(
                    "PARSER SERVICE {}.{} | SUCCESS | Duration: {} ms",
                    serviceName,
                    methodName,
                    durationMs
            );

            return result;

        } catch (Throwable exception) {

            long durationMs =
                    TimeUnit.NANOSECONDS.toMillis(
                            System.nanoTime() - startedAt
                    );

            log.error(
                    "PARSER SERVICE {}.{} | FAILED | Duration: {} ms | Error: {}",
                    serviceName,
                    methodName,
                    durationMs,
                    exception.getClass().getSimpleName()
            );

            throw exception;
        }
    }
}
