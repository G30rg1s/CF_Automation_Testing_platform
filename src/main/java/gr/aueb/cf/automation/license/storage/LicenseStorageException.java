package gr.aueb.cf.automation.license.storage;

public class LicenseStorageException
        extends RuntimeException {

    public LicenseStorageException(
            String message
    ) {
        super(message);
    }


    public LicenseStorageException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}