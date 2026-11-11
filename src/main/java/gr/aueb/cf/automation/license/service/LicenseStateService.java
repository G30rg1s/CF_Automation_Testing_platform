package gr.aueb.cf.automation.license.service;

import gr.aueb.cf.automation.license.model.LicenseAccessStatus;
import gr.aueb.cf.automation.license.model.LicenseState;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class LicenseStateService {

    private static final Duration VALIDATION_INTERVAL =
            Duration.ofSeconds(60);


    private final LicenseManager licenseManager;


    private volatile LicenseState currentState =
            notActivatedState();


    private volatile Instant lastValidationAt =
            Instant.EPOCH;


    public LicenseStateService(
            LicenseManager licenseManager
    ) {

        this.licenseManager =
                licenseManager;
    }


    /* ========================================
       STARTUP VALIDATION
       ======================================== */

    @EventListener(ApplicationReadyEvent.class)
    public void validateOnStartup() {

        refresh();
    }


    /* ========================================
       CURRENT STATE
       ======================================== */

    public LicenseState getCurrentState() {

        if (validationExpired()) {

            return refresh();
        }


        return currentState;
    }


    /* ========================================
       FORCE REFRESH
       ======================================== */

    public synchronized LicenseState refresh() {

        LicenseState state =
                licenseManager
                        .validateStoredLicense();


        currentState =
                state;


        lastValidationAt =
                Instant.now();


        return state;
    }


    /* ========================================
       ACTIVATE
       ======================================== */

    public synchronized LicenseState activate(
            String licenseId,
            String activationCode
    ) {

        LicenseState state =
                licenseManager.activate(
                        licenseId,
                        activationCode
                );


        currentState =
                state;


        lastValidationAt =
                Instant.now();


        return state;
    }


    /* ========================================
       CLEAR
       ======================================== */

    public synchronized void clearActivation() {

        licenseManager.clearActivation();


        currentState =
                notActivatedState();


        lastValidationAt =
                Instant.now();
    }


    /* ========================================
       VALIDATION AGE
       ======================================== */

    private boolean validationExpired() {

        Instant expiresAt =
                lastValidationAt.plus(
                        VALIDATION_INTERVAL
                );


        return Instant.now()
                .isAfter(
                        expiresAt
                );
    }


    private static LicenseState notActivatedState() {

        return new LicenseState(
                LicenseAccessStatus.NOT_ACTIVATED,
                null,
                null,
                0,
                null,
                "Application has not been activated."
        );
    }
}