package gr.aueb.cf.automation.license.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.aueb.cf.automation.license.runtime.RuntimeLicenseEnvelope;
import gr.aueb.cf.automation.license.runtime.RuntimeLicensePayload;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.regex.Pattern;

@Service
public class LicenseCryptoService {

    private static final int FORMAT_VERSION =
            1;

    private static final int PBKDF2_ITERATIONS =
            210_000;

    private static final int AES_KEY_BITS =
            256;

    private static final int GCM_TAG_BITS =
            128;


    private static final Pattern LICENSE_ID_PATTERN =
            Pattern.compile(
                    "^LIC-[A-F0-9]{16}$"
            );


    private static final Pattern ACTIVATION_CODE_PATTERN =
            Pattern.compile(
                    "^CF-[A-Za-z0-9_-]{32}$"
            );


    private final ObjectMapper objectMapper;

    private final LicensePublicKeyService publicKeyService;


    public LicenseCryptoService(
            ObjectMapper objectMapper,
            LicensePublicKeyService publicKeyService
    ) {

        this.objectMapper = objectMapper;

        this.publicKeyService =
                publicKeyService;
    }


    /* ========================================
       VERIFY + DECRYPT
       ======================================== */

    public RuntimeLicensePayload verifyAndDecrypt(
            RuntimeLicenseEnvelope envelope,
            String activationCode
    ) {

        validateEnvelope(
                envelope
        );


        String normalizedActivationCode =
                normalizeActivationCode(
                        activationCode
                );


        if (!verifySignature(envelope)) {

            throw new LicenseCryptoException(
                    "Runtime license signature is invalid."
            );
        }


        try {

            byte[] salt =
                    Base64.getDecoder()
                            .decode(
                                    envelope.salt()
                            );


            byte[] iv =
                    Base64.getDecoder()
                            .decode(
                                    envelope.iv()
                            );


            byte[] ciphertext =
                    Base64.getDecoder()
                            .decode(
                                    envelope.ciphertext()
                            );


            SecretKeySpec encryptionKey =
                    deriveEncryptionKey(
                            normalizedActivationCode,
                            salt
                    );


            Cipher cipher =
                    Cipher.getInstance(
                            "AES/GCM/NoPadding"
                    );


            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(
                            GCM_TAG_BITS,
                            iv
                    );


            cipher.init(
                    Cipher.DECRYPT_MODE,
                    encryptionKey,
                    parameterSpec
            );


            cipher.updateAAD(
                    buildAdditionalAuthenticatedData(
                            envelope
                    )
            );


            byte[] plaintext =
                    cipher.doFinal(
                            ciphertext
                    );


            RuntimeLicensePayload payload =
                    objectMapper.readValue(
                            plaintext,
                            RuntimeLicensePayload.class
                    );


            validatePayload(
                    envelope,
                    payload
            );


            return payload;

        } catch (LicenseCryptoException exception) {

            throw exception;

        } catch (Exception exception) {

            throw new LicenseCryptoException(
                    "Could not decrypt runtime license.",
                    exception
            );
        }
    }


    /* ========================================
       SIGNATURE
       ======================================== */

    public boolean verifySignature(
            RuntimeLicenseEnvelope envelope
    ) {

        validateEnvelope(
                envelope
        );


        try {

            PublicKey publicKey =
                    publicKeyService.loadPublicKey();


            Signature verifier =
                    Signature.getInstance(
                            "Ed25519"
                    );


            verifier.initVerify(
                    publicKey
            );


            verifier.update(
                    buildSignatureInput(
                            envelope
                    )
            );


            byte[] signatureBytes =
                    Base64.getDecoder()
                            .decode(
                                    envelope.signature()
                            );


            return verifier.verify(
                    signatureBytes
            );

        } catch (Exception exception) {

            throw new LicenseCryptoException(
                    "Could not verify runtime license signature.",
                    exception
            );
        }
    }


    /* ========================================
       KEY DERIVATION
       ======================================== */

    private SecretKeySpec deriveEncryptionKey(
            String activationCode,
            byte[] salt
    ) {

        PBEKeySpec keySpec =
                new PBEKeySpec(
                        activationCode.toCharArray(),
                        salt,
                        PBKDF2_ITERATIONS,
                        AES_KEY_BITS
                );


        try {

            SecretKeyFactory keyFactory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256"
                    );


            byte[] keyBytes =
                    keyFactory.generateSecret(
                            keySpec
                    ).getEncoded();


            return new SecretKeySpec(
                    keyBytes,
                    "AES"
            );

        } catch (Exception exception) {

            throw new LicenseCryptoException(
                    "Could not derive license encryption key.",
                    exception
            );

        } finally {

            keySpec.clearPassword();
        }
    }


    /* ========================================
       SIGNATURE INPUT
       ======================================== */

    private byte[] buildSignatureInput(
            RuntimeLicenseEnvelope envelope
    ) {

        String value =
                envelope.formatVersion()
                        + "\n"
                        + envelope.licenseId()
                        + "\n"
                        + envelope.generatedAt()
                        + "\n"
                        + envelope.salt()
                        + "\n"
                        + envelope.iv()
                        + "\n"
                        + envelope.ciphertext();


        return value.getBytes(
                StandardCharsets.UTF_8
        );
    }


    /* ========================================
       AES-GCM AAD
       ======================================== */

    private byte[] buildAdditionalAuthenticatedData(
            RuntimeLicenseEnvelope envelope
    ) {

        String value =
                envelope.formatVersion()
                        + "\n"
                        + envelope.licenseId()
                        + "\n"
                        + envelope.generatedAt();


        return value.getBytes(
                StandardCharsets.UTF_8
        );
    }


    /* ========================================
       VALIDATION
       ======================================== */

    private void validateEnvelope(
            RuntimeLicenseEnvelope envelope
    ) {

        if (envelope == null) {

            throw new LicenseCryptoException(
                    "Runtime license envelope is missing."
            );
        }


        if (
                envelope.formatVersion()
                        != FORMAT_VERSION
        ) {

            throw new LicenseCryptoException(
                    "Unsupported runtime license format version."
            );
        }


        String licenseId =
                normalizeLicenseId(
                        envelope.licenseId()
                );


        if (!licenseId.equals(envelope.licenseId())) {

            throw new LicenseCryptoException(
                    "Runtime license ID is not normalized."
            );
        }


        requireValue(
                envelope.generatedAt(),
                "generatedAt"
        );


        requireValue(
                envelope.salt(),
                "salt"
        );


        requireValue(
                envelope.iv(),
                "iv"
        );


        requireValue(
                envelope.ciphertext(),
                "ciphertext"
        );


        requireValue(
                envelope.signature(),
                "signature"
        );
    }


    private void validatePayload(
            RuntimeLicenseEnvelope envelope,
            RuntimeLicensePayload payload
    ) {

        if (payload == null) {

            throw new LicenseCryptoException(
                    "Runtime license payload is missing."
            );
        }


        String payloadLicenseId =
                normalizeLicenseId(
                        payload.licenseId()
                );


        if (
                !envelope.licenseId()
                        .equals(
                                payloadLicenseId
                        )
        ) {

            throw new LicenseCryptoException(
                    "Runtime license payload does not match envelope license ID."
            );
        }
    }


    /* ========================================
       NORMALIZATION
       ======================================== */

    public String normalizeLicenseId(
            String licenseId
    ) {

        if (licenseId == null) {

            throw new LicenseCryptoException(
                    "License ID is required."
            );
        }


        String normalized =
                licenseId.trim()
                        .toUpperCase();


        if (
                !LICENSE_ID_PATTERN
                        .matcher(normalized)
                        .matches()
        ) {

            throw new LicenseCryptoException(
                    "Invalid License ID format."
            );
        }


        return normalized;
    }


    public String normalizeActivationCode(
            String activationCode
    ) {

        if (activationCode == null) {

            throw new LicenseCryptoException(
                    "Activation code is required."
            );
        }


        String normalized =
                activationCode.trim();


        if (
                !ACTIVATION_CODE_PATTERN
                        .matcher(normalized)
                        .matches()
        ) {

            throw new LicenseCryptoException(
                    "Invalid activation code format."
            );
        }


        return normalized;
    }


    private void requireValue(
            String value,
            String field
    ) {

        if (
                value == null ||
                        value.isBlank()
        ) {

            throw new LicenseCryptoException(
                    "Runtime license field is missing: "
                            + field
            );
        }
    }
}