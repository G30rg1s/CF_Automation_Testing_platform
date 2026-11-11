
package gr.aueb.cf.automation.version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ApplicationUpdateMonitor {

    private static final Logger log =
            LoggerFactory.getLogger(ApplicationUpdateMonitor.class);

    private final ApplicationUpdateService applicationUpdateService;

    private volatile RemoteVersionInfo availableUpdate;
    private volatile boolean checkCompleted = false;

    public ApplicationUpdateMonitor(
            ApplicationUpdateService applicationUpdateService
    ) {
        this.applicationUpdateService = applicationUpdateService;
    }

    @Scheduled(
            initialDelayString = "${app.txt-update.initial-delay-ms:30000}",
            fixedDelayString = "${app.txt-update.check-interval-ms:300000}"
    )
    public void checkForUpdates() {

        log.info("Starting TXT SHA-256 version check");

        try {
            Optional<RemoteVersionInfo> update =
                    applicationUpdateService.checkForUpdate();

            availableUpdate = update.orElse(null);
            checkCompleted = true;

            if (update.isPresent()) {
                log.info(
                        "Installed TXT differs from GitHub release {}",
                        update.get().version()
                );
            } else {
                log.info(
                        "No different TXT release detected, or check unavailable"
                );
            }

        } catch (Exception e) {
            checkCompleted = false;
            log.error("TXT SHA-256 check failed", e);
        }
    }

    public Optional<RemoteVersionInfo> getAvailableUpdate() {
        return Optional.ofNullable(availableUpdate);
    }

    public boolean isCheckCompleted() {
        return checkCompleted;
    }
}
