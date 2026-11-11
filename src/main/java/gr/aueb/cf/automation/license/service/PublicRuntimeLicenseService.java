package gr.aueb.cf.automation.license.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.aueb.cf.automation.license.crypto.LicenseCryptoException;
import gr.aueb.cf.automation.license.crypto.LicenseCryptoService;
import gr.aueb.cf.automation.license.runtime.RuntimeLicenseEnvelope;
import gr.aueb.cf.automation.license.runtime.RuntimeLicenseInspection;
import gr.aueb.cf.automation.license.runtime.RuntimeLicensePayload;
import gr.aueb.cf.automation.license.runtime.RuntimeLicenseStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class PublicRuntimeLicenseService {

    private static final String OFFICIAL_RUNTIME_BASE_URL =
            "https://raw.githubusercontent.com/G30rg1s/automation-platform-license-runtime/main/licenses";


    private static final Duration CONNECT_TIMEOUT =
            Duration.ofSeconds(10);


    private static final Duration REQUEST_TIMEOUT =
            Duration.ofSeconds(15);


    private final ObjectMapper objectMapper;

    private final LicenseCryptoService licenseCryptoService;

    private final HttpClient httpClient;


    public PublicRuntimeLicenseService(
            ObjectMapper objectMapper,
            LicenseCryptoService licenseCryptoService
    ) {

        this.objectMapper =
                objectMapper;

        this.licenseCryptoService =
                licenseCryptoService;


        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(
                                CONNECT_TIMEOUT
                        )
                        .followRedirects(
                                HttpClient.Redirect.NORMAL
                        )
                        .build();
    }


    /* ========================================
       INSPECT OFFICIAL RUNTIME LICENSE
       ======================================== */

    public RuntimeLicenseInspection inspect(
            String licenseId,
            String activationCode
    ) {

        String normalizedLicenseId =
                licenseCryptoService
                        .normalizeLicenseId(
                                licenseId
                        );


        String normalizedActivationCode =
                licenseCryptoService
                        .normalizeActivationCode(
                                activationCode
                        );


        URI runtimeUri =
                buildRuntimeUri(
                        normalizedLicenseId
                );


        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                runtimeUri
                        )
                        .timeout(
                                REQUEST_TIMEOUT
                        )
                        .header(
                                "Accept",
                                "application/json"
                        )
                        .header(
                                "Cache-Control",
                                "no-cache, no-store"
                        )
                        .header(
                                "Pragma",
                                "no-cache"
                        )
                        .header(
                                "User-Agent",
                                "coding-factory-automation-license-client/1.0"
                        )
                        .GET()
                        .build();


        HttpResponse<String> response;

        try {

            response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8
                            )
                    );

        } catch (InterruptedException exception) {

            Thread.currentThread()
                    .interrupt();


            return new RuntimeLicenseInspection(
                    RuntimeLicenseStatus.NETWORK_ERROR,
                    0,
                    null,
                    null,
                    "License validation request was interrupted."
            );

        } catch (IOException exception) {

            return new RuntimeLicenseInspection(
                    RuntimeLicenseStatus.NETWORK_ERROR,
                    0,
                    null,
                    null,
                    "Could not reach the official license repository."
            );
        }


        int httpStatus =
                response.statusCode();


        /* ========================================
           NOT FOUND
           ======================================== */

        if (httpStatus == 404) {

            return new RuntimeLicenseInspection(
                    RuntimeLicenseStatus.NOT_FOUND,
                    httpStatus,
                    null,
                    null,
                    "Runtime license does not exist."
            );
        }


        /* ========================================
           OTHER HTTP ERROR
           ======================================== */

        if (
                httpStatus < 200 ||
                        httpStatus >= 300
        ) {

            return new RuntimeLicenseInspection(
                    RuntimeLicenseStatus.HTTP_ERROR,
                    httpStatus,
                    null,
                    null,
                    "Official license repository returned HTTP "
                            + httpStatus
                            + "."
            );
        }


        /* ========================================
           JSON
           ======================================== */

        RuntimeLicenseEnvelope envelope;

        try {

            envelope =
                    objectMapper.readValue(
                            response.body(),
                            RuntimeLicenseEnvelope.class
                    );

        } catch (JsonProcessingException exception) {

            return new RuntimeLicenseInspection(
                    RuntimeLicenseStatus.INVALID_JSON,
                    httpStatus,
                    null,
                    null,
                    "Runtime license contains invalid JSON."
            );
        }


        /* ========================================
           EXPECTED LICENSE ID
           ======================================== */

        if (
                envelope == null ||
                        envelope.licenseId() == null ||
                        !normalizedLicenseId.equals(
                                envelope.licenseId()
                        )
        ) {

            return new RuntimeLicenseInspection(
                    RuntimeLicenseStatus.INVALID_JSON,
                    httpStatus,
                    envelope,
                    null,
                    "Runtime license ID does not match requested license."
            );
        }


        /* ========================================
           SIGNATURE
           ======================================== */

        try {

            if (
                    !licenseCryptoService
                            .verifySignature(
                                    envelope
                            )
            ) {

                return new RuntimeLicenseInspection(
                        RuntimeLicenseStatus.INVALID_SIGNATURE,
                        httpStatus,
                        envelope,
                        null,
                        "Runtime license signature is invalid."
                );
            }

        } catch (LicenseCryptoException exception) {

            return new RuntimeLicenseInspection(
                    RuntimeLicenseStatus.INVALID_SIGNATURE,
                    httpStatus,
                    envelope,
                    null,
                    "Runtime license signature could not be verified."
            );
        }


        /* ========================================
           DECRYPT
           ======================================== */

        RuntimeLicensePayload payload;

        try {

            payload =
                    licenseCryptoService
                            .verifyAndDecrypt(
                                    envelope,
                                    normalizedActivationCode
                            );

        } catch (LicenseCryptoException exception) {

            return new RuntimeLicenseInspection(
                    RuntimeLicenseStatus.DECRYPTION_FAILED,
                    httpStatus,
                    envelope,
                    null,
                    "Activation code could not decrypt this license."
            );
        }


        /* ========================================
           VALID
           ======================================== */

        return new RuntimeLicenseInspection(
                RuntimeLicenseStatus.VALID,
                httpStatus,
                envelope,
                payload,
                "Runtime license is valid."
        );
    }


    /* ========================================
       OFFICIAL URI
       ======================================== */

    private URI buildRuntimeUri(
            String licenseId
    ) {

        /*
         * Cache-busting query parameter.
         *
         * The official source remains fixed.
         * This only prevents stale raw GitHub
         * responses after activation/deactivation.
         */

        return URI.create(
                OFFICIAL_RUNTIME_BASE_URL
                        + "/"
                        + licenseId
                        + ".json?v="
                        + System.currentTimeMillis()
        );
    }
}