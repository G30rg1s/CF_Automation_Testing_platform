const totalDocumentsCount =
    document.getElementById("totalDocumentsCount");

const categoriesCount =
    document.getElementById("categoriesCount");

const readyDocumentsCount =
    document.getElementById("readyDocumentsCount");

const pendingDocumentsCount =
    document.getElementById("pendingDocumentsCount");


/* ========================================
   LOAD DASHBOARD OVERVIEW
   ======================================== */

async function loadDashboardOverview() {

    try {

        const statsResponse = await fetch(
            "/cf_automation/dashboard/stats",
            {
                method: "GET",
                credentials: "same-origin"
            }
        );


        /* ========================================
           AUTH CHECK
           ======================================== */

        if (
            statsResponse.status === 401 ||
            statsResponse.status === 403
        ) {

            window.location.href =
                "/automation/unauthorized";

            return;
        }


        /* ========================================
           RESPONSE CHECK
           ======================================== */

        if (!statsResponse.ok) {

            showUnavailableCounters();

            return;
        }


        /* ========================================
           READ DATA
           ======================================== */

        const dashboardStats =
            await statsResponse.json();


        /* ========================================
           DISPLAY TOTAL DOCUMENTS
           ======================================== */

        if (totalDocumentsCount) {

            totalDocumentsCount.textContent =
                dashboardStats.totalDocuments;
        }


        /* ========================================
           DISPLAY CATEGORIES
           ======================================== */

        if (categoriesCount) {

            categoriesCount.textContent =
                dashboardStats.totalCategories;
        }


        /* ========================================
           READY / PENDING NOT USED YET
           ======================================== */

        if (readyDocumentsCount) {
            readyDocumentsCount.textContent = "-";
        }

        if (pendingDocumentsCount) {
            pendingDocumentsCount.textContent = "-";
        }


    } catch (error) {

        showUnavailableCounters();

        console.error(
            "Unable to load dashboard overview.",
            error
        );
    }
}


/* ========================================
   FALLBACK
   ======================================== */

function showUnavailableCounters() {

    if (totalDocumentsCount) {
        totalDocumentsCount.textContent = "-";
    }

    if (categoriesCount) {
        categoriesCount.textContent = "-";
    }

    if (readyDocumentsCount) {
        readyDocumentsCount.textContent = "-";
    }

    if (pendingDocumentsCount) {
        pendingDocumentsCount.textContent = "-";
    }
}


/* ========================================
   MAKE AVAILABLE TO OTHER COMPONENTS
   ======================================== */

window.loadDashboardOverview =
    loadDashboardOverview;


/* ========================================
   INITIAL LOAD
   ======================================== */

loadDashboardOverview();