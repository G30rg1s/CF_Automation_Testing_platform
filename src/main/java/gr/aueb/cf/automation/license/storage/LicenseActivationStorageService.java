package gr.aueb.cf.automation.license.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.aueb.cf.automation.license.config.LicenseRuntimeProperties;
import gr.aueb.cf.automation.license.crypto.LicenseCryptoService;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Service
public class LicenseActivationStorageService {

    private final ObjectMapper objectMapper;

    private final LicenseCryptoService licenseCryptoService;

    private final Path activationStoragePath;


    public LicenseActivationStorageService(
            ObjectMapper objectMapper,
            LicenseCryptoService licenseCryptoService,
            LicenseRuntimeProperties properties
    ) {

        this.objectMapper =
                objectMapper;

        this.licenseCryptoService =
                licenseCryptoService;

        this.activationStoragePath =
                properties.normalizedActivationStoragePath();
    }


    /* ========================================
       LOAD
       ======================================== */

    public Optional<StoredLicenseActivation> load() {

        if (
                !Files.isRegularFile(
                        activationStoragePath
                )
        ) {
            return Optional.empty();
        }


        try {

            StoredLicenseActivation stored =
                    objectMapper.readValue(
                            activationStoragePath.toFile(),
                            StoredLicenseActivation.class
                    );


            if (stored == null) {

                throw new LicenseStorageException(
                        "Stored license activation is empty."
                );
            }


            String normalizedLicenseId =
                    licenseCryptoService
                            .normalizeLicenseId(
                                    stored.licenseId()
                            );


            String normalizedActivationCode =
                    licenseCryptoService
                            .normalizeActivationCode(
                                    stored.activationCode()
                            );


            return Optional.of(
                    new StoredLicenseActivation(
                            normalizedLicenseId,
                            normalizedActivationCode
                    )
            );

        } catch (LicenseStorageException exception) {

            throw exception;

        } catch (Exception exception) {

            throw new LicenseStorageException(
                    "Could not read stored license activation.",
                    exception
            );
        }
    }


    /* ========================================
       SAVE
       ======================================== */

    public void save(
            String licenseId,
            String activationCode
    ) {

        String normalizedLicenseId =
                licenseCryptoService
                        .normalizeLicenseId(
                                licenseId
                        );


        String normalizedActivationCode =
                licenseCryptoService
                        .normalizeActivationCode(
                                activationCode
                        );


        StoredLicenseActivation activation =
                new StoredLicenseActivation(
                        normalizedLicenseId,
                        normalizedActivationCode
                );


        try {

            Path parent =
                    activationStoragePath
                            .getParent();


            if (parent == null) {

                throw new LicenseStorageException(
                        "Activation storage directory is invalid."
                );
            }


            Files.createDirectories(
                    parent
            );


            Path temporaryPath =
                    activationStoragePath.resolveSibling(
                            activationStoragePath
                                    .getFileName()
                                    + ".tmp"
                    );


            byte[] content =
                    objectMapper
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsBytes(
                                    activation
                            );


            Files.write(
                    temporaryPath,
                    content
            );


            try {

                Files.move(
                        temporaryPath,
                        activationStoragePath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );

            } catch (Exception atomicMoveException) {

                Files.move(
                        temporaryPath,
                        activationStoragePath,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

        } catch (LicenseStorageException exception) {

            throw exception;

        } catch (Exception exception) {

            throw new LicenseStorageException(
                    "Could not save license activation.",
                    exception
            );
        }
    }


    /* ========================================
       CLEAR
       ======================================== */

    public void clear() {

        try {

            Files.deleteIfExists(
                    activationStoragePath
            );


            Files.deleteIfExists(
                    activationStoragePath.resolveSibling(
                            activationStoragePath
                                    .getFileName()
                                    + ".tmp"
                    )
            );

        } catch (Exception exception) {

            throw new LicenseStorageException(
                    "Could not clear stored license activation.",
                    exception
            );
        }
    }


    /* ========================================
       EXISTS
       ======================================== */

    public boolean exists() {

        return Files.isRegularFile(
                activationStoragePath
        );
    }


    public Path getActivationStoragePath() {

        return activationStoragePath;
    }
}