
document.addEventListener("DOMContentLoaded", () => {
    "use strict";

    // ========================================
    // SQL QUERY CONSOLE
    // ========================================

    const API_URL = "/cf_automation/saved-queries";
    const SQL_EXECUTE_URL = "/cf_automation/sql-execute";

    // ========================================
    // MAIN ELEMENTS
    // ========================================

    const openButton =
        document.getElementById("openSqlConsoleButton");

    const consoleModal =
        document.getElementById("sqlQueryConsoleModal");

    const closeConsoleButton =
        document.getElementById("closeSqlConsoleButton");

    const closeConsoleFooterButton =
        document.getElementById("closeSqlConsoleFooterButton");

    const searchInput =
        document.getElementById("sqlQuerySearch");

    const refreshButton =
        document.getElementById("refreshSqlQueriesButton");

    const newQueryButton =
        document.getElementById("newSqlQueryButton");

    const queryList =
        document.getElementById("sqlQueryList");

    const emptyState =
        document.getElementById("sqlQueryEmptyState");

    const libraryStatus =
        document.getElementById("sqlQueryLibraryStatus");

    const queryCount =
        document.getElementById("sqlQueryCount");

    // ========================================
    // EDITOR ELEMENTS
    // ========================================

    const editorModal =
        document.getElementById("sqlQueryEditorModal");

    const editorTitle =
        document.getElementById("sqlQueryEditorTitle");

    const editorForm =
        document.getElementById("sqlQueryEditorForm");

    const editorUuid =
        document.getElementById("sqlQueryEditorUuid");

    const titleInput =
        document.getElementById("sqlQueryTitle");

    const sqlTextInput =
        document.getElementById("sqlQueryText");

    const commandSelector =
        document.getElementById("sqlQueryCommandTemplate");

    // ========================================
    // SQL TITLE COMMANDS
    // ========================================

    const SQL_TITLE_COMMANDS = [
        "SELECT",
        "INSERT",
        "UPDATE",
        "DELETE",
        "REPLACE",
        "CREATE",
        "ALTER",
        "DROP",
        "TRUNCATE",
        "RENAME",
        "SHOW",
        "DESCRIBE",
        "EXPLAIN"
    ];

    const titlePrefixPattern = new RegExp(
        "^(?:" + SQL_TITLE_COMMANDS.join("|") + ")\\s*\\.\\s*",
        "i"
    );

    function getExistingTitleCommand(title) {
        const match = String(title || "").match(
            titlePrefixPattern
        );

        return match
            ? match[0].split(".")[0].trim().toUpperCase()
            : "";
    }

    // ========================================
    // APPLY UPPERCASE TITLE PREFIX
    // ========================================

    function applySqlTitlePrefix() {
        if (!commandSelector || !commandSelector.value) {
            return;
        }

        const description = titleInput.value
            .replace(titlePrefixPattern, "")
            .trim();

        titleInput.value =
            commandSelector.value.toUpperCase() +
            "." +
            description;
    }

    // ========================================
    // SELECT COMMAND WHEN EDITING
    // ========================================

    function selectCommandForExistingQuery(query) {
        if (!commandSelector) {
            return;
        }

        const fromTitle =
            getExistingTitleCommand(query.title);

        const fromSql = String(query.sqlText || "")
            .replace(
                /^(?:\s|--[^\r\n]*(?:\r?\n|$)|\/\*[\s\S]*?\*\/)*/g,
                ""
            )
            .match(/^([a-zA-Z]+)/)?.[1]?.toUpperCase() || "";

        const command = fromTitle || fromSql;

        commandSelector.value =
            SQL_TITLE_COMMANDS.includes(command)
                ? command
                : "";

        if (commandSelector.value) {
            applySqlTitlePrefix();
        }
    }

    const detectedCount =
        document.getElementById("sqlQueryDetectedCount");

    const previewList =
        document.getElementById("sqlQueryPreviewList");

    const editorStatus =
        document.getElementById("sqlQueryEditorStatus");

    const saveButton =
        document.getElementById("saveSqlQueryButton");

    const closeEditorButton =
        document.getElementById("closeSqlQueryEditorButton");

    const cancelEditorButton =
        document.getElementById("cancelSqlQueryEditorButton");

    // ========================================
    // DELETE ELEMENTS
    // ========================================

    const deleteModal =
        document.getElementById("sqlQueryDeleteModal");

    const deleteName =
        document.getElementById("sqlQueryDeleteName");

    const deleteStatus =
        document.getElementById("sqlQueryDeleteStatus");

    const confirmDeleteButton =
        document.getElementById("confirmSqlQueryDeleteButton");

    const cancelDeleteButton =
        document.getElementById("cancelSqlQueryDeleteButton");

    const closeDeleteButton =
        document.getElementById("closeSqlQueryDeleteButton");

    // ========================================
    // VERIFY ELEMENTS
    // ========================================

    const requiredElements = {
        openButton,
        consoleModal,
        closeConsoleButton,
        closeConsoleFooterButton,
        searchInput,
        refreshButton,
        newQueryButton,
        queryList,
        emptyState,
        libraryStatus,
        queryCount,
        editorModal,
        editorTitle,
        editorForm,
        editorUuid,
        titleInput,
        sqlTextInput,
        detectedCount,
        previewList,
        editorStatus,
        saveButton,
        closeEditorButton,
        cancelEditorButton,
        deleteModal,
        deleteName,
        deleteStatus,
        confirmDeleteButton,
        cancelDeleteButton,
        closeDeleteButton
    };

    const missingElements = Object.entries(requiredElements)
        .filter(([, element]) => !element)
        .map(([name]) => name);

    if (missingElements.length > 0) {
        console.error(
            "SQL Query Console: Missing elements:",
            missingElements
        );

        return;
    }

    // ========================================
    // STATE
    // ========================================

    let savedQueries = [];
    let selectedDeleteQuery = null;
    let busy = false;
    let loading = false;

    const runningQueries = new Set();
    const focusHistory = new Map();

    const originalBodyOverflow =
        document.body.style.overflow;

    // ========================================
    // STATUS
    // ========================================

    function setStatus(
        element,
        message,
        status = "info"
    ) {
        element.textContent = message;
        element.dataset.status = status;
    }

    function setBusy(value) {
        busy = value;

        saveButton.disabled = value;
        confirmDeleteButton.disabled = value;
        refreshButton.disabled = value || loading;
        newQueryButton.disabled = value;
    }

    // ========================================
    // MODAL HELPERS
    // ========================================

    function isOpen(modal) {
        return modal.classList.contains("is-open");
    }

    function openModal(modal) {
        focusHistory.set(
            modal,
            document.activeElement
        );

        modal.classList.add("is-open");

        modal.setAttribute(
            "aria-hidden",
            "false"
        );

        document.body.style.overflow = "hidden";

        const focusable = modal.querySelector(
            "input:not([type='hidden']), textarea, button"
        );

        if (focusable) {
            focusable.focus();
        }
    }

    function closeModal(modal) {
        if (busy) {
            return;
        }

        modal.classList.remove("is-open");

        modal.setAttribute(
            "aria-hidden",
            "true"
        );

        const anyOpen = [
            consoleModal,
            editorModal,
            deleteModal
        ].some(isOpen);

        if (!anyOpen) {
            document.body.style.overflow =
                originalBodyOverflow;
        }

        const previousFocus =
            focusHistory.get(modal);

        if (
            previousFocus &&
            document.contains(previousFocus)
        ) {
            previousFocus.focus();
        }

        focusHistory.delete(modal);
    }

    // ========================================
    // API REQUEST
    // ========================================

    async function apiRequest(
        url,
        options = {}
    ) {
        const response = await fetch(url, {
            credentials: "same-origin",
            cache: "no-store",
            ...options,
            headers: {
                "Accept": "application/json",
                ...(options.headers || {})
            }
        });

        const responseText =
            await response.text();

        let data = null;

        if (responseText.trim()) {
            try {
                data = JSON.parse(responseText);
            } catch (error) {
                throw new Error(
                    `Server returned an invalid response ` +
                    `(HTTP ${response.status}).`
                );
            }
        }

        // ====================================
        // HTTP ERRORS
        // ====================================

        if (
            !response.ok &&
            response.status !== 207
        ) {
            throw new Error(
                data?.message ||
                data?.detail ||
                data?.error ||
                `Request failed (HTTP ${response.status}).`
            );
        }

        // ====================================
        // PARTIAL DATABASE SYNCHRONIZATION
        // ====================================

        if (
            response.status === 207 ||
            data?.success === false
        ) {
            const succeeded =
                Array.isArray(data?.succeeded)
                    ? data.succeeded.join(", ")
                    : "None";

            const failed =
                Array.isArray(data?.failed)
                    ? data.failed
                        .map(
                            item =>
                                item.database || "Unknown"
                        )
                        .join(", ")
                    : "Unknown";

            const error = new Error(
                "Database synchronization incomplete. " +
                `Succeeded: ${succeeded}. ` +
                `Failed: ${failed}. ` +
                "Do not repeat the operation blindly."
            );

            error.partial = true;
            error.result = data;

            throw error;
        }

        return data;
    }

    // ========================================
    // SPLIT QUERIES FOR PREVIEW
    // ========================================

    function splitQueries(sqlText) {
        if (
            typeof sqlText !== "string" ||
            !sqlText.trim()
        ) {
            return [];
        }

        return sqlText
            .split(/\r?\n[ \t]*\r?\n+/)
            .map(block => block.trim())
            .filter(Boolean);
    }

    // ========================================
    // DETECT QUERY TYPE
    // ========================================

    function detectQueryType(block) {
        const sql = block
            .split(/\r?\n/)
            .filter(line => {
                const trimmed = line.trim();

                return (
                    !trimmed.startsWith("//") &&
                    !trimmed.startsWith("--")
                );
            })
            .join("\n")
            .trim();

        const match = sql.match(/^([a-zA-Z]+)/);

        return match
            ? match[1].toUpperCase()
            : "COMMENT";
    }

    // ========================================
    // UPDATE QUERY PREVIEW
    // ========================================

    function updatePreview() {
        const blocks =
            splitQueries(sqlTextInput.value);

        detectedCount.textContent =
            String(blocks.length);

        previewList.replaceChildren();

        blocks.forEach((block, index) => {
            const item =
                document.createElement("div");

            item.className =
                "sql-console-preview-item";

            const heading =
                document.createElement("div");

            heading.className =
                "sql-console-preview-item-title";

            heading.textContent =
                `Query ${index + 1} — ${detectQueryType(block)}`;

            const code =
                document.createElement("pre");

            code.className =
                "sql-console-preview-item-code";

            code.textContent = block;

            item.append(
                heading,
                code
            );

            previewList.appendChild(item);
        });
    }

    // ========================================
    // LOAD SAVED QUERIES
    // ========================================

    async function loadSavedQueries() {
        if (loading) {
            return false;
        }

        loading = true;
        refreshButton.disabled = true;

        setStatus(
            libraryStatus,
            "Loading saved queries..."
        );

        try {
            const data =
                await apiRequest(API_URL);

            if (!Array.isArray(data)) {
                throw new Error(
                    "Invalid saved queries response."
                );
            }

            savedQueries = data;

            renderSavedQueries();

            setStatus(
                libraryStatus,
                ""
            );

            return true;

        } catch (error) {
            setStatus(
                libraryStatus,
                error.message,
                "error"
            );

            console.error(
                "SQL Query Library load failed:",
                error
            );

            return false;

        } finally {
            loading = false;
            refreshButton.disabled = busy;
        }
    }

    // ========================================
    // SEARCH BY TITLE
    // ========================================

    function getFilteredQueries() {
        const search = searchInput.value
            .trim()
            .toLocaleLowerCase();

        if (!search) {
            return savedQueries;
        }

        return savedQueries.filter(query =>
            String(query.title || "")
                .toLocaleLowerCase()
                .includes(search)
        );
    }

    // ========================================
    // CREATE ACTION BUTTON
    // ========================================

    function createActionButton(
        label,
        cssClass,
        callback
    ) {
        const button =
            document.createElement("button");

        button.type = "button";

        button.className =
            `sql-console-button ${cssClass}`;

        button.textContent = label;

        button.addEventListener(
            "click",
            callback
        );

        return button;
    }

    // ========================================
    // RENDER SAVED QUERIES
    // ========================================

    function renderSavedQueries() {
        const queries = getFilteredQueries();

        queryList.replaceChildren();

        queryCount.textContent =
            `${queries.length} saved ` +
            (
                queries.length === 1
                    ? "query"
                    : "queries"
            );

        emptyState.hidden =
            queries.length !== 0;

        queries.forEach(query => {

            // ====================================
            // CARD
            // ====================================

            const card =
                document.createElement("div");

            card.className =
                "sql-console-query-card";

            const header =
                document.createElement("div");

            header.className =
                "sql-console-query-card-header";

            const heading =
                document.createElement("h3");

            heading.className =
                "sql-console-query-card-title";

            heading.textContent =
                query.title || "Untitled Query";

            const actions =
                document.createElement("div");

            actions.className =
                "sql-console-query-card-actions";

            // ====================================
            // SQL CODE
            // ====================================

            const pre =
                document.createElement("pre");

            pre.className =
                "sql-console-query-code";

            const code =
                document.createElement("code");

            code.textContent =
                query.sqlText || "";

            pre.appendChild(code);

            // ====================================
            // RESULTS CONTAINER
            // ====================================

            const results =
                document.createElement("div");

            results.className =
                "sql-console-query-result";

            // ====================================
            // RUN BUTTON
            // ====================================

            const runButton =
                createActionButton(
                    "Run",
                    "primary",
                    () => runSavedQuery(
                        query,
                        runButton,
                        results
                    )
                );

            runButton.title =
                "Execute SQL on the selected Navbar database";

            // ====================================
            // EDIT BUTTON
            // ====================================

            const editButton =
                createActionButton(
                    "Edit",
                    "secondary",
                    () => openEditQuery(query)
                );

            // ====================================
            // COPY BUTTON
            // ====================================

            const copyButton =
                createActionButton(
                    "Copy",
                    "secondary",
                    () => copyQuery(query)
                );

            // ====================================
            // DELETE BUTTON
            // ====================================

            const deleteButton =
                createActionButton(
                    "Delete",
                    "danger",
                    () => openDeleteQuery(query)
                );

            actions.append(
                runButton,
                editButton,
                copyButton,
                deleteButton
            );

            header.append(
                heading,
                actions
            );

            card.append(
                header,
                pre,
                results
            );

            queryList.appendChild(card);
        });
    }

    // ========================================
    // RUN SAVED QUERY
    // ========================================

    async function runSavedQuery(
        query,
        runButton,
        results
    ) {
        const key =
            query.queryUuid ||
            String(query.id || query.title);

        if (
            busy ||
            runningQueries.has(key)
        ) {
            return;
        }

        const sql =
            String(query.sqlText || "").trim();

        if (!sql) {
            showResultMessage(
                results,
                "SQL content is empty.",
                "error"
            );

            return;
        }

        // Execute only statements supported
        // by the backend. Confirm potentially
        // modifying statements.

        const sqlWithoutLeadingComments = sql
            .replace(
                /^(?:\s|--[^\r\n]*(?:\r?\n|$)|\/\*[\s\S]*?\*\/)*/g,
                ""
            )
            .trim();

        const isReadOnlyStatement =
            /^(SELECT|SHOW|DESCRIBE|DESC|EXPLAIN)\b/i
                .test(sqlWithoutLeadingComments);

        if (!isReadOnlyStatement) {
            const confirmed = window.confirm(
                `Execute SQL on the selected database?\n\n` +
                `${sql.substring(0, 300)}\n\n` +
                `WARNING: This statement may modify or delete data.`
            );

            if (!confirmed) {
                showResultMessage(
                    results,
                    "SQL execution cancelled.",
                    "info"
                );

                return;
            }
        }

        runningQueries.add(key);

        runButton.disabled = true;
        runButton.textContent = "Running...";

        showResultMessage(
            results,
            "Executing query...",
            "info"
        );

        try {
            const data = await apiRequest(
                SQL_EXECUTE_URL,
                {
                    method: "POST",
                    headers: {
                        "Content-Type":
                            "application/json"
                    },
                    body: JSON.stringify({
                        sql
                    })
                }
            );

            if (
                !data ||
                data.success !== true ||
                !Array.isArray(data.columns) ||
                !Array.isArray(data.rows)
            ) {
                throw new Error(
                    "Invalid SQL execution response."
                );
            }

            renderQueryResults(
                results,
                data
            );

        } catch (error) {
            showResultMessage(
                results,
                error.message,
                "error"
            );

            console.error(
                "SQL query execution failed:",
                error
            );

        } finally {
            runningQueries.delete(key);

            runButton.disabled = false;
            runButton.textContent = "Run";
        }
    }

    // ========================================
    // RESULT MESSAGE
    // ========================================

    function showResultMessage(
        container,
        message,
        status = "info"
    ) {
        container.replaceChildren();

        const messageElement =
            document.createElement("div");

        messageElement.className =
            "sql-console-result-message";

        messageElement.textContent = message;
        messageElement.dataset.status = status;

        messageElement.style.padding = "12px";
        messageElement.style.marginTop = "12px";
        messageElement.style.borderRadius = "8px";
        messageElement.style.whiteSpace = "pre-wrap";

        if (status === "error") {
            messageElement.style.color =
                "#b91c1c";
        }

        if (status === "success") {
            messageElement.style.color =
                "#15803d";
        }

        container.appendChild(
            messageElement
        );
    }

    // ========================================
    // RENDER QUERY RESULTS
    // ========================================

    function renderQueryResults(
        container,
        data
    ) {
        container.replaceChildren();

        const database =
            String(data.database || "Unknown");

        const rows = data.rows;
        const columns = data.columns;

        const summary =
            document.createElement("div");

        summary.className =
            "sql-console-result-summary";

        summary.style.padding = "12px 0";
        summary.style.fontWeight = "600";

        summary.textContent =
            `Database: ${database} | ` +
            `Rows: ${rows.length}` +
            (
                rows.length === 200
                    ? " (maximum 200 rows displayed)"
                    : ""
            );

        container.appendChild(summary);

        if (rows.length === 0) {
            showResultMessage(
                container,
                data.message ||
                `SQL executed successfully on ${database}. ` +
                `${data.rowCount ?? 0} row(s) affected.`,
                "success"
            );

            return;
        }

        const wrapper =
            document.createElement("div");

        wrapper.className =
            "sql-console-result-table-wrapper";

        wrapper.style.width = "100%";
        wrapper.style.overflowX = "auto";
        wrapper.style.marginTop = "8px";
        wrapper.style.marginBottom = "12px";

        const table =
            document.createElement("table");

        table.className =
            "sql-console-result-table";

        table.style.width = "100%";
        table.style.borderCollapse = "collapse";
        table.style.fontSize = "13px";

        const thead =
            document.createElement("thead");

        const headerRow =
            document.createElement("tr");

        columns.forEach(column => {
            const th =
                document.createElement("th");

            th.textContent =
                String(column);

            th.style.padding = "10px";
            th.style.textAlign = "left";

            th.style.borderBottom =
                "2px solid #cbd5e1";

            headerRow.appendChild(th);
        });

        thead.appendChild(headerRow);
        table.appendChild(thead);

        const tbody =
            document.createElement("tbody");

        rows.forEach(row => {
            const tr =
                document.createElement("tr");

            columns.forEach(column => {
                const td =
                    document.createElement("td");

                const value =
                    row[String(column)];

                if (
                    value === null ||
                    value === undefined
                ) {
                    td.textContent = "NULL";

                } else if (
                    typeof value === "object"
                ) {
                    td.textContent =
                        JSON.stringify(value);

                } else {
                    td.textContent =
                        String(value);
                }

                td.style.padding = "8px 10px";
                td.style.borderBottom =
                    "1px solid #e2e8f0";
                td.style.maxWidth = "500px";
                td.style.overflowWrap = "anywhere";

                tr.appendChild(td);
            });

            tbody.appendChild(tr);
        });

        table.appendChild(tbody);
        wrapper.appendChild(table);

        container.appendChild(wrapper);
    }

    // ========================================
    // NEW QUERY
    // ========================================

    function openNewQuery() {
        if (busy) {
            return;
        }

        editorForm.reset();
        editorUuid.value = "";

        editorTitle.textContent =
            "New SQL Query";

        saveButton.textContent =
            "Save Query";

        setStatus(
            editorStatus,
            ""
        );

        updatePreview();

        openModal(editorModal);

        titleInput.focus();
    }

    // ========================================
    // EDIT QUERY
    // ========================================

    function openEditQuery(query) {
        if (busy) {
            return;
        }

        if (!query.queryUuid) {
            setStatus(
                libraryStatus,
                "This query has no UUID and cannot be edited.",
                "error"
            );

            return;
        }

        editorForm.reset();

        editorUuid.value =
            query.queryUuid;

        titleInput.value =
            query.title || "";

        sqlTextInput.value =
            query.sqlText || "";

        selectCommandForExistingQuery(query);

        editorTitle.textContent =
            "Edit SQL Query";

        saveButton.textContent =
            "Save Changes";

        setStatus(
            editorStatus,
            ""
        );

        updatePreview();

        openModal(editorModal);

        titleInput.focus();
    }

    // ========================================
    // SAVE / UPDATE QUERY
    // ========================================

    async function saveQuery(event) {
        event.preventDefault();

        if (busy) {
            return;
        }

        const queryUuid =
            editorUuid.value.trim();

        applySqlTitlePrefix();

        const title =
            titleInput.value.trim();

        const sqlText =
            sqlTextInput.value;

        if (!title) {
            setStatus(
                editorStatus,
                "Query title is required.",
                "error"
            );

            return;
        }

        if (!sqlText.trim()) {
            setStatus(
                editorStatus,
                "SQL content is required.",
                "error"
            );

            return;
        }

        const editing =
            Boolean(queryUuid);

        const url = editing
            ? `${API_URL}/${encodeURIComponent(queryUuid)}`
            : API_URL;

        const method =
            editing ? "PUT" : "POST";

        setBusy(true);

        setStatus(
            editorStatus,
            editing
                ? "Updating query in all three databases..."
                : "Saving query in all three databases..."
        );

        try {
            await apiRequest(url, {
                method,
                headers: {
                    "Content-Type":
                        "application/json"
                },
                body: JSON.stringify({
                    title,
                    sqlText
                })
            });

            setBusy(false);
            closeModal(editorModal);

            const loaded =
                await loadSavedQueries();

            if (loaded) {
                setStatus(
                    libraryStatus,
                    editing
                        ? "Query updated in all three databases."
                        : "Query saved in all three databases.",
                    "success"
                );
            }

        } catch (error) {
            setStatus(
                editorStatus,
                error.message,
                "error"
            );

            console.error(
                "Saved query operation failed:",
                error
            );

        } finally {
            setBusy(false);
        }
    }

    // ========================================
    // COPY QUERY
    // ========================================

    async function copyQuery(query) {
        try {
            await navigator.clipboard.writeText(
                query.sqlText || ""
            );

            setStatus(
                libraryStatus,
                `Copied: ${query.title}`,
                "success"
            );

        } catch (error) {
            setStatus(
                libraryStatus,
                "Unable to copy SQL text.",
                "error"
            );
        }
    }

    // ========================================
    // OPEN DELETE CONFIRMATION
    // ========================================

    function openDeleteQuery(query) {
        if (busy) {
            return;
        }

        if (!query.queryUuid) {
            setStatus(
                libraryStatus,
                "This query has no UUID and cannot be deleted.",
                "error"
            );

            return;
        }

        selectedDeleteQuery = query;

        deleteName.textContent =
            query.title || "Untitled Query";

        setStatus(
            deleteStatus,
            ""
        );

        openModal(deleteModal);
    }

    // ========================================
    // CONFIRM DELETE
    // ========================================

    async function confirmDeleteQuery() {
        if (
            busy ||
            !selectedDeleteQuery
        ) {
            return;
        }

        const query =
            selectedDeleteQuery;

        const uuid =
            query.queryUuid;

        if (!uuid) {
            setStatus(
                deleteStatus,
                "Query UUID is missing.",
                "error"
            );

            return;
        }

        setBusy(true);

        setStatus(
            deleteStatus,
            "Deleting saved query from all three databases..."
        );

        try {
            await apiRequest(
                `${API_URL}/${encodeURIComponent(uuid)}`,
                {
                    method: "DELETE"
                }
            );

            setBusy(false);
            closeModal(deleteModal);

            selectedDeleteQuery = null;

            const loaded =
                await loadSavedQueries();

            if (loaded) {
                setStatus(
                    libraryStatus,
                    "Saved query deleted from all three databases.",
                    "success"
                );
            }

        } catch (error) {
            setStatus(
                deleteStatus,
                error.message,
                "error"
            );

            console.error(
                "Saved query deletion failed:",
                error
            );

        } finally {
            setBusy(false);
        }
    }

    // ========================================
    // MAIN CONSOLE EVENTS
    // ========================================

    openButton.addEventListener(
        "click",
        async () => {
            if (busy) {
                return;
            }

            openModal(consoleModal);

            await loadSavedQueries();
        }
    );

    closeConsoleButton.addEventListener(
        "click",
        () => closeModal(consoleModal)
    );

    closeConsoleFooterButton.addEventListener(
        "click",
        () => closeModal(consoleModal)
    );

    refreshButton.addEventListener(
        "click",
        loadSavedQueries
    );

    searchInput.addEventListener(
        "input",
        renderSavedQueries
    );

    newQueryButton.addEventListener(
        "click",
        openNewQuery
    );

    // ========================================
    // EDITOR EVENTS
    // ========================================

    if (commandSelector) {
        commandSelector.addEventListener(
            "change",
            applySqlTitlePrefix
        );
    }

    editorForm.addEventListener(
        "submit",
        saveQuery
    );

    sqlTextInput.addEventListener(
        "input",
        updatePreview
    );

    closeEditorButton.addEventListener(
        "click",
        () => closeModal(editorModal)
    );

    cancelEditorButton.addEventListener(
        "click",
        () => closeModal(editorModal)
    );

    // ========================================
    // DELETE EVENTS
    // ========================================

    confirmDeleteButton.addEventListener(
        "click",
        confirmDeleteQuery
    );

    cancelDeleteButton.addEventListener(
        "click",
        () => closeModal(deleteModal)
    );

    closeDeleteButton.addEventListener(
        "click",
        () => closeModal(deleteModal)
    );

    // ========================================
    // ESCAPE KEY
    // ========================================

    document.addEventListener(
        "keydown",
        event => {
            if (
                event.key !== "Escape" ||
                busy
            ) {
                return;
            }

            if (isOpen(deleteModal)) {
                closeModal(deleteModal);
                return;
            }

            if (isOpen(editorModal)) {
                closeModal(editorModal);
                return;
            }

            if (isOpen(consoleModal)) {
                closeModal(consoleModal);
            }
        }
    );
});


/* ============================================
   SQL QUERY CONSOLE
   FLOATING WINDOW CONTROLS
   DRAG / RESIZE / MAXIMIZE / RESTORE
   ============================================ */

document.addEventListener("DOMContentLoaded", () => {
    "use strict";

    // ========================================
    // ELEMENTS
    // ========================================

    const windowElement =
        document.getElementById(
            "sqlQueryConsoleWindow"
        );

    const dragHandle =
        document.getElementById(
            "sqlQueryConsoleDragHandle"
        );

    const maximizeButton =
        document.getElementById(
            "maximizeSqlConsoleButton"
        );

    const overlay =
        document.getElementById(
            "sqlQueryConsoleModal"
        );

    if (
        !windowElement ||
        !dragHandle ||
        !maximizeButton ||
        !overlay
    ) {
        console.warn(
            "SQL Console: Floating window elements missing."
        );

        return;
    }

    // ========================================
    // STANDALONE WINDOW DETECTION
    // ========================================

    const STANDALONE_PATH =
        "/automation/sql-console";

    const isStandaloneWindow =
        window.location.pathname.replace(/\/$/, "") ===
        STANDALONE_PATH;

    // In standalone mode, use native browser
    // window controls instead of modal dragging.

    if (isStandaloneWindow) {
        dragHandle.style.cursor =
            "default";

        maximizeButton.hidden = true;
        maximizeButton.style.display =
            "none";

        windowElement.style.resize =
            "none";

        return;
    }

    // ========================================
    // STATE
    // ========================================

    let maximized = false;
    let dragging = false;
    let dragPointerId = null;

    let dragStartX = 0;
    let dragStartY = 0;

    let initialLeft = 0;
    let initialTop = 0;

    let restoredGeometry = null;

    // ========================================
    // HELPERS
    // ========================================

    function clamp(
        value,
        minimum,
        maximum
    ) {
        return Math.min(
            Math.max(value, minimum),
            Math.max(minimum, maximum)
        );
    }

    function fitInsideViewport() {
        if (maximized) {
            return;
        }

        const rect =
            windowElement.getBoundingClientRect();

        const width = Math.min(
            rect.width,
            window.innerWidth
        );

        const height = Math.min(
            rect.height,
            window.innerHeight
        );

        windowElement.style.width =
            `${width}px`;

        windowElement.style.height =
            `${height}px`;

        windowElement.style.left = `${
            clamp(
                rect.left,
                0,
                window.innerWidth - width
            )
        }px`;

        windowElement.style.top = `${
            clamp(
                rect.top,
                0,
                window.innerHeight - height
            )
        }px`;
    }

    // ========================================
    // DRAG START
    // ========================================

    dragHandle.addEventListener(
        "pointerdown",
        event => {
            if (
                event.button !== 0 ||
                maximized ||
                event.target.closest("button")
            ) {
                return;
            }

            const rect =
                windowElement.getBoundingClientRect();

            dragging = true;

            dragPointerId =
                event.pointerId;

            dragStartX =
                event.clientX;

            dragStartY =
                event.clientY;

            initialLeft =
                rect.left;

            initialTop =
                rect.top;

            windowElement.style.left =
                `${rect.left}px`;

            windowElement.style.top =
                `${rect.top}px`;

            windowElement.style.width =
                `${rect.width}px`;

            windowElement.style.height =
                `${rect.height}px`;

            dragHandle.classList.add(
                "is-dragging"
            );

            dragHandle.setPointerCapture(
                event.pointerId
            );

            event.preventDefault();
        }
    );

    // ========================================
    // DRAG MOVE
    // ========================================

    dragHandle.addEventListener(
        "pointermove",
        event => {
            if (
                !dragging ||
                event.pointerId !== dragPointerId
            ) {
                return;
            }

            const deltaX =
                event.clientX - dragStartX;

            const deltaY =
                event.clientY - dragStartY;

            const rect =
                windowElement.getBoundingClientRect();

            const newLeft = clamp(
                initialLeft + deltaX,
                0,
                window.innerWidth - rect.width
            );

            const newTop = clamp(
                initialTop + deltaY,
                0,
                window.innerHeight - rect.height
            );

            windowElement.style.left =
                `${newLeft}px`;

            windowElement.style.top =
                `${newTop}px`;
        }
    );

    // ========================================
    // DRAG END
    // ========================================

    function stopDragging() {
        dragging = false;
        dragPointerId = null;

        dragHandle.classList.remove(
            "is-dragging"
        );
    }

    dragHandle.addEventListener(
        "pointerup",
        stopDragging
    );

    dragHandle.addEventListener(
        "pointercancel",
        stopDragging
    );

    dragHandle.addEventListener(
        "lostpointercapture",
        stopDragging
    );

    // ========================================
    // MAXIMIZE / RESTORE
    // ========================================

    function toggleMaximize() {
        if (!maximized) {
            const rect =
                windowElement.getBoundingClientRect();

            restoredGeometry = {
                left: rect.left,
                top: rect.top,
                width: rect.width,
                height: rect.height
            };

            maximized = true;

            windowElement.classList.add(
                "is-maximized"
            );

            maximizeButton.textContent =
                "❐";

            maximizeButton.title =
                "Restore Window";

            maximizeButton.setAttribute(
                "aria-label",
                "Restore SQL Query Console"
            );

        } else {
            maximized = false;

            windowElement.classList.remove(
                "is-maximized"
            );

            if (restoredGeometry) {
                windowElement.style.left =
                    `${restoredGeometry.left}px`;

                windowElement.style.top =
                    `${restoredGeometry.top}px`;

                windowElement.style.width =
                    `${restoredGeometry.width}px`;

                windowElement.style.height =
                    `${restoredGeometry.height}px`;
            }

            maximizeButton.textContent =
                "□";

            maximizeButton.title =
                "Maximize Window";

            maximizeButton.setAttribute(
                "aria-label",
                "Maximize SQL Query Console"
            );

            fitInsideViewport();
        }
    }

    maximizeButton.addEventListener(
        "click",
        toggleMaximize
    );

    // ========================================
    // DOUBLE CLICK HEADER
    // ========================================

    dragHandle.addEventListener(
        "dblclick",
        event => {
            if (
                event.target.closest("button")
            ) {
                return;
            }

            toggleMaximize();
        }
    );

    // ========================================
    // VIEWPORT RESIZE
    // ========================================

    window.addEventListener(
        "resize",
        () => {
            if (
                !overlay.classList.contains(
                    "is-open"
                )
            ) {
                return;
            }

            fitInsideViewport();
        }
    );

    // ========================================
    // INITIALIZE
    // ========================================

    maximizeButton.textContent = "□";

    maximizeButton.title =
        "Maximize Window";

    maximizeButton.setAttribute(
        "aria-label",
        "Maximize SQL Query Console"
    );
});
