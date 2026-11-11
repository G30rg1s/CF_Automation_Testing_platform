
package gr.aueb.cf.automation.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class HttpRequestLoggingFilter
        extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(
                    HttpRequestLoggingFilter.class
            );

    // ========================================
    // FILTER REQUESTS
    // ========================================

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        String path = request.getRequestURI();

        // Avoid flooding logs with static assets
        // and the App Logs polling requests.

        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/webjars/")
                || path.startsWith("/favicon")
                || path.startsWith("/cf_automation/app-logs/");
    }

    // ========================================
    // GLOBAL HTTP REQUEST LOGGING
    // ========================================

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long startedAt = System.nanoTime();

        String method = request.getMethod();
        String path = request.getRequestURI();

        try {

            filterChain.doFilter(
                    request,
                    response
            );

        } finally {

            long durationMs =
                    TimeUnit.NANOSECONDS.toMillis(
                            System.nanoTime() - startedAt
                    );

            int status = response.getStatus();

            if (status >= 500) {

                log.error(
                        "HTTP {} {} | Status: {} | Duration: {} ms",
                        method,
                        path,
                        status,
                        durationMs
                );

            } else if (status >= 400) {

                log.warn(
                        "HTTP {} {} | Status: {} | Duration: {} ms",
                        method,
                        path,
                        status,
                        durationMs
                );

            } else {

                log.info(
                        "HTTP {} {} | Status: {} | Duration: {} ms",
                        method,
                        path,
                        status,
                        durationMs
                );
            }
        }
    }
}
