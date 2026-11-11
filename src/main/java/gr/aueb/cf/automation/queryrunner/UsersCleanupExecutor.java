
package gr.aueb.cf.automation.queryrunner;

import org.springframework.stereotype.Service;

@Service
public class UsersCleanupExecutor {

    private final QueryBatchRunner queryBatchRunner;

    public UsersCleanupExecutor(QueryBatchRunner queryBatchRunner) {
        this.queryBatchRunner = queryBatchRunner;
    }

    public QueryExecutionContext execute(DatabaseTarget target) {

        if (target == null) {
            throw new IllegalArgumentException(
                    "Database target is required."
            );
        }

        QueryBatch batch = new QueryBatch("Clean Users");

        // STEP 1: Delete all users.
        batch.add(
                "Delete all users",
                "DELETE FROM users"
        );

        // STEP 2: Reset AUTO_INCREMENT.
        batch.add(
                "Reset users AUTO_INCREMENT",
                "ALTER TABLE users AUTO_INCREMENT = 1"
        );

        return queryBatchRunner.run(batch, target);
    }
}
