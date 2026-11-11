
package gr.aueb.cf.automation.service;

import gr.aueb.cf.automation.entity.SavedSqlQuery;
import gr.aueb.cf.automation.repository.SavedSqlQueryRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SavedSqlQueryService {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_SQL_LENGTH = 1_000_000;

    private final SavedSqlQueryRepository repository;

    public SavedSqlQueryService(
            SavedSqlQueryRepository repository
    ) {
        this.repository = repository;
    }

    /* ========================================
       GET ALL SAVED QUERIES
       ======================================== */

    @Transactional(readOnly = true)
    public List<SavedSqlQuery> findAll(String owner) {

        validateOwner(owner);

        return repository.findByOwnerOrderByUpdatedAtDesc(
                owner
        );
    }

    /* ========================================
       SEARCH QUERIES BY TITLE
       ======================================== */

    @Transactional(readOnly = true)
    public List<SavedSqlQuery> searchByTitle(
            String owner,
            String title
    ) {

        validateOwner(owner);

        if (title == null || title.isBlank()) {
            return findAll(owner);
        }

        return repository
                .findByOwnerAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(
                        owner,
                        title.trim()
                );
    }

    /* ========================================
       GET SAVED QUERY BY ID
       ======================================== */

    @Transactional(readOnly = true)
    public SavedSqlQuery findById(
            Long id,
            String owner
    ) {

        validateOwner(owner);

        return repository.findByIdAndOwner(id, owner)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Saved SQL query not found."
                        )
                );
    }

    /* ========================================
       CREATE NEW SAVED QUERY
       ======================================== */

    @Transactional
    public SavedSqlQuery save(
            String title,
            String sqlText,
            String owner
    ) {

        validateOwner(owner);
        validateQuery(title, sqlText);

        SavedSqlQuery query = new SavedSqlQuery();

        query.setTitle(title.trim());

        // Store the complete SQL text unchanged.
        // Comments, newlines and blank lines are preserved.
        query.setSqlText(sqlText);

        query.setOwner(owner);

        return repository.save(query);
    }

    /* ========================================
       UPDATE EXISTING SAVED QUERY
       ======================================== */

    @Transactional
    public SavedSqlQuery update(
            Long id,
            String title,
            String sqlText,
            String owner
    ) {

        validateOwner(owner);
        validateQuery(title, sqlText);

        SavedSqlQuery query = findById(id, owner);

        query.setTitle(title.trim());

        // Preserve the original formatting and comments.
        query.setSqlText(sqlText);

        return repository.save(query);
    }

    /* ========================================
       DELETE SAVED QUERY
       ======================================== */

    @Transactional
    public void delete(
            Long id,
            String owner
    ) {

        validateOwner(owner);

        SavedSqlQuery query = findById(id, owner);

        // Deletes only the saved library entry.
        // Does NOT execute the stored SQL.
        repository.delete(query);
    }

    /* ========================================
       VALIDATE USER
       ======================================== */

    private void validateOwner(String owner) {

        if (owner == null || owner.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required."
            );
        }
    }

    /* ========================================
       VALIDATE QUERY CONTENT
       ======================================== */

    private void validateQuery(
            String title,
            String sqlText
    ) {

        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Query title is required."
            );
        }

        if (title.trim().length() > MAX_TITLE_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Query title must not exceed 200 characters."
            );
        }

        if (sqlText == null || sqlText.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "SQL content is required."
            );
        }

        if (sqlText.length() > MAX_SQL_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "SQL content exceeds the allowed size."
            );
        }
    }
}
