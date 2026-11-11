
package gr.aueb.cf.automation.queryrunner;

import org.springframework.stereotype.Service;

@Service
public class DatabaseCleanupExecutor {

    private final QueryBatchRunner queryBatchRunner;

    public DatabaseCleanupExecutor(QueryBatchRunner queryBatchRunner) {
        this.queryBatchRunner = queryBatchRunner;
    }

    public QueryExecutionContext execute(DatabaseTarget target) {

        if (target == null) {
            throw new IllegalArgumentException(
                    "Database target is required."
            );
        }

        QueryBatch batch = new QueryBatch("Clean Database");

        // STEP 1: Delete all document formatting records.
        // Must run before deleting documents due to foreign keys.
        batch.add(
                "Delete all document formats",
                "DELETE FROM document_formats"
        );

        // STEP 2: Delete all documents.
        batch.add(
                "Delete all documents",
                "DELETE FROM documents"
        );

        // STEP 3: Delete categories except ID 1 (Διάφορα).
        batch.add(
                "Delete categories except Διάφορα",
                "DELETE FROM categories WHERE id <> 1"
        );

        // STEP 4: Reset formatting AUTO_INCREMENT to 1.
        batch.add(
                "Reset document formats AUTO_INCREMENT",
                "ALTER TABLE document_formats AUTO_INCREMENT = 1"
        );

        // STEP 5: Reset documents AUTO_INCREMENT to 1.
        batch.add(
                "Reset documents AUTO_INCREMENT",
                "ALTER TABLE documents AUTO_INCREMENT = 1"
        );

        // STEP 6: Keep category 1 and start new IDs at 2.
        batch.add(
                "Reset categories AUTO_INCREMENT",
                "ALTER TABLE categories AUTO_INCREMENT = 2"
        );

        return queryBatchRunner.run(batch, target);
    }
}
