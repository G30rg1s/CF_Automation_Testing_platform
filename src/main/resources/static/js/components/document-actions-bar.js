/* ========================================
   ELEMENTS
   ======================================== */

const updateDocumentTitleButton =
    document.getElementById("updateDocumentTitleButton");

const reuploadDocumentButton =
    document.getElementById("reuploadDocumentButton");

const formatDocumentButton =
    document.getElementById("formatDocumentButton");

const deleteDocumentButton =
    document.getElementById("deleteDocumentButton");


/* ========================================
   DOCUMENT ID FROM URL
   ======================================== */

function getDocumentIdFromUrl() {
    const pathParts =
        window.location.pathname
            .split("/")
            .filter(Boolean);

    const documentsIndex =
        pathParts.indexOf("documents");

    if (documentsIndex === -1) {
        return null;
    }

    const documentId =
        pathParts[documentsIndex + 1];

    if (!documentId || !/^\d+$/.test(documentId)) {
        return null;
    }

    return documentId;
}


const currentDocumentId =
    getDocumentIdFromUrl();


/* ========================================
   OPEN MODAL
   ======================================== */

function openDocumentActionModal(modalId) {
    const modal =
        document.getElementById(modalId);

    if (!modal) {
        console.warn(
            `Modal not found: ${modalId}`
        );

        return;
    }

    modal.classList.add("active");

    modal.setAttribute(
        "aria-hidden",
        "false"
    );
}


/* ========================================
   UPDATE TITLE
   ======================================== */

if (updateDocumentTitleButton) {

    updateDocumentTitleButton.addEventListener(
        "click",
        () => {

            if (!currentDocumentId) {
                return;
            }

            openDocumentActionModal(
                "updateDocumentTitleModal"
            );
        }
    );
}


/* ========================================
   REUPLOAD DOCUMENT
   ======================================== */

if (reuploadDocumentButton) {

    reuploadDocumentButton.addEventListener(
        "click",
        () => {

            if (!currentDocumentId) {
                return;
            }

            openDocumentActionModal(
                "reuploadDocumentModal"
            );
        }
    );
}


/* ========================================
   FORMAT DOCUMENT
   ======================================== */

if (formatDocumentButton) {

    formatDocumentButton.addEventListener(
        "click",
        () => {

            if (!currentDocumentId) {
                return;
            }

            openDocumentActionModal(
                "formatDocumentModal"
            );
        }
    );
}


/* ========================================
   DELETE DOCUMENT BUTTON
   ======================================== */

if (deleteDocumentButton) {

    deleteDocumentButton.addEventListener(
        "click",
        () => {

            if (!currentDocumentId) {
                return;
            }

            openDocumentActionModal(
                "deleteDocumentModal"
            );
        }
    );
}


/* ========================================
   DELETE DOCUMENT
   ======================================== */

async function deleteDocument() {
    if (!currentDocumentId) {
        return false;
    }

    try {

        const response =
            await fetch(
                `/cf_automation/documents/${currentDocumentId}`,
                {
                    method: "DELETE",
                    credentials: "same-origin"
                }
            );


        /* ========================================
           AUTH CHECK
           ======================================== */

        if (
            response.status === 401 ||
            response.status === 403
        ) {

            window.location.href =
                "/automation/unauthorized";

            return false;
        }


        /* ========================================
           RESPONSE CHECK
           ======================================== */

        if (!response.ok) {

            console.error(
                "Unable to delete document."
            );

            return false;
        }


        /* ========================================
           DELETE SUCCESS
           ======================================== */

        window.location.href =
            "/automation/dashboard";

        return true;

    } catch (error) {

        console.error(
            "Unable to delete document.",
            error
        );

        return false;
    }
}


/* ========================================
   MAKE AVAILABLE TO OTHER COMPONENTS
   ======================================== */

window.currentDocumentId =
    currentDocumentId;

window.deleteDocument =
    deleteDocument;