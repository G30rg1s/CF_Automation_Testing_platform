
package gr.aueb.cf.automation.controller;

import gr.aueb.cf.automation.database.DatabaseContextHolder;
import gr.aueb.cf.automation.queryrunner.DatabaseTarget;
import gr.aueb.cf.automation.queryrunner.TheorySetupExecutor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/cf_automation/theory-setup")
public class TheorySetupController {

    private final TheorySetupExecutor executor;

    public TheorySetupController(TheorySetupExecutor executor) {
        this.executor = executor;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {

        DatabaseTarget target = DatabaseContextHolder.require();

        return Map.of(
                "configured", executor.isConfigured(),
                "database", target.name()
        );
    }

    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> execute() {

        DatabaseTarget target = DatabaseContextHolder.require();

        if (!executor.isConfigured()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "success", false,
                            "message",
                            "Theory setup SQL steps have not been configured yet."
                    ));
        }

        executor.execute(target);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "database", target.name(),
                "message", "Theory setup completed."
        ));
    }
}
