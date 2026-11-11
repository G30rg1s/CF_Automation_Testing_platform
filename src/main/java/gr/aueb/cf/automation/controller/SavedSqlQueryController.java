
package gr.aueb.cf.automation.controller;

import gr.aueb.cf.automation.entity.SavedSqlQuery;
import gr.aueb.cf.automation.repository.SavedSqlQueryRepository;
import gr.aueb.cf.automation.service.SavedSqlQueryService;
import gr.aueb.cf.automation.service.SavedSqlQuerySyncService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cf_automation/saved-queries")
@PreAuthorize("isAuthenticated()")
public class SavedSqlQueryController {

    private static final String SYSTEM_OWNER = "system";

    private final SavedSqlQueryService service;
    private final SavedSqlQuerySyncService syncService;
    private final SavedSqlQueryRepository repository;

    public SavedSqlQueryController(
            SavedSqlQueryService service,
            SavedSqlQuerySyncService syncService,
            SavedSqlQueryRepository repository
    ) {
        this.service = service;
        this.syncService = syncService;
        this.repository = repository;
    }

    // ========================================
    // REQUEST DTO
    // ========================================

    public record SaveQueryRequest(

            @NotBlank(message = "Query title is required.")
            @Size(
                    max = 200,
                    message = "Title must not exceed 200 characters."
            )
            String title,

            @NotBlank(message = "SQL content is required.")
            String sqlText

    ) {
    }

    // ========================================
    // QUERY RESPONSE DTO
    // ========================================

    public record SavedQueryResponse(
            Long id,
            String queryUuid,
            String title,
            String sqlText,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {

        public static SavedQueryResponse from(
                SavedSqlQuery query
        ) {

            return new SavedQueryResponse(
                    query.getId(),
                    query.getQueryUuid(),
                    query.getTitle(),
                    query.getSqlText(),
                    query.getCreatedAt(),
                    query.getUpdatedAt()
            );
        }
    }

    // ========================================
    // CURRENT AUTHENTICATED USER
    // ========================================

    private String currentOwner(
            Authentication authentication
    ) {

        if (
                authentication == null ||
                        !authentication.isAuthenticated() ||
                        "anonymousUser".equals(
                                authentication.getName()
                        )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required."
            );
        }

        return authentication.getName();
    }

    // ========================================
    // GET ALL / SEARCH BY TITLE
    // ========================================

    @GetMapping
    public List<SavedQueryResponse> findAll(

            @RequestParam(
                    name = "title",
                    required = false
            )
            String title,

            Authentication authentication
    ) {

        String owner = currentOwner(authentication);

        // Include both personal saved queries
        // and default queries owned by "system".

        List<SavedSqlQuery> personalQueries;
        List<SavedSqlQuery> systemQueries;

        if (title == null || title.isBlank()) {

            personalQueries = service.findAll(owner);

            systemQueries = SYSTEM_OWNER.equals(owner)
                    ? List.of()
                    : service.findAll(SYSTEM_OWNER);

        } else {

            personalQueries = service.searchByTitle(
                    owner,
                    title
            );

            systemQueries = SYSTEM_OWNER.equals(owner)
                    ? List.of()
                    : service.searchByTitle(
                    SYSTEM_OWNER,
                    title
            );
        }

        // Avoid duplicate UUIDs while preserving order.

        Map<String, SavedSqlQuery> combined =
                new LinkedHashMap<>();

        for (SavedSqlQuery query : personalQueries) {

            combined.put(
                    query.getQueryUuid(),
                    query
            );
        }

        for (SavedSqlQuery query : systemQueries) {

            combined.putIfAbsent(
                    query.getQueryUuid(),
                    query
            );
        }

        return combined.values()
                .stream()
                .map(SavedQueryResponse::from)
                .toList();
    }

    // ========================================
    // GET QUERY BY UUID
    // ========================================

    @GetMapping("/{queryUuid}")
    public SavedQueryResponse findByUuid(
            @PathVariable String queryUuid,
            Authentication authentication
    ) {

        String owner = currentOwner(authentication);

        // First look for a personal query.

        SavedSqlQuery query = repository
                .findByQueryUuidAndOwner(
                        queryUuid,
                        owner
                )
                .orElseGet(() -> repository
                        .findByQueryUuidAndOwner(
                                queryUuid,
                                SYSTEM_OWNER
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Saved query not found."
                                )
                        )
                );

        return SavedQueryResponse.from(query);
    }

    // ========================================
    // CREATE QUERY IN ALL 3 DATABASES
    // ========================================

    @PostMapping
    public ResponseEntity<SavedSqlQuerySyncService.SyncResult> create(

            @Valid
            @RequestBody SaveQueryRequest request,

            Authentication authentication
    ) {

        String owner = currentOwner(authentication);

        // Reserve the system owner for startup queries.

        requirePersonalOwner(owner);

        SavedSqlQuerySyncService.SyncResult result =
                syncService.save(
                        request.title(),
                        request.sqlText(),
                        owner
                );

        return buildSyncResponse(
                result,
                HttpStatus.CREATED
        );
    }

    // ========================================
    // UPDATE PERSONAL QUERY IN ALL 3 DATABASES
    // ========================================

    @PutMapping("/{queryUuid}")
    public ResponseEntity<SavedSqlQuerySyncService.SyncResult> update(

            @PathVariable String queryUuid,

            @Valid
            @RequestBody SaveQueryRequest request,

            Authentication authentication
    ) {

        String owner = currentOwner(authentication);

        requirePersonalOwner(owner);

        // Only the current user's own queries
        // can be updated through this endpoint.

        SavedSqlQuerySyncService.SyncResult result =
                syncService.update(
                        queryUuid,
                        request.title(),
                        request.sqlText(),
                        owner
                );

        return buildSyncResponse(
                result,
                HttpStatus.OK
        );
    }

    // ========================================
    // DELETE PERSONAL QUERY FROM ALL 3 DATABASES
    // ========================================

    @DeleteMapping("/{queryUuid}")
    public ResponseEntity<SavedSqlQuerySyncService.SyncResult> delete(

            @PathVariable String queryUuid,
            Authentication authentication
    ) {

        String owner = currentOwner(authentication);

        requirePersonalOwner(owner);

        // System queries are not owned by the
        // logged-in user and must not be deleted.

        SavedSqlQuerySyncService.SyncResult result =
                syncService.delete(
                        queryUuid,
                        owner
                );

        return buildSyncResponse(
                result,
                HttpStatus.OK
        );
    }

    // ========================================
    // RESERVE SYSTEM OWNER
    // ========================================

    private void requirePersonalOwner(String owner) {

        if (SYSTEM_OWNER.equals(owner)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "System queries are managed by initialization."
            );
        }
    }

    // ========================================
    // BUILD SYNC RESPONSE
    // ========================================

    private ResponseEntity<SavedSqlQuerySyncService.SyncResult>
    buildSyncResponse(
            SavedSqlQuerySyncService.SyncResult result,
            HttpStatus successStatus
    ) {

        if (result.success()) {

            return ResponseEntity
                    .status(successStatus)
                    .body(result);
        }

        return ResponseEntity
                .status(HttpStatus.MULTI_STATUS)
                .body(result);
    }
}
