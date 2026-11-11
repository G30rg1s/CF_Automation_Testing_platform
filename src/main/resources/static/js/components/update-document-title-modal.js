/* ========================================
   ELEMENTS
   ======================================== */

const updateDocumentTitleModal =
    document.getElementById("updateDocumentTitleModal");

const closeUpdateDocumentTitleModalButton =
    document.getElementById("closeUpdateDocumentTitleModalButton");

const updateDocumentTitleForm =
    document.getElementById("updateDocumentTitleForm");

const updateDocumentTitleInput =
    document.getElementById("updateDocumentTitleInput");

const updateDocumentTitleError =
    document.getElementById("updateDocumentTitleError");

const updateDocumentTitleMessage =
    document.getElementById("updateDocumentTitleMessage");

const cancelUpdateDocumentTitleButton =
    document.getElementById("cancelUpdateDocumentTitleButton");

const saveUpdateDocumentTitleButton =
    document.getElementById("saveUpdateDocumentTitleButton");

const updateTitleActionButton =
    document.getElementById("updateDocumentTitleButton");


/* ========================================
   CURRENT TITLE
   ======================================== */

let currentDocumentTitle = "";


/* ========================================
   LOAD CURRENT DOCUMENT
   ======================================== */

async function loadCurrentDocumentTitle() {
    if (!window.currentDocumentId) {
        showUpdateDocumentTitleMessage(
            "Unable to identify document.",
            "error"
        );

        return;
    }

    clearUpdateDocumentTitleErrors();

    setUpdateDocumentTitleLoading(true);

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

            showUpdateDocumentTitleMessage(
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

        currentDocumentTitle =
            documentData.title || "";

        if (updateDocumentTitleInput) {

            updateDocumentTitleInput.value =
                currentDocumentTitle;

            updateDocumentTitleInput.focus();

            updateDocumentTitleInput.select();
        }

    } catch (error) {

        console.error(
            "Unable to load document title.",
            error
        );

        showUpdateDocumentTitleMessage(
            "Unable to load document.",
            "error"
        );

    } finally {

        setUpdateDocumentTitleLoading(false);
    }
}


/* ========================================
   UPDATE TITLE BUTTON
   ======================================== */

if (updateTitleActionButton) {

    updateTitleActionButton.addEventListener(
        "click",
        async () => {

            await loadCurrentDocumentTitle();
        }
    );
}


/* ========================================
   VALIDATE TITLE
   ======================================== */

function validateUpdateDocumentTitle() {
    if (!updateDocumentTitleInput) {
        return false;
    }

    const title =
        updateDocumentTitleInput.value.trim();

    clearUpdateDocumentTitleErrors();


    /* ========================================
       REQUIRED
       ======================================== */

    if (!title) {

        showUpdateDocumentTitleFieldError(
            "Document title is required."
        );

        updateDocumentTitleInput.focus();

        return false;
    }


    /* ========================================
       MAX LENGTH
       ======================================== */

    if (title.length > 255) {

        showUpdateDocumentTitleFieldError(
            "Document title cannot exceed 255 characters."
        );

        updateDocumentTitleInput.focus();

        return false;
    }


    return true;
}


/* ========================================
   SUBMIT
   ======================================== */

if (updateDocumentTitleForm) {

    updateDocumentTitleForm.addEventListener(
        "submit",
        async (event) => {

            event.preventDefault();


            /* ========================================
               DOCUMENT CHECK
               ======================================== */

            if (!window.currentDocumentId) {

                showUpdateDocumentTitleMessage(
                    "Unable to identify document.",
                    "error"
                );

                return;
            }


            /* ========================================
               VALIDATION
               ======================================== */

            if (!validateUpdateDocumentTitle()) {
                return;
            }


            const newTitle =
                updateDocumentTitleInput.value.trim();


            /* ========================================
               NO CHANGES
               ======================================== */

            if (newTitle === currentDocumentTitle) {

                showUpdateDocumentTitleMessage(
                    "The title has not changed.",
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
                newTitle
            );


            /* ========================================
               REQUEST
               ======================================== */

            setUpdateDocumentTitleLoading(true);

            clearUpdateDocumentTitleErrors();

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
                   DUPLICATE TITLE
                   ======================================== */

                if (response.status === 409) {

                    showUpdateDocumentTitleFieldError(
                        "A document with this title already exists."
                    );

                    return;
                }


                /* ========================================
                   BAD REQUEST
                   ======================================== */

                if (response.status === 400) {

                    showUpdateDocumentTitleFieldError(
                        "Please enter a valid document title."
                    );

                    return;
                }


                /* ========================================
                   RESPONSE CHECK
                   ======================================== */

                if (!response.ok) {

                    showUpdateDocumentTitleMessage(
                        "Unable to update document title.",
                        "error"
                    );

                    return;
                }


                /* ========================================
                   SUCCESS
                   ======================================== */

                currentDocumentTitle =
                    newTitle;

                showUpdateDocumentTitleMessage(
                    "Document title updated successfully.",
                    "success"
                );


                /* ========================================
                   RELOAD PAGE
                   ======================================== */

                window.setTimeout(
                    () => {
                        window.location.reload();
                    },
                    500
                );

            } catch (error) {

                console.error(
                    "Unable to update document title.",
                    error
                );

                showUpdateDocumentTitleMessage(
                    "Unable to update document title.",
                    "error"
                );

            } finally {

                setUpdateDocumentTitleLoading(false);
            }
        }
    );
}


/* ========================================
   FIELD ERROR
   ======================================== */

function showUpdateDocumentTitleFieldError(message) {
    if (!updateDocumentTitleError) {
        return;
    }

    updateDocumentTitleError.textContent =
        message;

    if (updateDocumentTitleInput) {

        updateDocumentTitleInput.classList.add(
            "input-error"
        );
    }
}


/* ========================================
   RESPONSE MESSAGE
   ======================================== */

function showUpdateDocumentTitleMessage(message, type) {
    if (!updateDocumentTitleMessage) {
        return;
    }

    updateDocumentTitleMessage.textContent =
        message;

    updateDocumentTitleMessage.classList.remove(
        "success",
        "error"
    );

    updateDocumentTitleMessage.classList.add(
        type
    );
}


/* ========================================
   CLEAR ERRORS
   ======================================== */

function clearUpdateDocumentTitleErrors() {
    if (updateDocumentTitleError) {

        updateDocumentTitleError.textContent =
            "";
    }

    if (updateDocumentTitleMessage) {

        updateDocumentTitleMessage.textContent =
            "";

        updateDocumentTitleMessage.classList.remove(
            "success",
            "error"
        );
    }

    if (updateDocumentTitleInput) {

        updateDocumentTitleInput.classList.remove(
            "input-error"
        );
    }
}


/* ========================================
   LOADING STATE
   ======================================== */

function setUpdateDocumentTitleLoading(isLoading) {
    if (saveUpdateDocumentTitleButton) {

        saveUpdateDocumentTitleButton.disabled =
            isLoading;

        saveUpdateDocumentTitleButton.textContent =
            isLoading
                ? "Updating..."
                : "Update Title";
    }

    if (cancelUpdateDocumentTitleButton) {

        cancelUpdateDocumentTitleButton.disabled =
            isLoading;
    }

    if (closeUpdateDocumentTitleModalButton) {

        closeUpdateDocumentTitleModalButton.disabled =
            isLoading;
    }

    if (updateDocumentTitleInput) {

        updateDocumentTitleInput.disabled =
            isLoading;
    }
}


/* ========================================
   CLOSE MODAL
   ======================================== */

function closeUpdateDocumentTitleModal() {
    if (!updateDocumentTitleModal) {
        return;
    }

    updateDocumentTitleModal.classList.remove(
        "active"
    );

    updateDocumentTitleModal.setAttribute(
        "aria-hidden",
        "true"
    );

    clearUpdateDocumentTitleErrors();
}


/* ========================================
   CLOSE BUTTON
   ======================================== */

if (closeUpdateDocumentTitleModalButton) {

    closeUpdateDocumentTitleModalButton.addEventListener(
        "click",
        () => {

            closeUpdateDocumentTitleModal();
        }
    );
}


/* ========================================
   CANCEL BUTTON
   ======================================== */

if (cancelUpdateDocumentTitleButton) {

    cancelUpdateDocumentTitleButton.addEventListener(
        "click",
        () => {

            closeUpdateDocumentTitleModal();
        }
    );
}


/* ========================================
   CLEAR FIELD ERROR ON INPUT
   ======================================== */

if (updateDocumentTitleInput) {

    updateDocumentTitleInput.addEventListener(
        "input",
        () => {

            if (updateDocumentTitleError) {

                updateDocumentTitleError.textContent =
                    "";
            }

            updateDocumentTitleInput.classList.remove(
                "input-error"
            );
        }
    );
}


/* ========================================
   CLOSE WHEN CLICKING BACKDROP
   ======================================== */

if (updateDocumentTitleModal) {

    updateDocumentTitleModal.addEventListener(
        "click",
        (event) => {

            if (
                event.target ===
                updateDocumentTitleModal
            ) {

                closeUpdateDocumentTitleModal();
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
            updateDocumentTitleModal &&
            updateDocumentTitleModal.classList.contains(
                "active"
            )
        ) {

            closeUpdateDocumentTitleModal();
        }
    }
);