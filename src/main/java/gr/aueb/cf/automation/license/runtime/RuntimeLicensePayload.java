package gr.aueb.cf.automation.license.runtime;

public record RuntimeLicensePayload(
        String licenseId,
        boolean active
) {
}