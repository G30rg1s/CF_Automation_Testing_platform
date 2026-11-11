
package gr.aueb.cf.automation.license.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class LicenseRequestEmailService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String endpointUrl;

    public LicenseRequestEmailService(
            ObjectMapper objectMapper,
            @Value("${app.license-request.endpoint-url:}")
            String endpointUrl
    ) {
        this.objectMapper = objectMapper;
        this.endpointUrl = endpointUrl;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // ========================================
    // SEND REQUEST USING HTTP
    // NO EMAIL AND NO API KEY
    // ========================================

    public void send(LicenseRequest request) {

        if (endpointUrl == null || endpointUrl.isBlank()) {
            throw new IllegalStateException(
                    "License request endpoint is not configured."
            );
        }

        if (request == null) {
            throw new IllegalArgumentException(
                    "License request cannot be null."
            );
        }

        try {
            String requestBody =
                    objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    requestBody,
                                    StandardCharsets.UTF_8
                            )
                    )
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 201) {
                throw new IllegalStateException(
                        "License request endpoint returned HTTP "
                                + response.statusCode()
                );
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "License request was interrupted.",
                    exception
            );

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not submit license request.",
                    exception
            );
        }
    }
}
