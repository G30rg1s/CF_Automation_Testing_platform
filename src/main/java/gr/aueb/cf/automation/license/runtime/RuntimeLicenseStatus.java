package gr.aueb.cf.automation.license.runtime;

public enum RuntimeLicenseStatus {

    VALID,

    NOT_FOUND,

    INVALID_JSON,

    INVALID_SIGNATURE,

    DECRYPTION_FAILED,

    HTTP_ERROR,

    NETWORK_ERROR
}