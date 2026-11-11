package gr.aueb.cf.automation.queryrunner;

import java.util.HashMap;
import java.util.Map;

public class QueryExecutionContext {

    private final Map<String, Object> values =
            new HashMap<>();


    /* ========================================
       STORE VALUE
       ======================================== */

    public void put(
            String key,
            Object value
    ) {

        if (
                key == null ||
                        key.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Context key is required."
            );
        }

        values.put(
                key,
                value
        );
    }


    /* ========================================
       GET VALUE
       ======================================== */

    public Object get(
            String key
    ) {

        if (!values.containsKey(key)) {

            throw new IllegalArgumentException(
                    "Context value not found: "
                            + key
            );
        }

        return values.get(key);
    }


    /* ========================================
       GET LONG
       ======================================== */

    public Long getLong(
            String key
    ) {

        Object value =
                get(key);

        if (value instanceof Number number) {

            return number.longValue();
        }

        throw new IllegalArgumentException(
                "Context value is not numeric: "
                        + key
        );
    }


    /* ========================================
       GET STRING
       ======================================== */

    public String getString(
            String key
    ) {

        Object value =
                get(key);

        return String.valueOf(
                value
        );
    }


    /* ========================================
       HAS VALUE
       ======================================== */

    public boolean contains(
            String key
    ) {

        return values.containsKey(
                key
        );
    }


    /* ========================================
       CLEAR
       ======================================== */

    public void clear() {

        values.clear();
    }
}