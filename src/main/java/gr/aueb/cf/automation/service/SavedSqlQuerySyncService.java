
package gr.aueb.cf.automation.service;

import gr.aueb.cf.automation.queryrunner.DatabaseTarget;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SavedSqlQuerySyncService {

    private static final String SYSTEM_OWNER = "system";

    private final JdbcTemplate productionJdbc;
    private final JdbcTemplate frontendTestingJdbc;
    private final JdbcTemplate apiTestingJdbc;

    private final TransactionTemplate productionTransaction;
    private final TransactionTemplate frontendTestingTransaction;
    private final TransactionTemplate apiTestingTransaction;

    public SavedSqlQuerySyncService(
            @Qualifier("productionDataSource")
            DataSource productionDataSource,

            @Qualifier("frontendTestingDataSource")
            DataSource frontendTestingDataSource,

            @Qualifier("apiTestingDataSource")
            DataSource apiTestingDataSource
    ) {

        this.productionJdbc =
                new JdbcTemplate(productionDataSource);

        this.frontendTestingJdbc =
                new JdbcTemplate(frontendTestingDataSource);

        this.apiTestingJdbc =
                new JdbcTemplate(apiTestingDataSource);

        this.productionTransaction =
                new TransactionTemplate(
                        new DataSourceTransactionManager(
                                productionDataSource
                        )
                );

        this.frontendTestingTransaction =
                new TransactionTemplate(
                        new DataSourceTransactionManager(
                                frontendTestingDataSource
                        )
                );

        this.apiTestingTransaction =
                new TransactionTemplate(
                        new DataSourceTransactionManager(
                                apiTestingDataSource
                        )
                );
    }

    // ========================================
    // SAVE TO ALL DATABASES
    // ========================================

    public SyncResult save(
            String title,
            String sqlText,
            String owner
    ) {

        validate(title, sqlText, owner);
        requirePersonalOwner(owner);

        String queryUuid = UUID.randomUUID().toString();

        return saveWithUuid(
                queryUuid,
                title,
                sqlText,
                owner
        );
    }

    // ========================================
    // SAVE WITH SHARED UUID
    // ========================================

    public SyncResult saveWithUuid(
            String queryUuid,
            String title,
            String sqlText,
            String owner
    ) {

        validateUuid(queryUuid);
        validate(title, sqlText, owner);
        requirePersonalOwner(owner);

        return executeAcrossDatabases(
                queryUuid,
                jdbc -> {

                    // INSERT only.
                    //
                    // If the UUID already exists,
                    // the unique constraint rejects the INSERT.
                    //
                    // An existing query is never overwritten,
                    // regardless of its owner.

                    jdbc.update(
                            """
                            INSERT INTO saved_sql_queries
                            (
                                query_uuid,
                                title,
                                sql_text,
                                owner,
                                created_at,
                                updated_at
                            )
                            VALUES
                                (?, ?, ?, ?, NOW(6), NOW(6))
                            """,
                            queryUuid,
                            title.trim(),
                            sqlText,
                            owner
                    );
                }
        );
    }

    // ========================================
    // UPDATE IN ALL DATABASES
    // ========================================

    public SyncResult update(
            String queryUuid,
            String title,
            String sqlText,
            String owner
    ) {

        validateUuid(queryUuid);
        validate(title, sqlText, owner);
        requirePersonalOwner(owner);

        return executeAcrossDatabases(
                queryUuid,
                jdbc -> {

                    int rows = jdbc.update(
                            """
                            UPDATE saved_sql_queries
                            SET
                                title = ?,
                                sql_text = ?,
                                updated_at = NOW(6)
                            WHERE query_uuid = ?
                              AND owner = ?
                            """,
                            title.trim(),
                            sqlText,
                            queryUuid,
                            owner
                    );

                    if (rows == 0) {
                        throw new IllegalStateException(
                                "Saved query was not found or " +
                                        "does not belong to this user."
                        );
                    }
                }
        );
    }

    // ========================================
    // DELETE FROM ALL DATABASES
    // ========================================

    public SyncResult delete(
            String queryUuid,
            String owner
    ) {

        validateUuid(queryUuid);
        validateOwner(owner);
        requirePersonalOwner(owner);

        return executeAcrossDatabases(
                queryUuid,
                jdbc -> {

                    jdbc.update(
                            """
                            DELETE FROM saved_sql_queries
                            WHERE query_uuid = ?
                              AND owner = ?
                            """,
                            queryUuid,
                            owner
                    );
                }
        );
    }

    // ========================================
    // EXECUTE ACROSS ALL DATABASES
    // ========================================

    private SyncResult executeAcrossDatabases(
            String queryUuid,
            DatabaseOperation operation
    ) {

        List<DatabaseTarget> succeeded =
                new ArrayList<>();

        List<DatabaseFailure> failed =
                new ArrayList<>();

        runForDatabase(
                DatabaseTarget.PRODUCTION,
                productionJdbc,
                productionTransaction,
                operation,
                succeeded,
                failed
        );

        runForDatabase(
                DatabaseTarget.FRONTEND_TESTING,
                frontendTestingJdbc,
                frontendTestingTransaction,
                operation,
                succeeded,
                failed
        );

        runForDatabase(
                DatabaseTarget.API_TESTING,
                apiTestingJdbc,
                apiTestingTransaction,
                operation,
                succeeded,
                failed
        );

        return new SyncResult(
                queryUuid,
                failed.isEmpty(),
                List.copyOf(succeeded),
                List.copyOf(failed)
        );
    }

    // ========================================
    // INDIVIDUAL DATABASE TRANSACTION
    // ========================================

    private void runForDatabase(
            DatabaseTarget target,
            JdbcTemplate jdbc,
            TransactionTemplate transaction,
            DatabaseOperation operation,
            List<DatabaseTarget> succeeded,
            List<DatabaseFailure> failed
    ) {

        try {

            transaction.executeWithoutResult(
                    status -> operation.execute(jdbc)
            );

            succeeded.add(target);

        } catch (Exception exception) {

            failed.add(
                    new DatabaseFailure(
                            target,
                            "Operation failed on " + target
                    )
            );
        }
    }

    // ========================================
    // VALIDATION
    // ========================================

    private void validate(
            String title,
            String sqlText,
            String owner
    ) {

        validateOwner(owner);

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Query title is required."
            );
        }

        if (title.trim().length() > 200) {
            throw new IllegalArgumentException(
                    "Query title is too long."
            );
        }

        if (sqlText == null || sqlText.isBlank()) {
            throw new IllegalArgumentException(
                    "SQL content is required."
            );
        }

        if (sqlText.length() > 1_000_000) {
            throw new IllegalArgumentException(
                    "SQL content is too large."
            );
        }
    }

    private void validateOwner(String owner) {

        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException(
                    "Query owner is required."
            );
        }
    }

    // ========================================
    // PROTECT SYSTEM QUERIES
    // ========================================

    private void requirePersonalOwner(String owner) {

        if (SYSTEM_OWNER.equals(owner)) {
            throw new IllegalArgumentException(
                    "System queries can only be managed " +
                            "by the database initializer."
            );
        }
    }

    private void validateUuid(String queryUuid) {

        try {
            UUID.fromString(queryUuid);
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Invalid query UUID."
            );
        }
    }

    // ========================================
    // DATABASE OPERATION
    // ========================================

    @FunctionalInterface
    private interface DatabaseOperation {

        void execute(JdbcTemplate jdbc);
    }

    // ========================================
    // DATABASE FAILURE
    // ========================================

    public record DatabaseFailure(
            DatabaseTarget database,
            String message
    ) {
    }

    // ========================================
    // SYNC RESULT
    // ========================================

    public record SyncResult(
            String queryUuid,
            boolean success,
            List<DatabaseTarget> succeeded,
            List<DatabaseFailure> failed
    ) {
    }
}
