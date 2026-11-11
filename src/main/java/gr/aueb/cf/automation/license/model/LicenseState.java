package gr.aueb.cf.automation.license.model;

import gr.aueb.cf.automation.license.runtime.RuntimeLicenseStatus;

public record LicenseState(
        LicenseAccessStatus status,
        String licenseId,
        RuntimeLicenseStatus runtimeStatus,
        int httpStatus,
        String generatedAt,
        String message
) {

    public boolean accessAllowed() {

        return status ==
                LicenseAccessStatus.ACTIVE;
    }


    public boolean activatedLocally() {

        return licenseId != null;
    }
}