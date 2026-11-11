package gr.aueb.cf.automation.license.storage;

public record StoredLicenseActivation(
        String licenseId,
        String activationCode
) {
}