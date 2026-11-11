
package gr.aueb.cf.automation.controller;

import gr.aueb.cf.automation.database.DatabaseContextHolder;
import gr.aueb.cf.automation.queryrunner.DatabaseTarget;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cf_automation/sql-execute")
@PreAuthorize("isAuthenticated()")
public class SqlExecuteController {

    private static final int MAX_ROWS = 200;
    private static final int QUERY_TIMEOUT_SECONDS = 15;

    private final JdbcTemplate jdbcTemplate;

    public SqlExecuteController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ========================================
    // REQUEST
    // ========================================

    public record SqlExecuteRequest(

            @NotBlank(message = "SQL query is required.")
            String sql

    ) {
    }

    // ========================================
    // RESPONSE
    // ========================================

    public record SqlExecuteResponse(

            boolean success,
            String message,
            String database,
            List<String> columns,
            List<Map<String, Object>> rows,
            int rowCount

    ) {
    }

    // ========================================
    // EXECUTE SQL
    // ========================================

    @PostMapping
    public SqlExecuteResponse execute(
            @Valid @RequestBody SqlExecuteRequest request
    ) {

        // Database selected from the Navbar.
        DatabaseTarget target =
                DatabaseContextHolder.require();

        String sql = request.sql() == null
                ? ""
                : request.sql().trim();

        if (sql.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "SQL query is required."
            );
        }

        try {

            // Explicit ConnectionCallback cast fixes
            // the ambiguous JdbcTemplate.execute() call.

            return jdbcTemplate.execute(
                    (ConnectionCallback<SqlExecuteResponse>) connection -> {

                        List<String> columns =
                                new ArrayList<>();

                        List<Map<String, Object>> rows =
                                new ArrayList<>();

                        try (Statement statement =
                                     connection.createStatement()) {

                            statement.setQueryTimeout(
                                    QUERY_TIMEOUT_SECONDS
                            );

                            statement.setMaxRows(MAX_ROWS);

                            // Executes a SQL statement.
                            //
                            // true  -> ResultSet
                            // false -> update count or no result

                            boolean hasResultSet =
                                    statement.execute(sql);

                            // ========================================
                            // SELECT / SHOW / DESCRIBE / EXPLAIN
                            // ========================================

                            if (hasResultSet) {

                                try (ResultSet resultSet =
                                             statement.getResultSet()) {

                                    ResultSetMetaData metadata =
                                            resultSet.getMetaData();

                                    int columnCount =
                                            metadata.getColumnCount();

                                    for (
                                            int index = 1;
                                            index <= columnCount;
                                            index++
                                    ) {

                                        columns.add(
                                                metadata.getColumnLabel(index)
                                        );
                                    }

                                    while (
                                            resultSet.next()
                                                    && rows.size() < MAX_ROWS
                                    ) {

                                        Map<String, Object> row =
                                                new LinkedHashMap<>();

                                        for (
                                                int index = 1;
                                                index <= columnCount;
                                                index++
                                        ) {

                                            row.put(
                                                    columns.get(index - 1),
                                                    resultSet.getObject(index)
                                            );
                                        }

                                        rows.add(row);
                                    }
                                }

                                return new SqlExecuteResponse(
                                        true,
                                        "Query executed successfully. "
                                                + rows.size()
                                                + " row(s) returned"
                                                + " (maximum "
                                                + MAX_ROWS
                                                + ").",
                                        target.name(),
                                        columns,
                                        rows,
                                        rows.size()
                                );
                            }

                            // ========================================
                            // INSERT / UPDATE / DELETE / DDL
                            // ========================================

                            int affectedRows =
                                    statement.getUpdateCount();

                            String message;

                            if (affectedRows >= 0) {

                                message =
                                        "SQL executed successfully. "
                                                + affectedRows
                                                + " row(s) affected.";

                            } else {

                                message =
                                        "SQL statement executed successfully.";
                            }

                            return new SqlExecuteResponse(
                                    true,
                                    message,
                                    target.name(),
                                    columns,
                                    rows,
                                    Math.max(affectedRows, 0)
                            );
                        }
                    }
            );

        } catch (Exception exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "SQL execution failed: "
                            + exception.getMessage(),
                    exception
            );
        }
    }
}
