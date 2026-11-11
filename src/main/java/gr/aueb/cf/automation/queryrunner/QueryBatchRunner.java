package gr.aueb.cf.automation.queryrunner;

import gr.aueb.cf.automation.database.DatabaseContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.Statement;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QueryBatchRunner {

    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile(
                    "\\{\\{([a-zA-Z0-9_]+)}}"
            );


    private final JdbcTemplate jdbcTemplate;


    public QueryBatchRunner(
            JdbcTemplate jdbcTemplate
    ) {

        this.jdbcTemplate =
                jdbcTemplate;
    }


    /* ========================================
       RUN BATCH
       ======================================== */

    public QueryExecutionContext run(
            QueryBatch batch,
            DatabaseTarget databaseTarget
    ) {

        if (batch == null) {

            throw new IllegalArgumentException(
                    "Query batch is required."
            );
        }

        if (databaseTarget == null) {

            throw new IllegalArgumentException(
                    "Database target is required."
            );
        }


        DatabaseTarget previousTarget =
                DatabaseContextHolder.get();


        /*
         * An yparxei idi database sto current request
         * kai einai diaforetiki apo afti pou zitame,
         * stamataei.
         */
        if (
                previousTarget != null &&
                        previousTarget != databaseTarget
        ) {

            throw new IllegalStateException(
                    "Database target mismatch."
            );
        }


        QueryExecutionContext context =
                new QueryExecutionContext();


        try {

            /* ========================================
               SELECT DATABASE
               ======================================== */

            DatabaseContextHolder.set(
                    databaseTarget
            );


            System.out.println(
                    "Running batch: "
                            + batch.getName()
                            + " on "
                            + databaseTarget
            );


            /* ========================================
               EXECUTE STEPS IN ORDER
               ======================================== */

            for (
                    QueryStep step :
                    batch.getOrderedSteps()
            ) {

                executeStep(
                        step,
                        context
                );
            }


            return context;

        } finally {

            /*
             * An ypirxe database context prin ton runner,
             * ton epanaferoume.
             *
             * Alliws ton katharizoume.
             */
            if (previousTarget != null) {

                DatabaseContextHolder.set(
                        previousTarget
                );

            } else {

                DatabaseContextHolder.clear();
            }
        }
    }


    /* ========================================
       EXECUTE STEP
       ======================================== */

    private void executeStep(
            QueryStep step,
            QueryExecutionContext context
    ) {

        if (step == null) {

            throw new IllegalArgumentException(
                    "Query step is required."
            );
        }


        /* ========================================
           RESOLVE PLACEHOLDERS
           ======================================== */

        String resolvedSql =
                resolveSql(
                        step.getSql(),
                        context
                );


        System.out.println(
                "Running query "
                        + step.getExecutionOrder()
                        + " - "
                        + step.getName()
                        + " on "
                        + DatabaseContextHolder.require()
        );


        /* ========================================
           GENERATED KEY QUERY
           ======================================== */

        if (step.hasGeneratedKey()) {

            executeGeneratedKeyStep(
                    step,
                    resolvedSql,
                    context
            );

            return;
        }


        /* ========================================
           NORMAL QUERY
           ======================================== */

        jdbcTemplate.execute(
                resolvedSql
        );
    }


    /* ========================================
       EXECUTE GENERATED KEY STEP
       ======================================== */

    private void executeGeneratedKeyStep(
            QueryStep step,
            String sql,
            QueryExecutionContext context
    ) {

        KeyHolder keyHolder =
                new GeneratedKeyHolder();


        jdbcTemplate.update(
                connection ->
                        connection.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        ),
                keyHolder
        );


        Number generatedKey =
                keyHolder.getKey();


        if (generatedKey == null) {

            throw new IllegalStateException(
                    "No generated key returned for step: "
                            + step.getName()
            );
        }


        Long generatedId =
                generatedKey.longValue();


        context.put(
                step.getGeneratedKeyName(),
                generatedId
        );


        System.out.println(
                "Generated key stored: "
                        + step.getGeneratedKeyName()
                        + " = "
                        + generatedId
        );
    }


    /* ========================================
       RESOLVE SQL PLACEHOLDERS
       ======================================== */

    private String resolveSql(
            String sql,
            QueryExecutionContext context
    ) {

        Objects.requireNonNull(
                sql,
                "SQL query is required."
        );


        Matcher matcher =
                PLACEHOLDER_PATTERN.matcher(
                        sql
                );


        StringBuffer resolvedSql =
                new StringBuffer();


        while (matcher.find()) {

            String key =
                    matcher.group(1);


            if (!context.contains(key)) {

                throw new IllegalStateException(
                        "Query placeholder value not found: "
                                + key
                );
            }


            Object value =
                    context.get(key);


            /*
             * Pros to paron ta placeholders
             * epitrepetai na einai mono numeric IDs.
             */
            if (!(value instanceof Number)) {

                throw new IllegalStateException(
                        "Only numeric query placeholders are currently supported: "
                                + key
                );
            }


            matcher.appendReplacement(
                    resolvedSql,
                    Matcher.quoteReplacement(
                            String.valueOf(value)
                    )
            );
        }


        matcher.appendTail(
                resolvedSql
        );


        return resolvedSql.toString();
    }
}