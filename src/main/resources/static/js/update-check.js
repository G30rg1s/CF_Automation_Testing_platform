const UPDATE_CHECK_INTERVAL_MS = 60000;
const UPDATE_LATER_STORAGE_KEY = "automationPlatformUpdateLaterVersion";

async function checkForApplicationUpdate() {

    try {

        const response = await fetch("/api/update/status", {
            method: "GET",
            credentials: "same-origin"
        });

        if (!response.ok) {
            return;
        }

        const updateStatus = await response.json();

        if (!updateStatus.updateAvailable) {
            return;
        }

        if (!updateStatus.latestVersion) {
            return;
        }

        const ignoredVersion =
            sessionStorage.getItem(UPDATE_LATER_STORAGE_KEY);

        if (ignoredVersion === updateStatus.latestVersion) {
            return;
        }

        showApplicationUpdateModal(updateStatus);

    } catch (error) {
        // Update checking must never interrupt normal application usage.
    }
}


function showApplicationUpdateModal(updateStatus) {

    const modal =
        document.getElementById("applicationUpdateModal");

    const currentVersionElement =
        document.getElementById("applicationCurrentVersion");

    const latestVersionElement =
        document.getElementById("applicationLatestVersion");

    const laterButton =
        document.getElementById("applicationUpdateLaterButton");

    const updateButton =
        document.getElementById("applicationUpdateButton");

    if (
        !modal ||
        !currentVersionElement ||
        !latestVersionElement ||
        !laterButton ||
        !updateButton
    ) {
        return;
    }

    currentVersionElement.textContent =
        updateStatus.currentVersion ?? "-";

    latestVersionElement.textContent =
        updateStatus.latestVersion;

    modal.hidden = false;


    laterButton.onclick = () => {

        sessionStorage.setItem(
            UPDATE_LATER_STORAGE_KEY,
            updateStatus.latestVersion
        );

        modal.hidden = true;
    };


    updateButton.onclick = () => {

        modal.hidden = true;

        alert(
            "Automatic update will be handled by the Automation Platform Launcher."
        );
    };
}


document.addEventListener("DOMContentLoaded", () => {

    checkForApplicationUpdate();

    setInterval(
        checkForApplicationUpdate,
        UPDATE_CHECK_INTERVAL_MS
    );
});