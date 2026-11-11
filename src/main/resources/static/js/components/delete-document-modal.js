/* ========================================
   ELEMENTS
   ======================================== */

const deleteDocumentModal =
    document.getElementById("deleteDocumentModal");

const closeDeleteDocumentModalButton =
    document.getElementById("closeDeleteDocumentModalButton");

const cancelDeleteDocumentButton =
    document.getElementById("cancelDeleteDocumentButton");

const confirmDeleteDocumentButton =
    document.getElementById("confirmDeleteDocumentButton");

const deleteDocumentMessage =
    document.getElementById("deleteDocumentMessage");


/* ========================================
   CLOSE MODAL
   ======================================== */

function closeDeleteDocumentModal() {
    if (!deleteDocumentModal) {
        return;
    }

    deleteDocumentModal.classList.remove("active");

    deleteDocumentModal.setAttribute(
        "aria-hidden",
        "true"
    );

    clearDeleteDocumentMessage();
}


/* ========================================
   CLEAR MESSAGE
   ======================================== */

function clearDeleteDocumentMessage() {
    if (!deleteDocumentMessage) {
        return;
    }

    deleteDocumentMessage.textContent = "";

    deleteDocumentMessage.classList.remove(
        "success",
        "error"
    );
}


/* ========================================
   SHOW ERROR
   ======================================== */

function showDeleteDocumentError(message) {
    if (!deleteDocumentMessage) {
        return;
    }

    deleteDocumentMessage.textContent = message;

    deleteDocumentMessage.classList.remove(
        "success"
    );

    deleteDocumentMessage.classList.add(
        "error"
    );
}


/* ========================================
   SET LOADING STATE
   ======================================== */

function setDeleteDocumentLoading(isLoading) {
    if (confirmDeleteDocumentButton) {
        confirmDeleteDocumentButton.disabled =
            isLoading;

        confirmDeleteDocumentButton.textContent =
            isLoading
                ? "Deleting..."
                : "Delete Document";
    }

    if (cancelDeleteDocumentButton) {
        cancelDeleteDocumentButton.disabled =
            isLoading;
    }

    if (closeDeleteDocumentModalButton) {
        closeDeleteDocumentModalButton.disabled =
            isLoading;
    }
}


/* ========================================
   CLOSE BUTTON
   ======================================== */

if (closeDeleteDocumentModalButton) {

    closeDeleteDocumentModalButton.addEventListener(
        "click",
        () => {
            closeDeleteDocumentModal();
        }
    );
}


/* ========================================
   CANCEL BUTTON
   ======================================== */

if (cancelDeleteDocumentButton) {

    cancelDeleteDocumentButton.addEventListener(
        "click",
        () => {
            closeDeleteDocumentModal();
        }
    );
}


/* ========================================
   CONFIRM DELETE
   ======================================== */

if (confirmDeleteDocumentButton) {

    confirmDeleteDocumentButton.addEventListener(
        "click",
        async () => {

            clearDeleteDocumentMessage();

            if (
                typeof window.deleteDocument !==
                "function"
            ) {

                showDeleteDocumentError(
                    "Unable to delete document."
                );

                return;
            }

            setDeleteDocumentLoading(true);

            const deleted =
                await window.deleteDocument();

            if (!deleted) {

                setDeleteDocumentLoading(false);

                showDeleteDocumentError(
                    "Unable to delete document."
                );
            }
        }
    );
}


/* ========================================
   CLOSE WHEN CLICKING BACKDROP
   ======================================== */

if (deleteDocumentModal) {

    deleteDocumentModal.addEventListener(
        "click",
        (event) => {

            if (
                event.target ===
                deleteDocumentModal
            ) {

                closeDeleteDocumentModal();
            }
        }
    );
}


/* ========================================
   CLOSE WITH ESCAPE
   ======================================== */

document.addEventListener(
    "keydown",
    (event) => {

        if (
            event.key === "Escape" &&
            deleteDocumentModal &&
            deleteDocumentModal.classList.contains(
                "active"
            )
        ) {

            closeDeleteDocumentModal();
        }
    }
);