package gr.aueb.cf.automation.license.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.license")
public record LicenseRuntimeProperties(
        Path activationStoragePath
) {

    public Path normalizedActivationStoragePath() {

        if (activationStoragePath == null) {
            throw new IllegalStateException(
                    "License activation storage path is not configured."
            );
        }

        return activationStoragePath
                .toAbsolutePath()
                .normalize();
    }
}