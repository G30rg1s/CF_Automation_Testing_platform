
package gr.aueb.cf.automation.version;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@Service
public class RemoteVersionService {

    private static final Logger log =
            LoggerFactory.getLogger(RemoteVersionService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String manifestUrl;

    public RemoteVersionService(
            ObjectMapper objectMapper,
            @Value("${app.txt-update.manifest-url:}") String manifestUrl
    ) {
        this.objectMapper = objectMapper;
        this.manifestUrl = manifestUrl;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public Optional<RemoteVersionInfo> fetchLatestVersion() {

        if (manifestUrl == null || manifestUrl.isBlank()) {
            log.warn("TXT version check skipped: manifest URL is not configured");
            return Optional.empty();
        }

        try {
            log.info("Checking TXT version manifest: {}", manifestUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(manifestUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn(
                        "TXT version manifest request failed with HTTP {}",
                        response.statusCode()
                );
                return Optional.empty();
            }

            RemoteVersionInfo versionInfo = objectMapper.readValue(
                    response.body(),
                    RemoteVersionInfo.class
            );

            if (versionInfo.version() == null
                    || versionInfo.version().isBlank()
                    || versionInfo.gvmFilename() == null
                    || versionInfo.gvmFilename().isBlank()
                    || versionInfo.gvmUrl() == null
                    || versionInfo.gvmUrl().isBlank()) {

                log.warn("TXT version manifest has missing required fields");
                return Optional.empty();
            }

            log.info(
                    "Remote TXT version detected: {}, GVM TXT: {}",
                    versionInfo.version(),
                    versionInfo.gvmFilename()
            );

            return Optional.of(versionInfo);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("TXT version check interrupted", e);
            return Optional.empty();

        } catch (Exception e) {
            log.warn("TXT version check failed", e);
            return Optional.empty();
        }
    }
}
