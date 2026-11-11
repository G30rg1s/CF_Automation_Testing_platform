
package gr.aueb.cf.automation.version;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

@Service
public class TxtChecksumService {

    private static final Logger log =
            LoggerFactory.getLogger(TxtChecksumService.class);

    private final Path localTxtPath;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String manifestUrl;

    public TxtChecksumService(
            ObjectMapper objectMapper,
            @Value("${app.txt-update.local-txt-path:/app/application.gvm.txt}")
            String localTxtPath,
            @Value("${app.txt-update.manifest-url:}")
            String manifestUrl
    ) {
        this.objectMapper = objectMapper;
        this.localTxtPath = Path.of(localTxtPath);
        this.manifestUrl = manifestUrl;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public TxtChecksumStatus checkLatestRelease() {

        if (!Files.isRegularFile(localTxtPath)) {
            log.warn("Local TXT not found: {}", localTxtPath);
            return TxtChecksumStatus.ERROR;
        }

        if (manifestUrl == null || manifestUrl.isBlank()) {
            log.warn("TXT manifest URL is missing");
            return TxtChecksumStatus.ERROR;
        }

        try {
            HttpRequest manifestRequest = HttpRequest.newBuilder()
                    .uri(URI.create(manifestUrl))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> manifestResponse = httpClient.send(
                    manifestRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (manifestResponse.statusCode() != 200) {
                log.warn(
                        "TXT manifest returned HTTP {}",
                        manifestResponse.statusCode()
                );
                return TxtChecksumStatus.ERROR;
            }

            RemoteVersionInfo latest = objectMapper.readValue(
                    manifestResponse.body(),
                    RemoteVersionInfo.class
            );

            if (latest.version() == null || latest.version().isBlank()
                    || latest.gvmFilename() == null || latest.gvmFilename().isBlank()
                    || latest.gvmUrl() == null || latest.gvmUrl().isBlank()) {

                log.warn("Invalid TXT version manifest");
                return TxtChecksumStatus.ERROR;
            }

            String checksumUrl = latest.gvmUrl() + ".sha256";

            HttpRequest checksumRequest = HttpRequest.newBuilder()
                    .uri(URI.create(checksumUrl))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> checksumResponse = httpClient.send(
                    checksumRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (checksumResponse.statusCode() != 200) {
                log.warn(
                        "TXT checksum returned HTTP {}",
                        checksumResponse.statusCode()
                );
                return TxtChecksumStatus.ERROR;
            }

            String checksumText = checksumResponse.body().trim();
            String remoteHash = checksumText.split("\\s+")[0];

            if (!remoteHash.matches("(?i)[0-9a-f]{64}")) {
                log.warn("Invalid remote SHA-256");
                return TxtChecksumStatus.ERROR;
            }

            String localHash = calculateSha256(localTxtPath);

            if (localHash.equalsIgnoreCase(remoteHash)) {
                log.info(
                        "Installed TXT matches GitHub release {}",
                        latest.version()
                );
                return TxtChecksumStatus.MATCH;
            }

            log.info(
                    "Installed TXT differs from GitHub release {}",
                    latest.version()
            );
            return TxtChecksumStatus.DIFFERENT;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("TXT checksum check interrupted", e);
            return TxtChecksumStatus.ERROR;

        } catch (Exception e) {
            log.warn("TXT checksum check failed", e);
            return TxtChecksumStatus.ERROR;
        }
    }

    private String calculateSha256(Path path) throws Exception {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        return HexFormat.of().formatHex(digest.digest());
    }
}
