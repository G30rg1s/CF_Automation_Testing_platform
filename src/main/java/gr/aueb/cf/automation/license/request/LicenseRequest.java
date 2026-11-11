package gr.aueb.cf.automation.license.request;

public record LicenseRequest(
        LicenseRequestType requestType,
        String firstName,
        String lastName,
        String applicantType,
        String teamName,
        String email,
        String phone,
        String description,
        String reason,
        String existingLicenseId,
        String existingLicenseStatus,
        String existingRuntimeStatus,
        String existingLicenseMessage,
        String applicationVersion
) {
}