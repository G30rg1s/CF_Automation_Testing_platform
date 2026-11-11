
package gr.aueb.cf.automation.version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ApplicationUpdateService {

    private static final Logger log =
            LoggerFactory.getLogger(ApplicationUpdateService.class);

    private final RemoteVersionService remoteVersionService;
    private final TxtChecksumService txtChecksumService;

    public ApplicationUpdateService(
            RemoteVersionService remoteVersionService,
            TxtChecksumService txtChecksumService
    ) {
        this.remoteVersionService = remoteVersionService;
        this.txtChecksumService = txtChecksumService;
    }

    public Optional<RemoteVersionInfo> checkForUpdate() {

        TxtChecksumStatus status =
                txtChecksumService.checkLatestRelease();

        if (status == TxtChecksumStatus.ERROR) {
            log.warn("TXT checksum check failed");
            return Optional.empty();
        }

        if (status == TxtChecksumStatus.MATCH) {
            log.info("Installed TXT matches the latest GitHub release");
            return Optional.empty();
        }

        if (status == TxtChecksumStatus.DIFFERENT) {

            Optional<RemoteVersionInfo> remoteVersion =
                    remoteVersionService.fetchLatestVersion();

            if (remoteVersion.isEmpty()) {
                log.warn("Could not retrieve latest TXT version");
                return Optional.empty();
            }

            RemoteVersionInfo latest = remoteVersion.get();

            log.info(
                    "Installed TXT differs from GitHub release {}",
                    latest.version()
            );

            return Optional.of(latest);
        }

        return Optional.empty();
    }
}
