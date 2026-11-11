
package gr.aueb.cf.automation.logging;

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
public class ServiceLoggingAspect {

    private static final Logger log =
            LoggerFactory.getLogger(ServiceLoggingAspect.class);

    // ========================================
    // GLOBAL SERVICE LOGGING
    // ========================================

    @Around(
            "execution(public * gr.aueb.cf.automation.service..*(..))"
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
                    "SERVICE {}.{} | SUCCESS | Duration: {} ms",
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
                    "SERVICE {}.{} | FAILED | Duration: {} ms | Error: {}",
                    serviceName,
                    methodName,
                    durationMs,
                    exception.getClass().getSimpleName()
            );

            throw exception;
        }
    }
}
