document.addEventListener("DOMContentLoaded", () => {

    /* ========================================
       ELEMENTS
       ======================================== */

    const uploadDocumentForm =
        document.getElementById("uploadDocumentForm");

    const documentTitle =
        document.getElementById("documentTitle");

    const documentTitleError =
        document.getElementById("documentTitleError");

    const documentDropZone =
        document.getElementById("documentDropZone");

    const documentFile =
        document.getElementById("documentFile");

    const selectedFileName =
        document.getElementById("selectedFileName");

    const documentFileError =
        document.getElementById("documentFileError");

    const uploadDocumentMessage =
        document.getElementById("uploadDocumentMessage");

    const clearUploadFormButton =
        document.getElementById("clearUploadFormButton");

    const uploadDocumentButton =
        document.getElementById("uploadDocumentButton");


    /* ========================================
       SIDEBAR ACTIVE ITEM
       ======================================== */

    const dashboardNavigation =
        document.getElementById("dashboardNavigation");

    const profileNavigation =
        document.getElementById("profileNavigation");

    const uploadNavigation =
        document.getElementById("uploadNavigation");


    if (dashboardNavigation) {
        dashboardNavigation.classList.remove("active");
    }

    if (profileNavigation) {
        profileNavigation.classList.remove("active");
    }

    if (uploadNavigation) {
        uploadNavigation.classList.add("active");
    }


    /* ========================================
       SELECTED FILE
       ======================================== */

    let selectedFile = null;


    /* ========================================
       DISPLAY SELECTED FILE
       ======================================== */

    function displaySelectedFile(file) {

        selectedFile = file;

        if (!file) {
            selectedFileName.textContent = "No file selected";
            return;
        }

        selectedFileName.textContent = file.name;

        documentFileError.textContent = "";
        documentDropZone.classList.remove("input-error");
    }


    /* ========================================
       FILE INPUT CHANGE
       ======================================== */

    if (documentFile) {

        documentFile.addEventListener("change", () => {

            const file =
                documentFile.files?.[0] ?? null;

            displaySelectedFile(file);
        });
    }


    /* ========================================
       DRAG OVER
       ======================================== */

    if (documentDropZone) {

        documentDropZone.addEventListener("dragover", event => {

            event.preventDefault();

            documentDropZone.classList.add("drag-over");
        });


        /* ========================================
           DRAG LEAVE
           ======================================== */

        documentDropZone.addEventListener("dragleave", () => {

            documentDropZone.classList.remove("drag-over");
        });


        /* ========================================
           DROP FILE
           ======================================== */

        documentDropZone.addEventListener("drop", event => {

            event.preventDefault();

            documentDropZone.classList.remove("drag-over");

            const files =
                event.dataTransfer?.files;

            if (!files || files.length === 0) {
                return;
            }

            displaySelectedFile(files[0]);
        });
    }


    /* ========================================
       CLEAR TITLE ERROR WHILE TYPING
       ======================================== */

    if (documentTitle) {

        documentTitle.addEventListener("input", () => {

            documentTitleError.textContent = "";
            documentTitle.classList.remove("input-error");
        });
    }


    /* ========================================
       CLEAR FORM
       ======================================== */

    function clearUploadForm() {

        uploadDocumentForm.reset();

        selectedFile = null;

        selectedFileName.textContent =
            "No file selected";

        documentTitleError.textContent = "";
        documentFileError.textContent = "";

        documentTitle.classList.remove("input-error");

        documentDropZone.classList.remove(
            "input-error",
            "drag-over"
        );

        uploadDocumentMessage.textContent = "";

        uploadDocumentMessage.classList.remove(
            "success",
            "error"
        );

        documentTitle.focus();
    }


    /* ========================================
       CLEAR BUTTON
       ======================================== */

    if (clearUploadFormButton) {

        clearUploadFormButton.addEventListener(
            "click",
            clearUploadForm
        );
    }


    /* ========================================
       VALIDATE FORM
       ======================================== */

    function validateUploadForm() {

        let isValid = true;

        const title =
            documentTitle.value.trim();


        documentTitleError.textContent = "";
        documentTitle.classList.remove("input-error");


        /* ========================================
           TITLE REQUIRED
           ======================================== */

        if (!title) {

            documentTitleError.textContent =
                "Title is required.";

            documentTitle.classList.add(
                "input-error"
            );

            isValid = false;

        } else if (title.length > 255) {

            documentTitleError.textContent =
                "Title must not exceed 255 characters.";

            documentTitle.classList.add(
                "input-error"
            );

            isValid = false;
        }


        return isValid;
    }


    /* ========================================
       READ ERROR MESSAGE
       ======================================== */

    async function readErrorMessage(response) {

        const defaultMessage =
            "Unable to create document.";

        try {

            const contentType =
                response.headers.get("content-type") ?? "";


            if (contentType.includes("application/json")) {

                const errorData =
                    await response.json();

                return errorData.message
                    || errorData.error
                    || defaultMessage;
            }


            const text =
                await response.text();

            return text || defaultMessage;

        } catch {

            return defaultMessage;
        }
    }


    /* ========================================
       RESET AFTER SUCCESS
       ======================================== */

    function resetAfterSuccessfulUpload() {

        uploadDocumentForm.reset();

        selectedFile = null;

        selectedFileName.textContent =
            "No file selected";

        documentTitleError.textContent = "";
        documentFileError.textContent = "";

        documentTitle.classList.remove("input-error");

        documentDropZone.classList.remove(
            "input-error",
            "drag-over"
        );
    }


    /* ========================================
       CREATE DOCUMENT
       ======================================== */

    if (uploadDocumentForm) {

        uploadDocumentForm.addEventListener(
            "submit",
            async event => {

                event.preventDefault();


                uploadDocumentMessage.textContent = "";

                uploadDocumentMessage.classList.remove(
                    "success",
                    "error"
                );


                /* ========================================
                   FRONTEND VALIDATION
                   ======================================== */

                if (!validateUploadForm()) {
                    return;
                }


                /* ========================================
                   FORM DATA
                   ======================================== */

                const formData =
                    new FormData();


                formData.append(
                    "title",
                    documentTitle.value.trim()
                );


                /*
                 * File is optional.
                 * Append it only when the user selected one.
                 */

                if (selectedFile) {

                    formData.append(
                        "file",
                        selectedFile
                    );
                }


                /* ========================================
                   DISABLE BUTTON
                   ======================================== */

                uploadDocumentButton.disabled = true;

                uploadDocumentButton.textContent =
                    "Creating...";


                try {

                    /* ========================================
                       CREATE DOCUMENT
                       ======================================== */

                    const response = await fetch(
                        "/cf_automation/documents/upload",
                        {
                            method: "POST",
                            credentials: "same-origin",
                            body: formData
                        }
                    );


                    /* ========================================
                       UNAUTHORIZED
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
                       BACKEND ERROR
                       ======================================== */

                    if (!response.ok) {

                        const message =
                            await readErrorMessage(response);

                        uploadDocumentMessage.textContent =
                            message;

                        uploadDocumentMessage.classList.add(
                            "error"
                        );

                        return;
                    }


                    /* ========================================
                       SUCCESS
                       ======================================== */

                    if (selectedFile) {

                        uploadDocumentMessage.textContent =
                            "Document created successfully and sent for processing.";

                    } else {

                        uploadDocumentMessage.textContent =
                            "Document created successfully.";
                    }


                    uploadDocumentMessage.classList.add(
                        "success"
                    );


                    resetAfterSuccessfulUpload();


                } catch (error) {

                    uploadDocumentMessage.textContent =
                        "Unable to connect to the server.";

                    uploadDocumentMessage.classList.add(
                        "error"
                    );


                    console.error(
                        "Unable to create document.",
                        error
                    );


                } finally {

                    uploadDocumentButton.disabled =
                        false;

                    uploadDocumentButton.textContent =
                        "Create Document";
                }
            }
        );
    }


    /* ========================================
       INITIAL STATE
       ======================================== */

    documentTitle.focus();

});