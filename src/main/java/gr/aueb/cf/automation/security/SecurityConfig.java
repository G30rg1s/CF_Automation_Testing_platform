
package gr.aueb.cf.automation.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ACCESS_TOKEN_COOKIE =
            "access_token";


    // ========================================
    // JWT SECRET KEY
    // ========================================

    @Bean
    public SecretKeySpec jwtKey(
            @Value("${app.jwt.secret}") String secret
    ) {

        byte[] bytes =
                secret.getBytes(StandardCharsets.UTF_8);

        if (bytes.length < 32) {

            throw new IllegalArgumentException(
                    "JWT_SECRET must contain at least 32 bytes"
            );
        }

        return new SecretKeySpec(
                bytes,
                "HmacSHA256"
        );
    }


    // ========================================
    // JWT ENCODER
    // ========================================

    @Bean
    public JwtEncoder jwtEncoder(
            SecretKeySpec key
    ) {

        return new NimbusJwtEncoder(
                new ImmutableSecret<>(key)
        );
    }


    // ========================================
    // JWT DECODER
    // ========================================

    @Bean
    public JwtDecoder jwtDecoder(
            SecretKeySpec key
    ) {

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder
                        .withSecretKey(key)
                        .macAlgorithm(MacAlgorithm.HS256)
                        .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(
                        "coding-factory-automation"
                )
        );

        return decoder;
    }


    // ========================================
    // PASSWORD ENCODER
    // ========================================

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }


    // ========================================
    // BEARER TOKEN RESOLVER
    // ========================================

    @Bean
    public BearerTokenResolver bearerTokenResolver() {

        DefaultBearerTokenResolver defaultResolver =
                new DefaultBearerTokenResolver();

        return request -> {

            /*
             * 1. Authorization: Bearer ...
             *
             * Keeps Swagger and Postman compatible.
             */

            String bearerToken =
                    defaultResolver.resolve(request);

            if (bearerToken != null) {
                return bearerToken;
            }

            /*
             * 2. If no Authorization header exists,
             *    check the HttpOnly access_token cookie.
             */

            Cookie[] cookies =
                    request.getCookies();

            if (cookies == null) {
                return null;
            }

            for (Cookie cookie : cookies) {

                if (ACCESS_TOKEN_COOKIE.equals(
                        cookie.getName()
                )) {

                    return cookie.getValue();
                }
            }

            return null;
        };
    }


    // ========================================
    // SECURITY FILTER CHAIN
    // ========================================

    @Bean
    public SecurityFilterChain security(
            HttpSecurity http,
            ObjectMapper mapper,
            BearerTokenResolver bearerTokenResolver
    ) throws Exception {

        return http

                /*
                 * Existing CSRF configuration.
                 *
                 * CSRF protection should be configured
                 * before production when using cookie auth.
                 */

                .csrf(csrf -> csrf.disable())

                .cors(cors -> {})

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // ========================================
                        // PUBLIC PAGES
                        // ========================================

                        .requestMatchers(
                                "/automation/homepage",
                                "/automation/register",
                                "/automation/login",
                                "/automation/unauthorized"
                        ).permitAll()


                        // ========================================
                        // DATABASE MODE
                        // ========================================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/automation/database-mode"
                        ).permitAll()


                        // ========================================
                        // LICENSE
                        // ========================================

                        /*
                         * Activation and licence requests
                         * must work without authentication.
                         */

                        .requestMatchers(
                                "/license/activate",
                                "/license/retry",

                                // Licence request form
                                // Allows GET and POST
                                "/license/request",
                                "/license/request/",

                                "/error"
                        ).permitAll()


                        // ========================================
                        // STATIC RESOURCES
                        // ========================================

                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()


                        // ========================================
                        // PUBLIC APIs
                        // ========================================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/cf_automation/users/register",
                                "/cf_automation/users/login"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/cf_automation/health"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/update/status"
                        ).permitAll()


                        // ========================================
                        // SWAGGER
                        // ========================================

                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()


                        // ========================================
                        // EVERYTHING ELSE
                        // ========================================

                        .anyRequest().authenticated()
                )


                // ========================================
                // OAUTH2 RESOURCE SERVER
                // ========================================

                .oauth2ResourceServer(oauth -> oauth

                        /*
                         * JWT can be provided through:
                         *
                         * Authorization: Bearer ...
                         *
                         * or
                         *
                         * Cookie: access_token=...
                         */

                        .bearerTokenResolver(
                                bearerTokenResolver
                        )

                        .jwt(jwt -> {})

                        .authenticationEntryPoint(
                                (request, response, exception) -> {

                                    if (isApiRequest(
                                            request.getRequestURI()
                                    )) {

                                        writeError(
                                                mapper,
                                                response,
                                                HttpStatus.UNAUTHORIZED,
                                                "Missing, invalid or expired token"
                                        );

                                    } else {

                                        response.sendRedirect(
                                                "/automation/unauthorized"
                                        );
                                    }
                                }
                        )
                )


                // ========================================
                // EXCEPTION HANDLING
                // ========================================

                .exceptionHandling(errors -> errors

                        .authenticationEntryPoint(
                                (request, response, exception) -> {

                                    if (isApiRequest(
                                            request.getRequestURI()
                                    )) {

                                        writeError(
                                                mapper,
                                                response,
                                                HttpStatus.UNAUTHORIZED,
                                                "Authentication required"
                                        );

                                    } else {

                                        response.sendRedirect(
                                                "/automation/unauthorized"
                                        );
                                    }
                                }
                        )

                        .accessDeniedHandler(
                                (request, response, exception) -> {

                                    if (isApiRequest(
                                            request.getRequestURI()
                                    )) {

                                        writeError(
                                                mapper,
                                                response,
                                                HttpStatus.FORBIDDEN,
                                                "Access denied"
                                        );

                                    } else {

                                        response.sendRedirect(
                                                "/automation/unauthorized"
                                        );
                                    }
                                }
                        )
                )

                .build();
    }


    // ========================================
    // CORS CONFIGURATION
    // ========================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors-origins}") String origins
    ) {

        CorsConfiguration config =
                new CorsConfiguration();

        config.setAllowedOrigins(
                Arrays.stream(origins.split(","))
                        .map(String::strip)
                        .toList()
        );

        config.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
        );

        config.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type"
                )
        );

        /*
         * Required when cookies are used
         * from allowed frontend origins.
         */

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                config
        );

        return source;
    }


    // ========================================
    // HELPERS
    // ========================================

    private boolean isApiRequest(
            String requestUri
    ) {

        return requestUri.startsWith(
                "/cf_automation/"
        );
    }


    // ========================================
    // ERROR RESPONSE
    // ========================================

    private void writeError(
            ObjectMapper mapper,
            HttpServletResponse response,
            HttpStatus status,
            String message
    ) throws IOException {

        response.setStatus(
                status.value()
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        Map<String, Object> body =
                new LinkedHashMap<>();

        body.put(
                "timestamp",
                LocalDateTime.now()
        );

        body.put(
                "status",
                status.value()
        );

        body.put(
                "error",
                status.getReasonPhrase()
        );

        body.put(
                "message",
                message
        );

        mapper.writeValue(
                response.getOutputStream(),
                body
        );
    }
}
