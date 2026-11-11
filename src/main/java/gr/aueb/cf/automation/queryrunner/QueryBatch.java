package gr.aueb.cf.automation.queryrunner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class QueryBatch {

    private final String name;

    private final List<QueryStep> steps =
            new ArrayList<>();


    /* ========================================
       CONSTRUCTOR
       ======================================== */

    public QueryBatch(
            String name
    ) {

        if (
                name == null ||
                        name.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Query batch name is required."
            );
        }

        this.name =
                name.trim();
    }


    /* ========================================
       ADD NORMAL QUERY
       AUTO ORDER
       ======================================== */

    public QueryBatch add(
            String stepName,
            String sql
    ) {

        int executionOrder =
                getNextAutomaticOrder();

        return add(
                executionOrder,
                stepName,
                sql
        );
    }


    /* ========================================
       ADD NORMAL QUERY
       CUSTOM ORDER
       ======================================== */

    public QueryBatch add(
            int executionOrder,
            String stepName,
            String sql
    ) {

        validateExecutionOrder(
                executionOrder
        );

        ensureOrderIsAvailable(
                executionOrder
        );


        QueryStep step =
                new QueryStep(
                        executionOrder,
                        stepName,
                        sql
                );


        steps.add(
                step
        );

        return this;
    }


    /* ========================================
       ADD GENERATED KEY QUERY
       AUTO ORDER
       ======================================== */

    public QueryBatch addWithGeneratedKey(
            String stepName,
            String sql,
            String generatedKeyName
    ) {

        int executionOrder =
                getNextAutomaticOrder();

        return addWithGeneratedKey(
                executionOrder,
                stepName,
                sql,
                generatedKeyName
        );
    }


    /* ========================================
       ADD GENERATED KEY QUERY
       CUSTOM ORDER
       ======================================== */

    public QueryBatch addWithGeneratedKey(
            int executionOrder,
            String stepName,
            String sql,
            String generatedKeyName
    ) {

        validateExecutionOrder(
                executionOrder
        );

        ensureOrderIsAvailable(
                executionOrder
        );


        if (
                generatedKeyName == null ||
                        generatedKeyName.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Generated key name is required."
            );
        }


        QueryStep step =
                new QueryStep(
                        executionOrder,
                        stepName,
                        sql,
                        generatedKeyName
                );


        steps.add(
                step
        );

        return this;
    }


    /* ========================================
       NEXT AUTOMATIC ORDER
       ======================================== */

    private int getNextAutomaticOrder() {

        int order = 1;

        while (containsOrder(order)) {

            order++;
        }

        return order;
    }


    /* ========================================
       CHECK ORDER
       ======================================== */

    private boolean containsOrder(
            int executionOrder
    ) {

        return steps.stream()
                .anyMatch(
                        step ->
                                step.getExecutionOrder()
                                        == executionOrder
                );
    }


    /* ========================================
       VALIDATE ORDER
       ======================================== */

    private void validateExecutionOrder(
            int executionOrder
    ) {

        if (executionOrder <= 0) {

            throw new IllegalArgumentException(
                    "Execution order must be greater than 0."
            );
        }
    }


    /* ========================================
       DUPLICATE ORDER PROTECTION
       ======================================== */

    private void ensureOrderIsAvailable(
            int executionOrder
    ) {

        if (containsOrder(executionOrder)) {

            throw new IllegalArgumentException(
                    "Execution order already exists: "
                            + executionOrder
            );
        }
    }


    /* ========================================
       ORDERED STEPS
       ======================================== */

    public List<QueryStep> getOrderedSteps() {

        return steps.stream()
                .sorted(
                        Comparator.comparingInt(
                                QueryStep::getExecutionOrder
                        )
                )
                .toList();
    }


    /* ========================================
       GETTERS
       ======================================== */

    public String getName() {

        return name;
    }


    public List<QueryStep> getSteps() {

        return List.copyOf(
                steps
        );
    }
}