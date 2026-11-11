
document.addEventListener("DOMContentLoaded", () => {
    "use strict";

    // ========================================
    // ELEMENTS
    // ========================================

    const deleteAllDocumentsButton =
        document.getElementById("cleanDatabaseButton");

    const deleteAllDocumentsResult =
        document.getElementById("databaseCleanupResult");

    if (
        !deleteAllDocumentsButton ||
        !deleteAllDocumentsResult
    ) {
        console.error(
            "Manage Database: Required HTML elements missing."
        );
        return;
    }

    // ========================================
    // API URLS
    // ========================================

    const STATUS_URL =
        "/cf_automation/theory-setup/status";

    // Existing, previously tested cleanup endpoint.
    //
    // IMPORTANT:
    // This also removes categories except
    // the default category "Διάφορα".
    const CLEAN_DATABASE_URL =
        "/cf_automation/database-cleanup/execute";

    // ========================================
    // STATE
    // ========================================

    let selectedDatabase = null;
    let isExecuting = false;
    let isCheckingDatabase = false;

    // ========================================
    // RESULT MESSAGE
    // ========================================

    function setResult(
        element,
        message,
        status = "info"
    ) {
        element.textContent = message;
        element.dataset.status = status;
    }

    // ========================================
    // NORMALIZE DATABASE
    // ========================================

    function normalizeDatabase(value) {
        if (typeof value !== "string") {
            return null;
        }

        const normalized = value
            .trim()
            .toUpperCase()
            .replace(/[\s-]+/g, "_");

        const aliases = {
            FRONTENDTESTING: "FRONTEND_TESTING",
            APITESTING: "API_TESTING"
        };

        const database =
            aliases[normalized] || normalized;

        const validDatabases = [
            "PRODUCTION",
            "FRONTEND_TESTING",
            "API_TESTING"
        ];

        return validDatabases.includes(database)
            ? database
            : null;
    }

    // ========================================
    // UPDATE BUTTON STATE
    // ========================================

    function updateButtons() {
        deleteAllDocumentsButton.disabled =
            isExecuting ||
            isCheckingDatabase ||
            !selectedDatabase;
    }

    // ========================================
    // READ HTTP RESPONSE
    // ========================================

    async function readResponse(response) {
        const responseText = await response.text();

        if (!responseText.trim()) {
            return null;
        }

        try {
            return JSON.parse(responseText);
        } catch (error) {
            throw new Error(
                "Server returned an invalid response " +
                `(HTTP ${response.status}).`
            );
        }
    }

    // ========================================
    // READ SELECTED DATABASE
    // ========================================

    async function fetchSelectedDatabase() {
        const response = await fetch(STATUS_URL, {
            method: "GET",
            credentials: "same-origin",
            cache: "no-store",
            headers: {
                "Accept": "application/json"
            }
        });

        const data = await readResponse(response);

        if (!response.ok) {
            throw new Error(
                data?.message ||
                data?.detail ||
                "Cannot read selected database " +
                `(HTTP ${response.status}).`
            );
        }

        const database =
            normalizeDatabase(data?.database);

        if (!database) {
            throw new Error(
                "Select a valid database from the Navbar."
            );
        }

        return database;
    }

    // ========================================
    // LOAD DATABASE STATUS
    // ========================================

    async function loadDatabaseStatus() {
        if (isExecuting || isCheckingDatabase) {
            return;
        }

        isCheckingDatabase = true;
        selectedDatabase = null;

        updateButtons();

        try {
            const database =
                await fetchSelectedDatabase();

            selectedDatabase = database;

            setResult(
                deleteAllDocumentsResult,
                `Selected database: ${database}`,
                "info"
            );

        } catch (error) {
            selectedDatabase = null;

            setResult(
                deleteAllDocumentsResult,
                error.message,
                "error"
            );

            console.error(
                "Database status error:",
                error
            );

        } finally {
            isCheckingDatabase = false;
            updateButtons();
        }
    }

    // ========================================
    // CONFIRM CLEANUP
    // ========================================

    function confirmCleanup(database) {
        const confirmed = window.confirm(
            "DATABASE CLEANUP\n\n" +
            `Selected database: ${database}\n\n` +
            "The existing cleanup will permanently delete:\n" +
            "- ALL documents\n" +
            "- ALL document formatting records\n" +
            "- ALL categories except Διάφορα\n\n" +
            "User accounts will NOT be deleted.\n\n" +
            "WARNING: This action cannot be undone.\n\n" +
            "Do you want to continue?"
        );

        if (!confirmed) {
            return false;
        }

        // Extra confirmation for Production.
        if (database === "PRODUCTION") {
            const typedValue = window.prompt(
                "PRODUCTION DATABASE\n\n" +
                "This cleanup permanently deletes " +
                "documents, formats and categories.\n\n" +
                "Type PRODUCTION to confirm:"
            );

            return typedValue === "PRODUCTION";
        }

        return true;
    }

    // ========================================
    // EXECUTE CLEANUP
    // ========================================

    async function executeCleanup() {
        if (isExecuting || isCheckingDatabase) {
            return;
        }

        // ====================================
        // CHECK SELECTED DATABASE
        // ====================================

        let database;

        try {
            database =
                await fetchSelectedDatabase();

            selectedDatabase = database;
            updateButtons();

        } catch (error) {
            selectedDatabase = null;
            updateButtons();

            setResult(
                deleteAllDocumentsResult,
                error.message,
                "error"
            );

            return;
        }

        // ====================================
        // USER CONFIRMATION
        // ====================================

        const confirmed =
            confirmCleanup(database);

        if (!confirmed) {
            setResult(
                deleteAllDocumentsResult,
                "Cleanup cancelled.",
                "info"
            );

            return;
        }

        // ====================================
        // VERIFY DATABASE AGAIN
        // ====================================

        try {
            const currentDatabase =
                await fetchSelectedDatabase();

            if (currentDatabase !== database) {
                selectedDatabase = currentDatabase;
                updateButtons();

                setResult(
                    deleteAllDocumentsResult,
                    "The selected database changed. " +
                    "Please confirm the operation again.",
                    "error"
                );

                return;
            }

        } catch (error) {
            setResult(
                deleteAllDocumentsResult,
                error.message,
                "error"
            );

            return;
        }

        // ====================================
        // START CLEANUP
        // ====================================

        isExecuting = true;
        updateButtons();

        const originalText =
            deleteAllDocumentsButton.textContent;

        deleteAllDocumentsButton.textContent =
            "Executing...";

        setResult(
            deleteAllDocumentsResult,
            `Cleaning database ${database}...`,
            "info"
        );

        try {
            const response = await fetch(
                CLEAN_DATABASE_URL,
                {
                    method: "POST",
                    credentials: "same-origin",
                    cache: "no-store",
                    headers: {
                        "Accept": "application/json",
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        database: database,
                        confirmation: database
                    })
                }
            );

            const data =
                await readResponse(response);

            // ====================================
            // HTTP ERROR
            // ====================================

            if (!response.ok) {
                throw new Error(
                    data?.message ||
                    data?.detail ||
                    data?.error ||
                    `Cleanup failed (HTTP ${response.status}).`
                );
            }

            // ====================================
            // VERIFY SUCCESS RESPONSE
            // ====================================

            if (data?.success !== true) {
                throw new Error(
                    data?.message ||
                    "The server did not confirm " +
                    "that cleanup completed. " +
                    "Check the database before retrying."
                );
            }

            // ====================================
            // SUCCESS
            // ====================================

            setResult(
                deleteAllDocumentsResult,
                data.message ||
                `Database cleanup completed in ${database}.`,
                "success"
            );

        } catch (error) {
            setResult(
                deleteAllDocumentsResult,
                `Cleanup failed: ${error.message}`,
                "error"
            );

            console.error(
                "Database cleanup error:",
                error
            );

        } finally {
            isExecuting = false;

            deleteAllDocumentsButton.textContent =
                originalText;

            updateButtons();
        }
    }

    // ========================================
    // EVENTS
    // ========================================

    deleteAllDocumentsButton.addEventListener(
        "click",
        executeCleanup
    );

    window.addEventListener(
        "pageshow",
        loadDatabaseStatus
    );

    // ========================================
    // INITIAL LOAD
    // ========================================

    loadDatabaseStatus();
});
