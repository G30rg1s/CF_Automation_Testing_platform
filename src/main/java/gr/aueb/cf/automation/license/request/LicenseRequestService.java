package gr.aueb.cf.automation.license.request;

import gr.aueb.cf.automation.license.model.LicenseState;
import gr.aueb.cf.automation.license.service.LicenseStateService;
import gr.aueb.cf.automation.version.ApplicationVersionService;
import org.springframework.stereotype.Service;

@Service
public class LicenseRequestService {

    private final LicenseStateService licenseStateService;
    private final ApplicationVersionService applicationVersionService;


    public LicenseRequestService(
            LicenseStateService licenseStateService,
            ApplicationVersionService applicationVersionService
    ) {

        this.licenseStateService =
                licenseStateService;

        this.applicationVersionService =
                applicationVersionService;
    }


    public LicenseRequest createRequest(
            LicenseRequestForm form
    ) {

        String existingLicenseId = null;
        String existingLicenseStatus = null;
        String existingRuntimeStatus = null;
        String existingLicenseMessage = null;


        if (
                form.requestType()
                        == LicenseRequestType.EXISTING_LICENSE_PROBLEM
        ) {

            LicenseState state =
                    licenseStateService
                            .getCurrentState();


            existingLicenseId =
                    state.licenseId();

            existingLicenseStatus =
                    state.status() != null
                            ? state.status().name()
                            : null;

            existingRuntimeStatus =
                    state.runtimeStatus() != null
                            ? state.runtimeStatus().name()
                            : null;

            existingLicenseMessage =
                    state.message();
        }


        return new LicenseRequest(
                form.requestType(),
                normalize(form.firstName()),
                normalize(form.lastName()),
                normalize(form.applicantType()),
                normalizeOptional(form.teamName()),
                normalize(form.email()),
                normalizeOptional(form.phone()),
                normalizeOptional(form.description()),
                normalize(form.reason()),
                existingLicenseId,
                existingLicenseStatus,
                existingRuntimeStatus,
                existingLicenseMessage,
                applicationVersionService.getCurrentVersion()
        );
    }


    private String normalize(
            String value
    ) {

        return value == null
                ? null
                : value.trim();
    }


    private String normalizeOptional(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isBlank()
                ? null
                : normalized;
    }
}