package gr.aueb.cf.automation.database;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class DatabaseModePageFilter
        extends OncePerRequestFilter {

    private static final String DATABASE_COOKIE =
            "database_target";


    /* ========================================
       FILTER
       ======================================== */

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path =
                request.getRequestURI();


        /* ========================================
           ONLY AUTOMATION PAGES
           ======================================== */

        if (!path.startsWith("/automation")) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        /* ========================================
           ROUTES ALLOWED WITHOUT MODE
           ======================================== */

        if (
                path.equals("/automation/homepage") ||
                        path.equals("/automation/database-mode") ||
                        path.equals("/automation/unauthorized")
        ) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        /* ========================================
           DATABASE MODE REQUIRED
           ======================================== */

        if (!hasValidDatabaseMode(request)) {

            response.sendRedirect(
                    "/automation/homepage"
            );

            return;
        }


        filterChain.doFilter(
                request,
                response
        );
    }


    /* ========================================
       VALID DATABASE MODE
       ======================================== */

    private boolean hasValidDatabaseMode(
            HttpServletRequest request
    ) {

        Cookie[] cookies =
                request.getCookies();

        if (cookies == null) {

            return false;
        }


        for (Cookie cookie : cookies) {

            if (!DATABASE_COOKIE.equals(
                    cookie.getName()
            )) {

                continue;
            }


            String value =
                    cookie.getValue();


            if (value == null) {

                return false;
            }


            return switch (
                    value.trim()
                            .toLowerCase()
                    ) {

                case "production",
                     "frontendtesting",
                     "apitesting" -> true;

                default -> false;
            };
        }


        return false;
    }
}