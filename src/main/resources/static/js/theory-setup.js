
document.addEventListener("DOMContentLoaded", () => {

    const initializeButton =
        document.getElementById("initializeTheoryButton");

    const infoElement =
        document.getElementById("theorySetupInfo");

    const resultElement =
        document.getElementById("theorySetupResult");

    const API_BASE = "/cf_automation/theory-setup";

    if (!initializeButton || !infoElement || !resultElement) {
        return;
    }

    let isConfigured = false;
    let selectedDatabase = null;
    let isExecuting = false;

    // ========================================
    // DISPLAY HELPERS
    // ========================================

    function setResult(message, type = "info") {
        resultElement.textContent = message;
        resultElement.dataset.status = type;
    }

    function updateButton() {
        initializeButton.disabled =
            !isConfigured || isExecuting;
    }

    // ========================================
    // LOAD THEORY SETUP STATUS
    // ========================================

    async function loadTheorySetupStatus() {

        initializeButton.disabled = true;

        infoElement.textContent =
            "Checking theory setup configuration...";

        try {

            const response = await fetch(
                `${API_BASE}/status`,
                {
                    method: "GET",
                    credentials: "same-origin",
                    cache: "no-store"
                }
            );

            if (!response.ok) {
                throw new Error(
                    `Unable to load status (${response.status}).`
                );
            }

            const data = await response.json();

            isConfigured = data.configured === true;
            selectedDatabase = data.database || null;

            if (!isConfigured) {
                infoElement.textContent =
                    `Database: ${selectedDatabase || "Unknown"} — ` +
                    "Theory setup queries are not configured yet.";

                setResult(
                    "The Initialize Theory button will be available " +
                    "after the SQL queries have been added."
                );
            } else {
                infoElement.textContent =
                    `Selected database: ${selectedDatabase}`;

                setResult("Theory setup is ready.");
            }

        } catch (error) {

            isConfigured = false;

            infoElement.textContent =
                "Unable to load theory setup status.";

            setResult(error.message, "error");

            console.error(
                "Theory setup status error:",
                error
            );
        }

        updateButton();
    }

    // ========================================
    // EXECUTE THEORY SETUP
    // ========================================

    async function executeTheorySetup() {

        if (!isConfigured || isExecuting) {
            return;
        }

        // Refresh status immediately before execution.
        // This reduces the chance of using a stale DB selection.
        try {
            const statusResponse = await fetch(
                `${API_BASE}/status`,
                {
                    credentials: "same-origin",
                    cache: "no-store"
                }
            );

            if (!statusResponse.ok) {
                throw new Error(
                    "Unable to verify the selected database."
                );
            }

            const status = await statusResponse.json();

            if (
                status.configured !== true ||
                status.database !== selectedDatabase
            ) {
                isConfigured = status.configured === true;
                selectedDatabase = status.database || null;

                infoElement.textContent =
                    `Selected database: ${selectedDatabase || "Unknown"}`;

                setResult(
                    "Configuration or database changed. " +
                    "Please review and click again.",
                    "error"
                );

                updateButton();
                return;
            }

        } catch (error) {
            setResult(error.message, "error");
            return;
        }

        const confirmed = window.confirm(
            `Initialize theory in ${selectedDatabase}?\n\n` +
            "This operation may delete and recreate theory data " +
            "once the SQL cleanup queries are configured.\n\n" +
            "Continue?"
        );

        if (!confirmed) {
            return;
        }

        isExecuting = true;
        updateButton();

        initializeButton.textContent = "Initializing...";

        setResult(
            `Executing theory setup on ${selectedDatabase}...`
        );

        try {

            const response = await fetch(
                `${API_BASE}/execute`,
                {
                    method: "POST",
                    credentials: "same-origin",
                    headers: {
                        "Content-Type": "application/json"
                    }
                }
            );

            const data = await response.json();

            if (!response.ok || data.success !== true) {
                throw new Error(
                    data.message ||
                    `Theory setup failed (${response.status}).`
                );
            }

            setResult(
                data.message || "Theory setup completed successfully.",
                "success"
            );

        } catch (error) {

            setResult(
                `Theory setup failed: ${error.message}`,
                "error"
            );

            console.error(
                "Theory setup execution error:",
                error
            );

        } finally {

            isExecuting = false;

            initializeButton.textContent =
                "Initialize Theory";

            updateButton();
        }
    }

    // ========================================
    // EVENT LISTENERS
    // ========================================

    initializeButton.addEventListener(
        "click",
        executeTheorySetup
    );

    // Initial status load
    loadTheorySetupStatus();

});
