
"use strict";

/* ========================================
   APP LOGS CONFIGURATION
   ======================================== */

const APP_LOGS_REFRESH_INTERVAL = 2000;
const APP_LOGS_MAX_LINES = 3000;

const APP_LOGS_ENDPOINTS = {
    automation: "/cf_automation/app-logs/automation",
    parser: "/cf_automation/app-logs/parser"
};

/* ========================================
   ELEMENTS
   ======================================== */

const automationLogsTab =
    document.getElementById("automationLogsTab");

const parserLogsTab =
    document.getElementById("parserLogsTab");

const automationLogsPanel =
    document.getElementById("automationLogsPanel");

const parserLogsPanel =
    document.getElementById("parserLogsPanel");

const automationLogsOutput =
    document.getElementById("automationLogsOutput");

const parserLogsOutput =
    document.getElementById("parserLogsOutput");

const appLogsConnectionStatus =
    document.getElementById("appLogsConnectionStatus");

const appLogsStatusText =
    document.getElementById("appLogsStatusText");

const appLogsLiveIndicator =
    document.getElementById("appLogsLiveIndicator");

const appLogsActiveSource =
    document.getElementById("appLogsActiveSource");

const appLogsLineCount =
    document.getElementById("appLogsLineCount");

const appLogsAutoScroll =
    document.getElementById("appLogsAutoScroll");

const appLogsPauseButton =
    document.getElementById("appLogsPauseButton");

const appLogsClearButton =
    document.getElementById("appLogsClearButton");

/* ========================================
   STATE
   ======================================== */

let activeLogSource = "automation";
let logsPaused = false;
let refreshInProgress = false;

const logState = {
    automation: {
        lines: [],
        visibleLines: [],
        cleared: false,
        connected: false,
        error: null
    },
    parser: {
        lines: [],
        visibleLines: [],
        cleared: false,
        connected: false,
        error: null
    }
};

/* ========================================
   HELPERS
   ======================================== */

function getActiveState() {
    return logState[activeLogSource];
}

function getOutput(source) {
    return source === "automation"
        ? automationLogsOutput
        : parserLogsOutput;
}

function scrollToBottom(output) {

    if (!output || !appLogsAutoScroll.checked) {
        return;
    }

    output.scrollTop = output.scrollHeight;
}

function normalizeLogsResponse(data) {

    // Backend response:
    // { lines: ["...", "..."] }
    //
    // Each refresh returns a snapshot,
    // not an append-only stream.

    if (
        !data ||
        !Array.isArray(data.lines)
    ) {
        throw new Error("Invalid logs response.");
    }

    return data.lines
        .filter(line => typeof line === "string")
        .slice(-APP_LOGS_MAX_LINES);
}

/* ========================================
   FIND NEW LINES BETWEEN SNAPSHOTS
   ======================================== */

function countOverlappingLines(previous, current) {

    if (
        previous.length === 0 ||
        current.length === 0
    ) {
        return 0;
    }

    // Find the longest suffix of the previous
    // snapshot that matches the prefix of
    // the current snapshot.
    //
    // This also works when older lines have
    // been removed from the server snapshot.

    const separator = null;

    const sequence = [
        ...current,
        separator,
        ...previous
    ];

    const prefix = new Array(sequence.length).fill(0);

    for (let i = 1; i < sequence.length; i++) {

        let j = prefix[i - 1];

        while (
            j > 0 &&
            sequence[i] !== sequence[j]
        ) {
            j = prefix[j - 1];
        }

        if (sequence[i] === sequence[j]) {
            j++;
        }

        prefix[i] = j;
    }

    return Math.min(
        prefix[prefix.length - 1],
        current.length
    );
}

/* ========================================
   RENDER LOG OUTPUT
   ======================================== */

function renderSource(source) {

    const state = logState[source];
    const output = getOutput(source);

    if (!output) {
        return;
    }

    const visibleLines = state.visibleLines;

    if (visibleLines.length > 0) {

        output.textContent = visibleLines.join("\n");

    } else if (state.error) {

        output.textContent = state.error;

    } else if (!state.connected) {

        output.textContent = "Waiting for logs...";

    } else {

        output.textContent = "No log entries available.";
    }

    if (source === activeLogSource) {
        scrollToBottom(output);
    }
}

/* ========================================
   CONNECTION STATUS
   ======================================== */

function updateConnectionStatus() {

    const state = getActiveState();

    appLogsConnectionStatus.classList.remove(
        "connected",
        "disconnected"
    );

    if (logsPaused) {

        appLogsStatusText.textContent = "Paused";

    } else if (state.connected) {

        appLogsConnectionStatus.classList.add(
            "connected"
        );

        appLogsStatusText.textContent = "Connected";

    } else {

        appLogsConnectionStatus.classList.add(
            "disconnected"
        );

        appLogsStatusText.textContent =
            state.error ? "Disconnected" : "Connecting...";
    }

    appLogsLiveIndicator.textContent =
        logsPaused ? "PAUSED" : "LIVE";

    appLogsLiveIndicator.classList.toggle(
        "paused",
        logsPaused
    );

    appLogsActiveSource.textContent =
        activeLogSource === "automation"
            ? "Automation Platform"
            : "Parser Service";

    appLogsLineCount.textContent =
        `${state.visibleLines.length} lines`;
}

/* ========================================
   ACTIVATE TAB
   ======================================== */

function activateTab(source) {

    activeLogSource = source;

    const automationActive = source === "automation";
    const parserActive = source === "parser";

    automationLogsTab.classList.toggle(
        "active",
        automationActive
    );

    parserLogsTab.classList.toggle(
        "active",
        parserActive
    );

    automationLogsTab.setAttribute(
        "aria-selected",
        String(automationActive)
    );

    parserLogsTab.setAttribute(
        "aria-selected",
        String(parserActive)
    );

    automationLogsTab.tabIndex =
        automationActive ? 0 : -1;

    parserLogsTab.tabIndex =
        parserActive ? 0 : -1;

    automationLogsPanel.hidden = !automationActive;
    parserLogsPanel.hidden = !parserActive;

    updateConnectionStatus();
    renderSource(source);

    if (!logsPaused) {
        refreshSource(source);
    }
}

/* ========================================
   FETCH LOG SNAPSHOT
   ======================================== */

async function fetchLogSnapshot(source) {

    const response = await fetch(
        APP_LOGS_ENDPOINTS[source],
        {
            method: "GET",
            credentials: "same-origin",
            cache: "no-store",
            headers: {
                "Accept": "application/json"
            }
        }
    );

    if (!response.ok) {

        throw new Error(
            `Unable to load logs (HTTP ${response.status}).`
        );
    }

    return normalizeLogsResponse(
        await response.json()
    );
}

/* ========================================
   UPDATE SOURCE SNAPSHOT
   ======================================== */

function updateSourceSnapshot(source, newLines) {

    const state = logState[source];

    if (state.cleared) {

        // After Clear View, show only new lines.
        // Existing server logs remain untouched.

        const overlap = countOverlappingLines(
            state.lines,
            newLines
        );

        const addedLines = newLines.slice(overlap);

        state.visibleLines = [
            ...state.visibleLines,
            ...addedLines
        ].slice(-APP_LOGS_MAX_LINES);

    } else {

        // Normal mode: display the latest
        // snapshot from the backend.

        state.visibleLines = newLines;
    }

    // Always remember the latest complete
    // snapshot for the next comparison.

    state.lines = newLines;
    state.connected = true;
    state.error = null;
}

/* ========================================
   REFRESH ONE SOURCE
   ======================================== */

async function refreshSource(source) {

    if (
        logsPaused ||
        refreshInProgress
    ) {
        return;
    }

    refreshInProgress = true;

    try {

        const lines = await fetchLogSnapshot(source);

        if (logsPaused) {
            return;
        }

        updateSourceSnapshot(source, lines);

        renderSource(source);

    } catch (error) {

        const state = logState[source];

        state.connected = false;
        state.error = error.message;

        renderSource(source);

    } finally {

        refreshInProgress = false;
        updateConnectionStatus();
    }
}

/* ========================================
   PAUSE / RESUME
   ======================================== */

function togglePause() {

    logsPaused = !logsPaused;

    appLogsPauseButton.textContent =
        logsPaused ? "Resume" : "Pause";

    updateConnectionStatus();

    if (!logsPaused) {
        refreshSource(activeLogSource);
    }
}

/* ========================================
   CLEAR VIEW
   ======================================== */

function clearView() {

    const state = getActiveState();

    // Keep the last server snapshot as a
    // baseline for detecting new entries.
    //
    // Clear only the displayed lines.
    // Do not delete actual log files.

    state.visibleLines = [];
    state.cleared = true;
    state.error = null;

    renderSource(activeLogSource);
    updateConnectionStatus();
}

/* ========================================
   EVENT LISTENERS
   ======================================== */

automationLogsTab.addEventListener(
    "click",
    () => activateTab("automation")
);

parserLogsTab.addEventListener(
    "click",
    () => activateTab("parser")
);

appLogsPauseButton.addEventListener(
    "click",
    togglePause
);

appLogsClearButton.addEventListener(
    "click",
    clearView
);

/* ========================================
   KEYBOARD TAB NAVIGATION
   ======================================== */

const appLogsTabs = [
    automationLogsTab,
    parserLogsTab
];

appLogsTabs.forEach((tab, index) => {

    tab.addEventListener("keydown", event => {

        if (
            event.key !== "ArrowLeft" &&
            event.key !== "ArrowRight"
        ) {
            return;
        }

        event.preventDefault();

        const nextIndex =
            event.key === "ArrowRight"
                ? (index + 1) % appLogsTabs.length
                : (index - 1 + appLogsTabs.length)
                    % appLogsTabs.length;

        appLogsTabs[nextIndex].focus();

        activateTab(
            appLogsTabs[nextIndex].dataset.logSource
        );
    });
});

/* ========================================
   LIVE REFRESH
   ======================================== */

setInterval(() => {

    if (
        !logsPaused &&
        !document.hidden
    ) {
        refreshSource(activeLogSource);
    }

}, APP_LOGS_REFRESH_INTERVAL);

/* ========================================
   INITIAL LOAD
   ======================================== */

activateTab("automation");
