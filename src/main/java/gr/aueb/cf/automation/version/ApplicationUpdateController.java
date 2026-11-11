
package gr.aueb.cf.automation.version;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/update")
public class ApplicationUpdateController {

    private final ApplicationUpdateMonitor applicationUpdateMonitor;

    public ApplicationUpdateController(
            ApplicationUpdateMonitor applicationUpdateMonitor
    ) {
        this.applicationUpdateMonitor = applicationUpdateMonitor;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getUpdateStatus() {

        Map<String, Object> response = new LinkedHashMap<>();

        Optional<RemoteVersionInfo> availableUpdate =
                applicationUpdateMonitor.getAvailableUpdate();

        response.put("checkMethod", "SHA-256");
        response.put(
                "checkCompleted",
                applicationUpdateMonitor.isCheckCompleted()
        );

        if (!applicationUpdateMonitor.isCheckCompleted()) {
            response.put("updateAvailable", false);
            response.put("status", "CHECK_PENDING_OR_FAILED");

        } else if (availableUpdate.isPresent()) {
            RemoteVersionInfo update = availableUpdate.get();

            response.put("updateAvailable", true);
            response.put("status", "DIFFERENT_RELEASE");
            response.put("latestVersion", update.version());
            response.put("gvmFilename", update.gvmFilename());
            response.put("gvmUrl", update.gvmUrl());

        } else {
            response.put("updateAvailable", false);
            response.put("status", "NO_DIFFERENT_RELEASE_DETECTED");
        }

        return ResponseEntity.ok(response);
    }
}
