/* ========================================
   ELEMENTS
   ======================================== */

const reuploadDocumentModal =
    document.getElementById("reuploadDocumentModal");

const closeReuploadDocumentModalButton =
    document.getElementById("closeReuploadDocumentModalButton");

const reuploadDocumentForm =
    document.getElementById("reuploadDocumentForm");

const reuploadDocumentTitleInput =
    document.getElementById("reuploadDocumentTitleInput");

const reuploadDocumentFileInput =
    document.getElementById("reuploadDocumentFileInput");

const reuploadDocumentFileName =
    document.getElementById("reuploadDocumentFileName");

const reuploadDocumentTitleError =
    document.getElementById("reuploadDocumentTitleError");

const reuploadDocumentMessage =
    document.getElementById("reuploadDocumentMessage");

const cancelReuploadDocumentButton =
    document.getElementById("cancelReuploadDocumentButton");

const saveReuploadDocumentButton =
    document.getElementById("saveReuploadDocumentButton");

const reuploadActionButton =
    document.getElementById("reuploadDocumentButton");


/* ========================================
   STATE
   ======================================== */

let currentReuploadDocumentTitle = "";

let reuploadDocumentIsPending = false;

let reuploadDocumentIsLoading = false;


/* ========================================
   LOAD CURRENT DOCUMENT
   ======================================== */

async function loadReuploadDocumentData() {
    if (!window.currentDocumentId) {
        showReuploadDocumentMessage(
            "Unable to identify document.",
            "error"
        );

        return;
    }

    clearReuploadDocumentErrors();
    resetReuploadDocumentFile();

    reuploadDocumentIsPending = false;

    setReuploadDocumentLoading(true);

    try {

        const response =
            await fetch(
                `/cf_automation/documents/${window.currentDocumentId}`,
                {
                    method: "GET",
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

            return;
        }


        /* ========================================
           RESPONSE CHECK
           ======================================== */

        if (!response.ok) {

            showReuploadDocumentMessage(
                "Unable to load document.",
                "error"
            );

            return;
        }


        /* ========================================
           READ DOCUMENT
           ======================================== */

        const documentData =
            await response.json();

        currentReuploadDocumentTitle =
            documentData.title || "";

        if (reuploadDocumentTitleInput) {

            reuploadDocumentTitleInput.value =
                currentReuploadDocumentTitle;
        }


        /* ========================================
           PENDING CHECK
           ======================================== */

        if (documentData.status === "PENDING") {

            reuploadDocumentIsPending = true;

            showReuploadDocumentMessage(
                "This document is currently being processed and cannot be reuploaded.",
                "error"
            );

            return;
        }


        /* ========================================
           READY FOR REUPLOAD
           ======================================== */

        reuploadDocumentIsPending = false;

        if (reuploadDocumentTitleInput) {

            reuploadDocumentTitleInput.focus();
        }

    } catch (error) {

        console.error(
            "Unable to load document.",
            error
        );

        showReuploadDocumentMessage(
            "Unable to load document.",
            "error"
        );

    } finally {

        setReuploadDocumentLoading(false);
    }
}


/* ========================================
   REUPLOAD ACTION BUTTON
   ======================================== */

if (reuploadActionButton) {

    reuploadActionButton.addEventListener(
        "click",
        async () => {

            await loadReuploadDocumentData();
        }
    );
}


/* ========================================
   FILE CHANGE
   ======================================== */

if (reuploadDocumentFileInput) {

    reuploadDocumentFileInput.addEventListener(
        "change",
        () => {

            const file =
                reuploadDocumentFileInput.files[0];

            if (!reuploadDocumentFileName) {
                return;
            }

            if (!file) {

                reuploadDocumentFileName.textContent =
                    "No file selected";

                return;
            }

            reuploadDocumentFileName.textContent =
                file.name;
        }
    );
}


/* ========================================
   VALIDATE TITLE
   ======================================== */

function validateReuploadDocumentTitle() {
    if (!reuploadDocumentTitleInput) {
        return false;
    }

    const title =
        reuploadDocumentTitleInput.value.trim();

    clearReuploadDocumentErrors();


    /* ========================================
       REQUIRED
       ======================================== */

    if (!title) {

        showReuploadDocumentTitleError(
            "Document title is required."
        );

        reuploadDocumentTitleInput.focus();

        return false;
    }


    /* ========================================
       MAX LENGTH
       ======================================== */

    if (title.length > 255) {

        showReuploadDocumentTitleError(
            "Document title cannot exceed 255 characters."
        );

        reuploadDocumentTitleInput.focus();

        return false;
    }

    return true;
}


/* ========================================
   SUBMIT
   ======================================== */

if (reuploadDocumentForm) {

    reuploadDocumentForm.addEventListener(
        "submit",
        async (event) => {

            event.preventDefault();


            /* ========================================
               DOCUMENT CHECK
               ======================================== */

            if (!window.currentDocumentId) {

                showReuploadDocumentMessage(
                    "Unable to identify document.",
                    "error"
                );

                return;
            }


            /* ========================================
               PENDING CHECK
               ======================================== */

            if (reuploadDocumentIsPending) {

                showReuploadDocumentMessage(
                    "This document is currently being processed and cannot be reuploaded.",
                    "error"
                );

                return;
            }


            /* ========================================
               VALIDATION
               ======================================== */

            if (!validateReuploadDocumentTitle()) {
                return;
            }


            /* ========================================
               VALUES
               ======================================== */

            const title =
                reuploadDocumentTitleInput.value.trim();

            const file =
                reuploadDocumentFileInput &&
                reuploadDocumentFileInput.files.length > 0
                    ? reuploadDocumentFileInput.files[0]
                    : null;


            /* ========================================
               NOTHING CHANGED
               ======================================== */

            if (
                title === currentReuploadDocumentTitle &&
                !file
            ) {

                showReuploadDocumentMessage(
                    "No changes were made.",
                    "error"
                );

                return;
            }


            /* ========================================
               FORM DATA
               ======================================== */

            const formData =
                new FormData();

            formData.append(
                "title",
                title
            );

            if (file) {

                formData.append(
                    "file",
                    file
                );
            }


            /* ========================================
               REQUEST
               ======================================== */

            clearReuploadDocumentErrors();

            setReuploadDocumentLoading(true);

            try {

                const response =
                    await fetch(
                        `/cf_automation/documents/${window.currentDocumentId}/reupload`,
                        {
                            method: "POST",
                            credentials: "same-origin",
                            body: formData
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

                    return;
                }


                /* ========================================
                   DOCUMENT PENDING
                   ======================================== */

                if (response.status === 423) {

                    reuploadDocumentIsPending = true;

                    showReuploadDocumentMessage(
                        "This document is currently being processed and cannot be reuploaded.",
                        "error"
                    );

                    return;
                }


                /* ========================================
                   DUPLICATE TITLE
                   ======================================== */

                if (response.status === 409) {

                    showReuploadDocumentTitleError(
                        "A document with this title already exists."
                    );

                    return;
                }


                /* ========================================
                   BAD REQUEST
                   ======================================== */

                if (response.status === 400) {

                    showReuploadDocumentMessage(
                        "Please check the document data.",
                        "error"
                    );

                    return;
                }


                /* ========================================
                   RESPONSE CHECK
                   ======================================== */

                if (!response.ok) {

                    showReuploadDocumentMessage(
                        "Unable to update document.",
                        "error"
                    );

                    return;
                }


                /* ========================================
                   SUCCESS
                   ======================================== */

                currentReuploadDocumentTitle =
                    title;

                showReuploadDocumentMessage(
                    file
                        ? "Document reuploaded successfully."
                        : "Document updated successfully.",
                    "success"
                );


                /* ========================================
                   RELOAD PAGE
                   ======================================== */

                window.setTimeout(
                    () => {

                        window.location.reload();
                    },
                    600
                );

            } catch (error) {

                console.error(
                    "Unable to update document.",
                    error
                );

                showReuploadDocumentMessage(
                    "Unable to update document.",
                    "error"
                );

            } finally {

                setReuploadDocumentLoading(false);
            }
        }
    );
}


/* ========================================
   TITLE ERROR
   ======================================== */

function showReuploadDocumentTitleError(message) {
    if (!reuploadDocumentTitleError) {
        return;
    }

    reuploadDocumentTitleError.textContent =
        message;

    if (reuploadDocumentTitleInput) {

        reuploadDocumentTitleInput.classList.add(
            "input-error"
        );
    }
}


/* ========================================
   RESPONSE MESSAGE
   ======================================== */

function showReuploadDocumentMessage(message, type) {
    if (!reuploadDocumentMessage) {
        return;
    }

    reuploadDocumentMessage.textContent =
        message;

    reuploadDocumentMessage.classList.remove(
        "success",
        "error"
    );

    reuploadDocumentMessage.classList.add(
        type
    );
}


/* ========================================
   CLEAR ERRORS
   ======================================== */

function clearReuploadDocumentErrors() {
    if (reuploadDocumentTitleError) {

        reuploadDocumentTitleError.textContent =
            "";
    }

    if (reuploadDocumentMessage) {

        reuploadDocumentMessage.textContent =
            "";

        reuploadDocumentMessage.classList.remove(
            "success",
            "error"
        );
    }

    if (reuploadDocumentTitleInput) {

        reuploadDocumentTitleInput.classList.remove(
            "input-error"
        );
    }
}


/* ========================================
   RESET FILE
   ======================================== */

function resetReuploadDocumentFile() {
    if (reuploadDocumentFileInput) {

        reuploadDocumentFileInput.value =
            "";
    }

    if (reuploadDocumentFileName) {

        reuploadDocumentFileName.textContent =
            "No file selected";
    }
}


/* ========================================
   UPDATE CONTROL STATE
   ======================================== */

function updateReuploadDocumentControls() {
    const disabled =
        reuploadDocumentIsLoading ||
        reuploadDocumentIsPending;

    if (reuploadDocumentTitleInput) {

        reuploadDocumentTitleInput.disabled =
            disabled;
    }

    if (reuploadDocumentFileInput) {

        reuploadDocumentFileInput.disabled =
            disabled;
    }

    if (saveReuploadDocumentButton) {

        saveReuploadDocumentButton.disabled =
            disabled;

        if (reuploadDocumentIsLoading) {

            saveReuploadDocumentButton.textContent =
                "Saving...";

        } else if (reuploadDocumentIsPending) {

            saveReuploadDocumentButton.textContent =
                "Processing...";

        } else {

            saveReuploadDocumentButton.textContent =
                "Save Changes";
        }
    }

    if (cancelReuploadDocumentButton) {

        cancelReuploadDocumentButton.disabled =
            reuploadDocumentIsLoading;
    }

    if (closeReuploadDocumentModalButton) {

        closeReuploadDocumentModalButton.disabled =
            reuploadDocumentIsLoading;
    }
}


/* ========================================
   LOADING STATE
   ======================================== */

function setReuploadDocumentLoading(isLoading) {
    reuploadDocumentIsLoading =
        isLoading;

    updateReuploadDocumentControls();
}


/* ========================================
   CLOSE MODAL
   ======================================== */

function closeReuploadDocumentModal() {
    if (!reuploadDocumentModal) {
        return;
    }

    if (reuploadDocumentIsLoading) {
        return;
    }

    reuploadDocumentModal.classList.remove(
        "active"
    );

    reuploadDocumentModal.setAttribute(
        "aria-hidden",
        "true"
    );

    currentReuploadDocumentTitle =
        "";

    reuploadDocumentIsPending =
        false;

    reuploadDocumentIsLoading =
        false;

    if (reuploadDocumentTitleInput) {

        reuploadDocumentTitleInput.value =
            "";
    }

    clearReuploadDocumentErrors();

    resetReuploadDocumentFile();

    updateReuploadDocumentControls();
}


/* ========================================
   CLOSE BUTTON
   ======================================== */

if (closeReuploadDocumentModalButton) {

    closeReuploadDocumentModalButton.addEventListener(
        "click",
        () => {

            closeReuploadDocumentModal();
        }
    );
}


/* ========================================
   CANCEL BUTTON
   ======================================== */

if (cancelReuploadDocumentButton) {

    cancelReuploadDocumentButton.addEventListener(
        "click",
        () => {

            closeReuploadDocumentModal();
        }
    );
}


/* ========================================
   CLEAR TITLE ERROR ON INPUT
   ======================================== */

if (reuploadDocumentTitleInput) {

    reuploadDocumentTitleInput.addEventListener(
        "input",
        () => {

            if (reuploadDocumentTitleError) {

                reuploadDocumentTitleError.textContent =
                    "";
            }

            reuploadDocumentTitleInput.classList.remove(
                "input-error"
            );
        }
    );
}


/* ========================================
   CLOSE WHEN CLICKING BACKDROP
   ======================================== */

if (reuploadDocumentModal) {

    reuploadDocumentModal.addEventListener(
        "click",
        (event) => {

            if (
                event.target ===
                reuploadDocumentModal
            ) {

                closeReuploadDocumentModal();
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
            reuploadDocumentModal &&
            reuploadDocumentModal.classList.contains(
                "active"
            )
        ) {

            closeReuploadDocumentModal();
        }
    }
);