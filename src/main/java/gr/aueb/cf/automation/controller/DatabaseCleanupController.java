
package gr.aueb.cf.automation.controller;

import gr.aueb.cf.automation.database.DatabaseContextHolder;
import gr.aueb.cf.automation.queryrunner.DatabaseCleanupExecutor;
import gr.aueb.cf.automation.queryrunner.DatabaseTarget;
import gr.aueb.cf.automation.queryrunner.QueryExecutionContext;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@RestController
@RequestMapping("/cf_automation/database-cleanup")
public class DatabaseCleanupController {

    private final DatabaseCleanupExecutor cleanupExecutor;

    public DatabaseCleanupController(
            DatabaseCleanupExecutor cleanupExecutor
    ) {
        this.cleanupExecutor = cleanupExecutor;
    }

    public record CleanupRequest(
            @NotBlank(message = "Database is required.")
            String database,

            @NotBlank(message = "Confirmation is required.")
            String confirmation
    ) {
    }

    public record CleanupResponse(
            boolean success,
            String message,
            String database
    ) {
    }

    @PostMapping("/execute")
    public ResponseEntity<CleanupResponse> execute(
            @Valid @RequestBody CleanupRequest request,
            Authentication authentication
    ) {

        // ========================================
        // AUTHENTICATION
        // ========================================

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required."
            );
        }

        // ========================================
        // SELECTED DATABASE
        // ========================================

        final DatabaseTarget target;

        try {
            target = DatabaseContextHolder.require();

        } catch (IllegalStateException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No database has been selected.",
                    exception
            );
        }

        String selectedDatabase = target.name();

        // ========================================
        // VERIFY REQUEST
        // ========================================

        String requestedDatabase =
                normalizeDatabase(request.database());

        String confirmedDatabase =
                normalizeDatabase(request.confirmation());

        if (!selectedDatabase.equals(requestedDatabase)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Database selection mismatch."
            );
        }

        if (!selectedDatabase.equals(confirmedDatabase)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid database confirmation."
            );
        }

        // ========================================
        // EXECUTE DATABASE CLEANUP
        // ========================================

        try {

            QueryExecutionContext executionContext =
                    cleanupExecutor.execute(target);

            if (executionContext == null) {

                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Cleanup returned no execution result."
                );
            }

            // QueryBatchRunner propagates SQL exceptions.
            // Reaching this point means every batch step
            // completed without throwing an exception.

            return ResponseEntity.ok(
                    new CleanupResponse(
                            true,
                            "Database cleanup completed successfully for "
                                    + selectedDatabase + ".",
                            selectedDatabase
                    )
            );

        } catch (ResponseStatusException exception) {

            throw exception;

        } catch (Exception exception) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Database cleanup failed. " +
                            "Check the execution log before retrying.",
                    exception
            );
        }
    }

    // ========================================
    // NORMALIZE DATABASE NAME
    // ========================================

    private String normalizeDatabase(String value) {

        if (value == null) {
            return "";
        }

        String normalized = value
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace("-", "_")
                .replace(" ", "_");

        return switch (normalized) {

            case "FRONTENDTESTING" ->
                    "FRONTEND_TESTING";

            case "APITESTING" ->
                    "API_TESTING";

            default -> normalized;
        };
    }
}
