
package gr.aueb.cf.automation.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Configuration
public class SavedSqlQueriesSchemaInitializer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    SavedSqlQueriesSchemaInitializer.class
            );

    // ========================================
    // DEFAULT QUERY OWNER
    // ========================================

    private static final String DEFAULT_OWNER = "system";

    // ========================================
    // DEFAULT SAVED SQL QUERIES
    // ========================================

    private record DefaultQuery(
            String title,
            String sql
    ) {
    }

    private static final List<DefaultQuery> DEFAULT_QUERIES =
            List.of(

                    new DefaultQuery(
                            "DESCRIBE. Show Categories Table Structure",
                            "DESCRIBE categories;"
                    ),

                    new DefaultQuery(
                            "DESCRIBE. Show Documents Table Structure",
                            "DESCRIBE documents;"
                    ),

                    new DefaultQuery(
                            "DESCRIBE. Show Document Formats Table Structure",
                            "DESCRIBE document_formats;"
                    ),

                    new DefaultQuery(
                            "SELECT. Show Complete Database Schema",
                            """
                            SELECT
                                TABLE_NAME,
                                ORDINAL_POSITION,
                                COLUMN_NAME,
                                COLUMN_TYPE,
                                IS_NULLABLE,
                                COLUMN_KEY,
                                COLUMN_DEFAULT,
                                EXTRA
                            FROM information_schema.COLUMNS
                            WHERE TABLE_SCHEMA = DATABASE()
                            ORDER BY TABLE_NAME, ORDINAL_POSITION;
                            """
                    ),

                    new DefaultQuery(
                            "SELECT. Show All Database Foreign Keys",
                            """
                            SELECT
                                TABLE_NAME,
                                COLUMN_NAME,
                                REFERENCED_TABLE_NAME,
                                REFERENCED_COLUMN_NAME,
                                CONSTRAINT_NAME
                            FROM information_schema.KEY_COLUMN_USAGE
                            WHERE TABLE_SCHEMA = DATABASE()
                              AND REFERENCED_TABLE_NAME IS NOT NULL
                            ORDER BY TABLE_NAME, COLUMN_NAME;
                            """
                    )
            );

    // ========================================
    // APPLICATION RUNNER
    // ========================================

    @Bean
    public ApplicationRunner savedQueriesSchemaRunner(

            @Qualifier("productionDataSource")
            DataSource productionDataSource,

            @Qualifier("frontendTestingDataSource")
            DataSource frontendTestingDataSource,

            @Qualifier("apiTestingDataSource")
            DataSource apiTestingDataSource,

            @Value("${app.saved-queries.schema-init.enabled:true}")
            boolean enabled

    ) {

        return args -> {

            if (!enabled) {

                log.info(
                        "Saved SQL Queries initialization is disabled."
                );

                return;
            }

            initialize(
                    "PRODUCTION",
                    productionDataSource
            );

            initialize(
                    "FRONTEND_TESTING",
                    frontendTestingDataSource
            );

            initialize(
                    "API_TESTING",
                    apiTestingDataSource
            );

            log.info(
                    "Saved SQL Queries initialization completed."
            );
        };
    }

    // ========================================
    // INITIALIZE ONE DATABASE
    // ========================================

    private void initialize(
            String databaseName,
            DataSource dataSource
    ) {

        log.info(
                "Checking saved_sql_queries table in {}",
                databaseName
        );

        // ====================================
        // CREATE TABLE IF NEEDED
        // ====================================

        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator(
                        new ClassPathResource(
                                "db/create-saved-sql-queries.sql"
                        )
                );

        populator.setContinueOnError(false);
        populator.setIgnoreFailedDrops(false);

        // Physical DataSource for this database.
        // No Navbar or ThreadLocal routing.

        populator.execute(dataSource);

        log.info(
                "saved_sql_queries table ready in {}",
                databaseName
        );

        // ====================================
        // INSERT DEFAULT QUERIES
        // ====================================

        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(dataSource);

        int inserted = 0;
        int existing = 0;

        for (DefaultQuery query : DEFAULT_QUERIES) {

            // Check this query by title.
            // Never overwrite an existing saved query.

            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM saved_sql_queries
                    WHERE title = ?
                    """,
                    Integer.class,
                    query.title()
            );

            if (count != null && count > 0) {

                existing++;

                log.info(
                        "Default query already exists in {}: {}",
                        databaseName,
                        query.title()
                );

                continue;
            }

            // ====================================
            // STABLE UUID
            // ====================================

            // Identical UUID across all 3 databases,
            // compatible with synchronized saved queries.

            String queryUuid =
                    UUID.nameUUIDFromBytes(
                            (
                                    "coding-factory-default-sql:"
                                            + query.title()
                            ).getBytes(
                                    StandardCharsets.UTF_8
                            )
                    ).toString();

            // ====================================
            // INSERT
            // ====================================

            // Actual saved_sql_queries schema:
            //
            // id         AUTO_INCREMENT
            // query_uuid NOT NULL
            // title      NOT NULL
            // sql_text   NOT NULL
            // owner      NOT NULL
            // created_at NOT NULL
            // updated_at NOT NULL

            jdbcTemplate.update(
                    """
                    INSERT INTO saved_sql_queries (
                        query_uuid,
                        title,
                        sql_text,
                        owner,
                        created_at,
                        updated_at
                    )
                    VALUES (
                        ?, ?, ?, ?,
                        NOW(6),
                        NOW(6)
                    )
                    """,
                    queryUuid,
                    query.title(),
                    query.sql(),
                    DEFAULT_OWNER
            );

            inserted++;

            log.info(
                    "Inserted default SQL query in {}: {}",
                    databaseName,
                    query.title()
            );
        }

        // ====================================
        // SUMMARY
        // ====================================

        log.info(
                "Default SQL queries initialized in {}. " +
                        "Inserted: {}. Already existing: {}.",
                databaseName,
                inserted,
                existing
        );
    }
}
