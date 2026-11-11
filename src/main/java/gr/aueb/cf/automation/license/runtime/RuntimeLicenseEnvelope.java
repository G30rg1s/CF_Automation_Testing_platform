package gr.aueb.cf.automation.license.runtime;

public record RuntimeLicenseEnvelope(
        int formatVersion,
        String licenseId,
        String generatedAt,
        String salt,
        String iv,
        String ciphertext,
        String signature
) {
}