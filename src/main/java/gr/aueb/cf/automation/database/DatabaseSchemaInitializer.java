package gr.aueb.cf.automation.database;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;


@Component
@Order(1)
public class DatabaseSchemaInitializer
        implements ApplicationRunner {

    private static final String SCHEMA_SCRIPT =
            "db/schema.sql";


    private final DataSource productionDataSource;

    private final DataSource frontendTestingDataSource;

    private final DataSource apiTestingDataSource;


    public DatabaseSchemaInitializer(
            @Qualifier("productionDataSource")
            DataSource productionDataSource,

            @Qualifier("frontendTestingDataSource")
            DataSource frontendTestingDataSource,

            @Qualifier("apiTestingDataSource")
            DataSource apiTestingDataSource
    ) {

        this.productionDataSource =
                productionDataSource;

        this.frontendTestingDataSource =
                frontendTestingDataSource;

        this.apiTestingDataSource =
                apiTestingDataSource;
    }


    /* ========================================
       APPLICATION STARTUP
       ======================================== */

    @Override
    public void run(
            ApplicationArguments args
    ) {

        Resource schemaResource =
                new ClassPathResource(
                        SCHEMA_SCRIPT
                );


        if (!schemaResource.exists()) {

            throw new IllegalStateException(
                    "Database schema script not found: "
                            + SCHEMA_SCRIPT
            );
        }


        initializeDatabase(
                "production",
                productionDataSource,
                schemaResource
        );


        initializeDatabase(
                "frontendtesting",
                frontendTestingDataSource,
                schemaResource
        );


        initializeDatabase(
                "apitesting",
                apiTestingDataSource,
                schemaResource
        );
    }


    /* ========================================
       INITIALIZE ONE DATABASE
       ======================================== */

    private void initializeDatabase(
            String databaseName,
            DataSource dataSource,
            Resource schemaResource
    ) {

        System.out.println(
                "Initializing database schema: "
                        + databaseName
        );


        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator();


        populator.addScript(
                schemaResource
        );


        /*
         * An ena SQL statement apotyxei,
         * stamataei to initialization.
         *
         * Den theloume na exoume
         * miso-ftiaxmeni database.
         */
        populator.setContinueOnError(
                false
        );


        /*
         * To schema.sql tha einai idempotent,
         * px CREATE TABLE IF NOT EXISTS.
         */
        DatabasePopulatorUtils.execute(
                populator,
                dataSource
        );


        System.out.println(
                "Database schema ready: "
                        + databaseName
        );
    }
}