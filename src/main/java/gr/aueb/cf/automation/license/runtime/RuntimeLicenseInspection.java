package gr.aueb.cf.automation.license.runtime;

public record RuntimeLicenseInspection(
        RuntimeLicenseStatus status,
        int httpStatus,
        RuntimeLicenseEnvelope envelope,
        RuntimeLicensePayload payload,
        String message
) {

    public boolean valid() {

        return status ==
                RuntimeLicenseStatus.VALID;
    }


    public Boolean active() {

        if (payload == null) {
            return null;
        }

        return payload.active();
    }


    public String generatedAt() {

        if (envelope == null) {
            return null;
        }

        return envelope.generatedAt();
    }
}