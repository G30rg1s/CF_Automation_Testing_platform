package gr.aueb.cf.automation.database;

import gr.aueb.cf.automation.queryrunner.DatabaseTarget;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DatabaseRoutingDataSource
        extends AbstractRoutingDataSource
        implements ApplicationListener<ApplicationReadyEvent> {


    private volatile boolean applicationReady = false;


    /* ========================================
       CURRENT DATABASE
       ======================================== */

    @Override
    protected Object determineCurrentLookupKey() {

        DatabaseTarget databaseTarget =
                DatabaseContextHolder.get();


        if (databaseTarget != null) {

            return databaseTarget;
        }

        if (!applicationReady) {

            return DatabaseTarget.PRODUCTION;
        }

        throw new IllegalStateException(
                "No database has been selected."
        );
    }


    /* ========================================
       APPLICATION READY
       ======================================== */

    @Override
    public void onApplicationEvent(
            ApplicationReadyEvent event
    ) {

        applicationReady = true;
    }
}