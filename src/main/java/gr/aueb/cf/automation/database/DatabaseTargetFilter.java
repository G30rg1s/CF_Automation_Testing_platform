package gr.aueb.cf.automation.database;

import gr.aueb.cf.automation.queryrunner.DatabaseTarget;
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
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseTargetFilter
        extends OncePerRequestFilter {

    private static final String DATABASE_HEADER =
            "X-Database-Target";

    private static final String DATABASE_COOKIE =
            "database_target";


    /* ========================================
       FILTER REQUEST
       ======================================== */

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestPath =
                request.getRequestURI();


        /*
         * Database selection theloume
         * mono gia ta backend API endpoints.
         *
         * Thymeleaf pages:
         * /automation/...
         *
         * Static resources:
         * /css/...
         * /js/...
         *
         * den xreiazontai database context
         * gia na fortwsoun.
         */
        if (!requestPath.startsWith(
                "/cf_automation/"
        )) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        /* ========================================
           HEALTH ENDPOINT
           ======================================== */

        if (requestPath.equals(
                "/cf_automation/health"
        )) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        /* ========================================
           READ HEADER
           ======================================== */

        String headerValue =
                request.getHeader(
                        DATABASE_HEADER
                );


        /* ========================================
           READ COOKIE
           ======================================== */

        String cookieValue =
                getDatabaseCookieValue(
                        request
                );


        try {

            DatabaseTarget headerTarget =
                    parseOptionalDatabaseTarget(
                            headerValue
                    );

            DatabaseTarget cookieTarget =
                    parseOptionalDatabaseTarget(
                            cookieValue
                    );


            /* ========================================
               CONFLICT PROTECTION
               ======================================== */

            if (
                    headerTarget != null &&
                            cookieTarget != null &&
                            headerTarget != cookieTarget
            ) {

                writeError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Database target header and cookie do not match."
                );

                return;
            }


            /* ========================================
               RESOLVE DATABASE
               ======================================== */

            DatabaseTarget databaseTarget;


            /*
             * Header exei proteraiotita
             * gia Postman / API tests /
             * automated suites.
             */
            if (headerTarget != null) {

                databaseTarget =
                        headerTarget;

            } else {

                /*
                 * Gia to browser UI
                 * xrisimopoioume to cookie
                 * pou epilexame apo to navbar.
                 */
                databaseTarget =
                        cookieTarget;
            }


            /* ========================================
               DATABASE REQUIRED
               ======================================== */

            if (databaseTarget == null) {

                writeError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Database target is required."
                );

                return;
            }


            /* ========================================
               STORE DATABASE CONTEXT
               ======================================== */

            DatabaseContextHolder.set(
                    databaseTarget
            );


            /* ========================================
               CONTINUE REQUEST
               ======================================== */

            filterChain.doFilter(
                    request,
                    response
            );

        } catch (IllegalArgumentException exception) {

            writeError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid database target."
            );

        } finally {

            /*
             * Panta clear.
             *
             * Ta server threads
             * epanaxrisimopoiountai.
             */
            DatabaseContextHolder.clear();
        }
    }


    /* ========================================
       GET DATABASE COOKIE
       ======================================== */

    private String getDatabaseCookieValue(
            HttpServletRequest request
    ) {

        Cookie[] cookies =
                request.getCookies();

        if (cookies == null) {

            return null;
        }


        for (Cookie cookie : cookies) {

            if (DATABASE_COOKIE.equals(
                    cookie.getName()
            )) {

                return cookie.getValue();
            }
        }


        return null;
    }


    /* ========================================
       PARSE OPTIONAL DATABASE TARGET
       ======================================== */

    private DatabaseTarget parseOptionalDatabaseTarget(
            String value
    ) {

        if (
                value == null ||
                        value.isBlank()
        ) {

            return null;
        }

        return parseDatabaseTarget(
                value
        );
    }


    /* ========================================
       PARSE DATABASE TARGET
       ======================================== */

    private DatabaseTarget parseDatabaseTarget(
            String value
    ) {

        return switch (
                value.trim()
                        .toLowerCase()
                ) {

            case "production" ->
                    DatabaseTarget.PRODUCTION;

            case "frontendtesting" ->
                    DatabaseTarget.FRONTEND_TESTING;

            case "apitesting" ->
                    DatabaseTarget.API_TESTING;

            default ->
                    throw new IllegalArgumentException(
                            "Unknown database target: "
                                    + value
                    );
        };
    }


    /* ========================================
       ERROR RESPONSE
       ======================================== */

    private void writeError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        response.setStatus(
                status
        );

        response.setContentType(
                "application/json"
        );

        response.setCharacterEncoding(
                "UTF-8"
        );

        response.getWriter().write(
                """
                {
                  "status": %d,
                  "message": "%s"
                }
                """.formatted(
                        status,
                        message
                )
        );
    }
}