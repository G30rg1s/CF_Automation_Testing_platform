package gr.aueb.cf.automation.queryrunner;

public class QueryStep {

    private Integer executionOrder;

    private final String name;

    private final String sql;

    /*
     * An den einai null,
     * to step perimenei generated key
     * apo INSERT kai tha tin apothikeusei
     * sto QueryExecutionContext me auto to onoma.
     *
     * Px:
     * "documentId"
     */
    private final String generatedKeyName;


    /* ========================================
       NORMAL QUERY - AUTO ORDER
       ======================================== */

    public QueryStep(
            String name,
            String sql
    ) {

        this(
                null,
                name,
                sql,
                null
        );
    }


    /* ========================================
       NORMAL QUERY - CUSTOM ORDER
       ======================================== */

    public QueryStep(
            Integer executionOrder,
            String name,
            String sql
    ) {

        this(
                executionOrder,
                name,
                sql,
                null
        );
    }


    /* ========================================
       GENERATED KEY QUERY - AUTO ORDER
       ======================================== */

    public QueryStep(
            String name,
            String sql,
            String generatedKeyName
    ) {

        this(
                null,
                name,
                sql,
                generatedKeyName
        );
    }


    /* ========================================
       FULL CONSTRUCTOR
       ======================================== */

    public QueryStep(
            Integer executionOrder,
            String name,
            String sql,
            String generatedKeyName
    ) {

        if (
                name == null ||
                        name.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Query step name is required."
            );
        }

        if (
                sql == null ||
                        sql.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "SQL query is required."
            );
        }


        this.executionOrder =
                executionOrder;

        this.name =
                name;

        this.sql =
                sql;

        this.generatedKeyName =
                normalizeGeneratedKeyName(
                        generatedKeyName
                );
    }


    /* ========================================
       EXECUTION ORDER
       ======================================== */

    public Integer getExecutionOrder() {

        return executionOrder;
    }


    public void setExecutionOrder(
            Integer executionOrder
    ) {

        if (
                executionOrder != null &&
                        executionOrder <= 0
        ) {

            throw new IllegalArgumentException(
                    "Execution order must be greater than 0."
            );
        }

        this.executionOrder =
                executionOrder;
    }


    /* ========================================
       NAME
       ======================================== */

    public String getName() {

        return name;
    }


    /* ========================================
       SQL
       ======================================== */

    public String getSql() {

        return sql;
    }


    /* ========================================
       GENERATED KEY
       ======================================== */

    public String getGeneratedKeyName() {

        return generatedKeyName;
    }


    public boolean hasGeneratedKey() {

        return generatedKeyName != null;
    }


    /* ========================================
       NORMALIZE GENERATED KEY NAME
       ======================================== */

    private String normalizeGeneratedKeyName(
            String generatedKeyName
    ) {

        if (generatedKeyName == null) {

            return null;
        }

        String normalized =
                generatedKeyName.trim();

        if (normalized.isEmpty()) {

            return null;
        }

        return normalized;
    }
}