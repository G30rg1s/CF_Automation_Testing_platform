package gr.aueb.cf.automation.database;

import gr.aueb.cf.automation.queryrunner.DatabaseTarget;

public final class DatabaseContextHolder {

    private static final ThreadLocal<DatabaseTarget> DATABASE_CONTEXT =
            new ThreadLocal<>();


    private DatabaseContextHolder() {
    }


    /* ========================================
       SET DATABASE
       ======================================== */

    public static void set(
            DatabaseTarget databaseTarget
    ) {

        if (databaseTarget == null) {

            throw new IllegalArgumentException(
                    "Database target is required."
            );
        }

        DATABASE_CONTEXT.set(
                databaseTarget
        );
    }


    /* ========================================
       GET DATABASE
       ======================================== */

    public static DatabaseTarget get() {

        return DATABASE_CONTEXT.get();
    }


    /* ========================================
       REQUIRE DATABASE
       ======================================== */

    public static DatabaseTarget require() {

        DatabaseTarget databaseTarget =
                DATABASE_CONTEXT.get();

        if (databaseTarget == null) {

            throw new IllegalStateException(
                    "No database has been selected."
            );
        }

        return databaseTarget;
    }


    /* ========================================
       CLEAR DATABASE
       ======================================== */

    public static void clear() {

        DATABASE_CONTEXT.remove();
    }
}