package gr.aueb.cf.automation.license.service;

import gr.aueb.cf.automation.license.crypto.LicenseCryptoException;
import gr.aueb.cf.automation.license.model.LicenseAccessStatus;
import gr.aueb.cf.automation.license.model.LicenseState;
import gr.aueb.cf.automation.license.runtime.RuntimeLicenseInspection;
import gr.aueb.cf.automation.license.runtime.RuntimeLicenseStatus;
import gr.aueb.cf.automation.license.storage.LicenseActivationStorageService;
import gr.aueb.cf.automation.license.storage.LicenseStorageException;
import gr.aueb.cf.automation.license.storage.StoredLicenseActivation;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LicenseManager {

    private final LicenseActivationStorageService storageService;

    private final PublicRuntimeLicenseService publicRuntimeLicenseService;


    public LicenseManager(
            LicenseActivationStorageService storageService,
            PublicRuntimeLicenseService publicRuntimeLicenseService
    ) {

        this.storageService =
                storageService;

        this.publicRuntimeLicenseService =
                publicRuntimeLicenseService;
    }


    /* ========================================
       ACTIVATE
       ======================================== */

    public LicenseState activate(
            String licenseId,
            String activationCode
    ) {

        RuntimeLicenseInspection inspection;

        try {

            inspection =
                    publicRuntimeLicenseService.inspect(
                            licenseId,
                            activationCode
                    );

        } catch (LicenseCryptoException exception) {

            return new LicenseState(
                    LicenseAccessStatus.INVALID_LICENSE,
                    null,
                    null,
                    0,
                    null,
                    safeMessage(
                            exception,
                            "Invalid license credentials."
                    )
            );
        }


        String resolvedLicenseId =
                resolveLicenseId(
                        licenseId,
                        inspection
                );


        LicenseState state =
                mapInspection(
                        resolvedLicenseId,
                        inspection
                );


        /*
         * We persist the credentials only when
         * the official runtime artifact is VALID.
         *
         * VALID + ACTIVE:
         *     credentials are stored and access is allowed.
         *
         * VALID + INACTIVE:
         *     credentials are also stored, but access remains
         *     locked. If the administrator activates the license
         *     later, the application can unlock automatically
         *     after the next validation without asking for the
         *     activation code again.
         *
         * INVALID / NOT_FOUND / NETWORK ERROR / HTTP ERROR:
         *     credentials are not stored.
         */

        if (
                inspection.status()
                        != RuntimeLicenseStatus.VALID
        ) {

            return state;
        }


        try {

            storageService.save(
                    resolvedLicenseId,
                    activationCode
            );

        } catch (LicenseStorageException exception) {

            return new LicenseState(
                    LicenseAccessStatus.STORAGE_ERROR,
                    resolvedLicenseId,
                    inspection.status(),
                    inspection.httpStatus(),
                    inspection.generatedAt(),
                    "License is valid, but activation could not be stored locally."
            );
        }


        return state;
    }


    /* ========================================
       VALIDATE STORED ACTIVATION
       ======================================== */

    public LicenseState validateStoredLicense() {

        Optional<StoredLicenseActivation> storedOptional;

        try {

            storedOptional =
                    storageService.load();

        } catch (
                LicenseStorageException |
                LicenseCryptoException exception
        ) {

            return new LicenseState(
                    LicenseAccessStatus.STORAGE_ERROR,
                    null,
                    null,
                    0,
                    null,
                    "Stored license activation is invalid or unreadable."
            );
        }


        if (storedOptional.isEmpty()) {

            return new LicenseState(
                    LicenseAccessStatus.NOT_ACTIVATED,
                    null,
                    null,
                    0,
                    null,
                    "Application has not been activated."
            );
        }


        StoredLicenseActivation stored =
                storedOptional.get();


        RuntimeLicenseInspection inspection;

        try {

            inspection =
                    publicRuntimeLicenseService.inspect(
                            stored.licenseId(),
                            stored.activationCode()
                    );

        } catch (LicenseCryptoException exception) {

            return new LicenseState(
                    LicenseAccessStatus.INVALID_LICENSE,
                    stored.licenseId(),
                    null,
                    0,
                    null,
                    safeMessage(
                            exception,
                            "Stored license credentials are invalid."
                    )
            );
        }


        return mapInspection(
                stored.licenseId(),
                inspection
        );
    }


    /* ========================================
       CLEAR LOCAL ACTIVATION
       ======================================== */

    public void clearActivation() {

        storageService.clear();
    }


    /* ========================================
       LOCAL ACTIVATION INFO
       ======================================== */

    public boolean hasStoredActivation() {

        return storageService.exists();
    }


    public Optional<String> getStoredLicenseId() {

        try {

            return storageService
                    .load()
                    .map(
                            StoredLicenseActivation::licenseId
                    );

        } catch (RuntimeException exception) {

            return Optional.empty();
        }
    }


    /* ========================================
       MAP RUNTIME RESULT
       ======================================== */

    private LicenseState mapInspection(
            String licenseId,
            RuntimeLicenseInspection inspection
    ) {

        if (inspection == null) {

            return new LicenseState(
                    LicenseAccessStatus.SERVICE_ERROR,
                    licenseId,
                    null,
                    0,
                    null,
                    "License validation returned no result."
            );
        }


        RuntimeLicenseStatus runtimeStatus =
                inspection.status();


        if (runtimeStatus == null) {

            return new LicenseState(
                    LicenseAccessStatus.SERVICE_ERROR,
                    licenseId,
                    null,
                    inspection.httpStatus(),
                    inspection.generatedAt(),
                    "License validation returned no status."
            );
        }


        /*
         * Cryptographically valid runtime artifact.
         */

        if (
                runtimeStatus ==
                        RuntimeLicenseStatus.VALID
        ) {

            if (
                    Boolean.TRUE.equals(
                            inspection.active()
                    )
            ) {

                return new LicenseState(
                        LicenseAccessStatus.ACTIVE,
                        licenseId,
                        runtimeStatus,
                        inspection.httpStatus(),
                        inspection.generatedAt(),
                        "License is active."
                );
            }


            return new LicenseState(
                    LicenseAccessStatus.INACTIVE,
                    licenseId,
                    runtimeStatus,
                    inspection.httpStatus(),
                    inspection.generatedAt(),
                    "License has been deactivated."
            );
        }


        /*
         * Non-valid runtime results.
         */

        return switch (runtimeStatus) {

            case NOT_FOUND ->
                    new LicenseState(
                            LicenseAccessStatus.NOT_FOUND,
                            licenseId,
                            runtimeStatus,
                            inspection.httpStatus(),
                            inspection.generatedAt(),
                            "License no longer exists."
                    );


            case INVALID_JSON,
                 INVALID_SIGNATURE,
                 DECRYPTION_FAILED ->
                    new LicenseState(
                            LicenseAccessStatus.INVALID_LICENSE,
                            licenseId,
                            runtimeStatus,
                            inspection.httpStatus(),
                            inspection.generatedAt(),
                            safeInspectionMessage(
                                    inspection,
                                    "License validation failed."
                            )
                    );


            case NETWORK_ERROR ->
                    new LicenseState(
                            LicenseAccessStatus.NETWORK_ERROR,
                            licenseId,
                            runtimeStatus,
                            inspection.httpStatus(),
                            inspection.generatedAt(),
                            "Could not validate license because the official repository is unreachable."
                    );


            case HTTP_ERROR ->
                    new LicenseState(
                            LicenseAccessStatus.SERVICE_ERROR,
                            licenseId,
                            runtimeStatus,
                            inspection.httpStatus(),
                            inspection.generatedAt(),
                            safeInspectionMessage(
                                    inspection,
                                    "Official license repository returned an error."
                            )
                    );


            case VALID ->
                    throw new IllegalStateException(
                            "VALID runtime status was not handled."
                    );
        };
    }


    /* ========================================
       RESOLVE LICENSE ID
       ======================================== */

    private String resolveLicenseId(
            String requestedLicenseId,
            RuntimeLicenseInspection inspection
    ) {

        if (
                inspection != null &&
                        inspection.envelope() != null &&
                        inspection.envelope().licenseId() != null &&
                        !inspection.envelope().licenseId().isBlank()
        ) {

            return inspection
                    .envelope()
                    .licenseId();
        }


        if (requestedLicenseId == null) {

            return null;
        }


        String value =
                requestedLicenseId.trim();


        if (value.isBlank()) {

            return null;
        }


        return value.toUpperCase();
    }


    /* ========================================
       SAFE MESSAGES
       ======================================== */

    private String safeInspectionMessage(
            RuntimeLicenseInspection inspection,
            String fallback
    ) {

        if (
                inspection.message() == null ||
                        inspection.message().isBlank()
        ) {

            return fallback;
        }


        return inspection.message();
    }


    private String safeMessage(
            RuntimeException exception,
            String fallback
    ) {

        if (
                exception.getMessage() == null ||
                        exception.getMessage().isBlank()
        ) {

            return fallback;
        }


        return exception.getMessage();
    }
}