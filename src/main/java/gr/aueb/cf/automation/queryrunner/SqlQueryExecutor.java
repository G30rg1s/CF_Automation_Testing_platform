
package gr.aueb.cf.automation.queryrunner;

import gr.aueb.cf.automation.database.DatabaseContextHolder;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SqlQueryExecutor {

    private static final int MAX_ROWS = 100;
    private static final int QUERY_TIMEOUT_SECONDS = 15;
    private static final int MAX_SQL_LENGTH = 10000;

    private final JdbcTemplate jdbcTemplate;

    public SqlQueryExecutor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ========================================
    // EXECUTE SQL
    // ========================================

    public QueryResult execute(String sql) {

        DatabaseTarget database =
                DatabaseContextHolder.require();

        String cleanedSql = prepareSql(sql);

        return jdbcTemplate.execute(
                (ConnectionCallback<QueryResult>) connection -> {

                    List<String> columns = new ArrayList<>();

                    List<Map<String, Object>> rows =
                            new ArrayList<>();

                    try (Statement statement =
                                 connection.createStatement()) {

                        statement.setQueryTimeout(
                                QUERY_TIMEOUT_SECONDS
                        );

                        statement.setMaxRows(MAX_ROWS);

                        boolean hasResultSet =
                                statement.execute(cleanedSql);

                        // ====================================
                        // SELECT / SHOW / DESCRIBE / EXPLAIN
                        // ====================================

                        if (hasResultSet) {

                            try (ResultSet resultSet =
                                         statement.getResultSet()) {

                                ResultSetMetaData metadata =
                                        resultSet.getMetaData();

                                int columnCount =
                                        metadata.getColumnCount();

                                for (int i = 1;
                                     i <= columnCount;
                                     i++) {

                                    columns.add(
                                            metadata.getColumnLabel(i)
                                    );
                                }

                                while (
                                        resultSet.next()
                                                && rows.size() < MAX_ROWS
                                ) {

                                    Map<String, Object> row =
                                            new LinkedHashMap<>();

                                    for (int i = 1;
                                         i <= columnCount;
                                         i++) {

                                        row.put(
                                                columns.get(i - 1),
                                                resultSet.getObject(i)
                                        );
                                    }

                                    rows.add(row);
                                }
                            }

                            return new QueryResult(
                                    database.name(),
                                    columns,
                                    rows.size(),
                                    rows,
                                    MAX_ROWS
                            );
                        }

                        // ====================================
                        // INSERT / UPDATE / DELETE / DDL
                        // ====================================

                        int affectedRows =
                                statement.getUpdateCount();

                        return new QueryResult(
                                database.name(),
                                columns,
                                Math.max(affectedRows, 0),
                                rows,
                                MAX_ROWS
                        );
                    }
                }
        );
    }

    // ========================================
    // BACKWARD COMPATIBILITY
    // ========================================

    /**
     * Preserves existing callers of executeSelect().
     *
     * For new code, prefer execute().
     */
    public QueryResult executeSelect(String sql) {
        return execute(sql);
    }

    // ========================================
    // PREPARE SQL
    // ========================================

    private String prepareSql(String sql) {

        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException(
                    "SQL query cannot be empty."
            );
        }

        if (sql.length() > MAX_SQL_LENGTH) {
            throw new IllegalArgumentException(
                    "SQL query is too long."
            );
        }

        String[] lines = sql.split("\\R", -1);

        StringBuilder builder = new StringBuilder();

        boolean sqlStarted = false;

        for (String line : lines) {

            String trimmed = line.trim();

            // Skip optional description comments
            // before the SQL statement.

            if (!sqlStarted) {

                if (
                        trimmed.isEmpty() ||
                                trimmed.startsWith("//") ||
                                trimmed.startsWith("--")
                ) {
                    continue;
                }

                sqlStarted = true;
            }

            // Custom // comments are not SQL syntax.

            if (trimmed.startsWith("//")) {
                throw new IllegalArgumentException(
                        "Place // comments before the SQL statement."
                );
            }

            builder.append(line).append("\n");
        }

        String cleanedSql =
                builder.toString().trim();

        if (cleanedSql.isEmpty()) {
            throw new IllegalArgumentException(
                    "SQL statement cannot be empty."
            );
        }

        // Remove an optional trailing semicolon.
        // The JDBC driver executes the statement.

        if (cleanedSql.endsWith(";")) {

            cleanedSql = cleanedSql
                    .substring(0, cleanedSql.length() - 1)
                    .trim();
        }

        if (cleanedSql.isEmpty()) {
            throw new IllegalArgumentException(
                    "SQL statement cannot be empty."
            );
        }

        return cleanedSql;
    }

    // ========================================
    // QUERY RESULT
    // ========================================

    public record QueryResult(
            String database,
            List<String> columns,
            int rowCount,
            List<Map<String, Object>> rows,
            int maxRows
    ) {
    }
}
