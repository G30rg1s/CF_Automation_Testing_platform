package gr.aueb.cf.automation.version;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ApplicationVersionService {

    private static final Logger log =
            LoggerFactory.getLogger(ApplicationVersionService.class);

    private final ObjectMapper objectMapper;
    private final Path installedVersionPath;

    public ApplicationVersionService(
            ObjectMapper objectMapper,
            @Value("${app.txt-update.installed-version-file:/app/installed-version.json}")
            String installedVersionFile
    ) {
        this.objectMapper = objectMapper;
        this.installedVersionPath = Path.of(installedVersionFile);
    }

    public String getCurrentVersion() {

        if (!Files.isRegularFile(installedVersionPath)) {
            log.warn(
                    "Installed TXT version file not found: {}",
                    installedVersionPath
            );
            return "unknown";
        }

        try {
            JsonNode root = objectMapper.readTree(
                    installedVersionPath.toFile()
            );

            JsonNode versionNode = root.path("version");

            if (!versionNode.isTextual()
                    || versionNode.asText().isBlank()) {
                log.warn("Invalid installed TXT version metadata");
                return "unknown";
            }

            return versionNode.asText().trim();

        } catch (Exception e) {
            log.warn("Failed to read installed TXT version", e);
            return "unknown";
        }
    }
}
