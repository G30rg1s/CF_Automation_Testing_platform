package gr.aueb.cf.automation.license.crypto;

public class LicenseCryptoException
        extends RuntimeException {

    public LicenseCryptoException(
            String message
    ) {

        super(
                message
        );
    }


    public LicenseCryptoException(
            String message,
            Throwable cause
    ) {

        super(
                message,
                cause
        );
    }
}