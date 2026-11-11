/* ========================================
   ELEMENTS
   ======================================== */

const documentPageTitle =
    document.getElementById(
        "documentPageTitle"
    );

const documentInfoTitle =
    document.getElementById(
        "documentInfoTitle"
    );

const documentInfoCategory =
    document.getElementById(
        "documentInfoCategory"
    );

const documentInfoStatus =
    document.getElementById(
        "documentInfoStatus"
    );

const documentInfoId =
    document.getElementById(
        "documentInfoId"
    );

const documentLoading =
    document.getElementById(
        "documentLoading"
    );

const documentLoadError =
    document.getElementById(
        "documentLoadError"
    );

const documentLoadErrorMessage =
    document.getElementById(
        "documentLoadErrorMessage"
    );

const documentPendingState =
    document.getElementById(
        "documentPendingState"
    );

const documentFailedState =
    document.getElementById(
        "documentFailedState"
    );

const documentFailedMessage =
    document.getElementById(
        "documentFailedMessage"
    );

const documentEmptyState =
    document.getElementById(
        "documentEmptyState"
    );

const documentContentContainer =
    document.getElementById(
        "documentContentContainer"
    );

const documentContentFrame =
    document.getElementById(
        "documentContentFrame"
    );

const documentDetailsMessage =
    document.getElementById(
        "documentDetailsMessage"
    );

const documentReuploadButton =
    document.getElementById(
        "reuploadDocumentButton"
    );

const documentFormatButton =
    document.getElementById(
        "formatDocumentButton"
    );

const documentFullscreenActionButton =
    document.getElementById(
        "openDocumentFullscreenButton"
    );


/* ========================================
   CATEGORY ELEMENTS
   ======================================== */

const documentCategoryInfoItem =
    document.querySelector(
        ".document-category-info-item"
    );

const changeDocumentCategoryButton =
    document.getElementById(
        "changeDocumentCategoryButton"
    );

const documentCategoryDisplay =
    document.getElementById(
        "documentCategoryDisplay"
    );

const documentCategoryEditor =
    document.getElementById(
        "documentCategoryEditor"
    );

const documentCategoryExistingSection =
    document.getElementById(
        "documentCategoryExistingSection"
    );

const documentCategorySelect =
    document.getElementById(
        "documentCategorySelect"
    );

const newDocumentCategoryButton =
    document.getElementById(
        "newDocumentCategoryButton"
    );

const documentCategoryNewSection =
    document.getElementById(
        "documentCategoryNewSection"
    );

const documentCategoryNewInput =
    document.getElementById(
        "documentCategoryNewInput"
    );

const backToExistingCategoriesButton =
    document.getElementById(
        "backToExistingCategoriesButton"
    );

const saveDocumentCategoryButton =
    document.getElementById(
        "saveDocumentCategoryButton"
    );

const cancelDocumentCategoryButton =
    document.getElementById(
        "cancelDocumentCategoryButton"
    );

const documentCategoryMessage =
    document.getElementById(
        "documentCategoryMessage"
    );


/* ========================================
   CATEGORY STATE
   ======================================== */

let documentCategories =
    [];

let documentCategoryMode =
    "existing";

let documentCategoryLoading =
    false;

let documentCategorySaving =
    false;

let documentCategoryLoadVersion =
    0;


/* ========================================
   DOCUMENT ID
   ======================================== */

function getDocumentDetailsId() {

    if (
        window.currentDocumentId &&
        /^\d+$/.test(
            window.currentDocumentId
        )
    ) {

        return window.currentDocumentId;
    }

    const pathParts =
        window.location.pathname
            .split("/")
            .filter(Boolean);

    const documentsIndex =
        pathParts.indexOf(
            "documents"
        );

    if (
        documentsIndex ===
        -1
    ) {

        return null;
    }

    const documentId =
        pathParts[
            documentsIndex + 1
        ];

    if (
        !documentId ||
        !/^\d+$/.test(
            documentId
        )
    ) {

        return null;
    }

    return documentId;
}


const documentDetailsId =
    getDocumentDetailsId();


/* ========================================
   STORE CURRENT DOCUMENT ID
   ======================================== */

if (documentDetailsId) {

    window.currentDocumentId =
        documentDetailsId;
}


/* ========================================
   LOAD DOCUMENT
   ======================================== */

async function loadDocumentDetails() {

    if (!documentDetailsId) {

        showDocumentLoadError(
            "Unable to identify the requested document."
        );

        return;
    }


    /* ========================================
       INITIAL STATE
       ======================================== */

    showDocumentLoading();

    clearDocumentDetailsMessage();

    updateDocumentCategoryAction(
        false
    );

    updateFormatAction(
        false
    );

    updateFullscreenAction(
        false
    );


    try {

        const [
            documentResponse,
            formatsResponse
        ] = await Promise.all([
            fetch(
                `/cf_automation/documents/${documentDetailsId}`,
                {
                    method:
                        "GET",

                    credentials:
                        "same-origin"
                }
            ),

            fetch(
                `/cf_automation/documents/${documentDetailsId}/formats`,
                {
                    method:
                        "GET",

                    credentials:
                        "same-origin"
                }
            )
        ]);


        /* ========================================
           AUTH CHECK
           ======================================== */

        if (
            documentResponse.status ===
            401 ||
            documentResponse.status ===
            403 ||
            formatsResponse.status ===
            401 ||
            formatsResponse.status ===
            403
        ) {

            window.location.href =
                "/automation/unauthorized";

            return;
        }


        /* ========================================
           DOCUMENT NOT FOUND
           ======================================== */

        if (
            documentResponse.status ===
            404
        ) {

            showDocumentLoadError(
                "The requested document was not found."
            );

            return;
        }


        /* ========================================
           DOCUMENT RESPONSE CHECK
           ======================================== */

        if (
            !documentResponse.ok
        ) {

            showDocumentLoadError(
                "The document could not be loaded."
            );

            return;
        }


        /* ========================================
           FORMAT RESPONSE CHECK
           ======================================== */

        if (
            !formatsResponse.ok
        ) {

            showDocumentLoadError(
                "The document formatting could not be loaded."
            );

            return;
        }


        /* ========================================
           READ DATA
           ======================================== */

        const documentData =
            await documentResponse.json();

        const formatsData =
            await formatsResponse.json();


        /* ========================================
           STORE CURRENT DATA
           ======================================== */

        window.currentDocumentStatus =
            documentData.status ||
            null;

        window.currentDocumentData =
            documentData;

        window.currentDocumentFormats =
            Array.isArray(
                formatsData
            )
                ? formatsData
                : [];


        /* ========================================
           DISPLAY INFORMATION
           ======================================== */

        displayDocumentInformation(
            documentData
        );


        /* ========================================
           DISPLAY CONTENT
           ======================================== */

        displayDocumentContent(
            documentData,
            window.currentDocumentFormats
        );


        /* ========================================
           UPDATE ACTIONS
           ======================================== */

        updateDocumentActions(
            documentData
        );

        updateDocumentCategoryAction(
            true
        );

    } catch (error) {

        console.error(
            "Unable to load document.",
            error
        );

        showDocumentLoadError(
            "Unable to connect to the server."
        );
    }
}


/* ========================================
   DISPLAY DOCUMENT INFORMATION
   ======================================== */

function displayDocumentInformation(
    documentData
) {

    const title =
        documentData.title ||
        "Untitled Document";

    const category =
        documentData.categoryName ||
        "No Category";

    const status =
        documentData.status ||
        "UNKNOWN";


    /* ========================================
       PAGE TITLE
       ======================================== */

    if (documentPageTitle) {

        documentPageTitle.textContent =
            title;
    }

    document.title =
        `${title} | Document`;


    /* ========================================
       DOCUMENT TITLE
       ======================================== */

    if (documentInfoTitle) {

        documentInfoTitle.textContent =
            title;

        documentInfoTitle.title =
            title;
    }


    /* ========================================
       CATEGORY
       ======================================== */

    if (documentInfoCategory) {

        documentInfoCategory.textContent =
            category;

        documentInfoCategory.title =
            category;
    }


    /* ========================================
       DOCUMENT ID
       ======================================== */

    if (documentInfoId) {

        documentInfoId.textContent =
            documentData.id ??
            "-";
    }


    /* ========================================
       STATUS
       ======================================== */

    displayDocumentStatus(
        status
    );
}


/* ========================================
   DISPLAY STATUS
   ======================================== */

function displayDocumentStatus(
    status
) {

    if (!documentInfoStatus) {

        return;
    }

    documentInfoStatus.textContent =
        status;

    documentInfoStatus.classList.remove(
        "ready",
        "pending",
        "awaiting-file",
        "failed"
    );

    switch (status) {

        case "READY":

            documentInfoStatus.classList.add(
                "ready"
            );

            break;


        case "PENDING":

            documentInfoStatus.classList.add(
                "pending"
            );

            break;


        case "AWAITING_FILE":

            documentInfoStatus.classList.add(
                "awaiting-file"
            );

            break;


        case "FAILED":

            documentInfoStatus.classList.add(
                "failed"
            );

            break;
    }
}


/* ========================================
   UPDATE CATEGORY ACTION
   ======================================== */

function updateDocumentCategoryAction(
    isAvailable
) {

    if (
        !changeDocumentCategoryButton
    ) {

        return;
    }

    const editorOpen =
        documentCategoryEditor &&
        !documentCategoryEditor.hidden;

    changeDocumentCategoryButton.disabled =
        !isAvailable ||
        editorOpen ||
        documentCategoryLoading ||
        documentCategorySaving;
}


/* ========================================
   CHECK DEFAULT CATEGORY
   ======================================== */

function isDefaultDocumentCategory(
    categoryName
) {

    if (!categoryName) {

        return false;
    }

    return categoryName
        .trim()
        .localeCompare(
            "Διάφορα",
            "el",
            {
                sensitivity:
                    "base"
            }
        ) === 0;
}


/* ========================================
   CHECK GREEK CATEGORY
   ======================================== */

function isGreekDocumentCategory(
    categoryName
) {

    if (!categoryName) {

        return false;
    }

    return /[\u0370-\u03FF\u1F00-\u1FFF]/.test(
        categoryName
    );
}


/* ========================================
   SORT CATEGORIES
   ======================================== */

function sortDocumentCategories(
    categories
) {

    return [
        ...categories
    ].sort(
        (
            firstCategory,
            secondCategory
        ) => {

            const firstName =
                (
                    firstCategory.name ||
                    ""
                ).trim();

            const secondName =
                (
                    secondCategory.name ||
                    ""
                ).trim();


            /* ========================================
               DEFAULT CATEGORY FIRST
               ======================================== */

            const firstIsDefault =
                isDefaultDocumentCategory(
                    firstName
                );

            const secondIsDefault =
                isDefaultDocumentCategory(
                    secondName
                );

            if (
                firstIsDefault &&
                !secondIsDefault
            ) {

                return -1;
            }

            if (
                !firstIsDefault &&
                secondIsDefault
            ) {

                return 1;
            }


            /* ========================================
               GREEK BEFORE ENGLISH
               ======================================== */

            const firstIsGreek =
                isGreekDocumentCategory(
                    firstName
                );

            const secondIsGreek =
                isGreekDocumentCategory(
                    secondName
                );

            if (
                firstIsGreek &&
                !secondIsGreek
            ) {

                return -1;
            }

            if (
                !firstIsGreek &&
                secondIsGreek
            ) {

                return 1;
            }


            /* ========================================
               GREEK ALPHABETICAL
               ======================================== */

            if (
                firstIsGreek &&
                secondIsGreek
            ) {

                return firstName.localeCompare(
                    secondName,
                    "el",
                    {
                        sensitivity:
                            "base"
                    }
                );
            }


            /* ========================================
               ENGLISH ALPHABETICAL
               ======================================== */

            return firstName.localeCompare(
                secondName,
                "en",
                {
                    sensitivity:
                        "base"
                }
            );
        }
    );
}


/* ========================================
   CATEGORY MESSAGE
   ======================================== */

function showDocumentCategoryMessage(
    message,
    type
) {

    if (
        !documentCategoryMessage
    ) {

        return;
    }

    documentCategoryMessage.textContent =
        message;

    documentCategoryMessage.classList.remove(
        "success",
        "error"
    );

    if (type) {

        documentCategoryMessage.classList.add(
            type
        );
    }
}


/* ========================================
   CLEAR CATEGORY MESSAGE
   ======================================== */

function clearDocumentCategoryMessage() {

    if (
        !documentCategoryMessage
    ) {

        return;
    }

    documentCategoryMessage.textContent =
        "";

    documentCategoryMessage.classList.remove(
        "success",
        "error"
    );
}


/* ========================================
   UPDATE CATEGORY EDITOR CONTROLS
   ======================================== */

function updateDocumentCategoryEditorControls() {

    const busy =
        documentCategoryLoading ||
        documentCategorySaving;


    /* ========================================
       SELECT
       ======================================== */

    if (
        documentCategorySelect
    ) {

        documentCategorySelect.disabled =
            busy ||
            documentCategoryMode !==
            "existing";
    }


    /* ========================================
       NEW BUTTON
       ======================================== */

    if (
        newDocumentCategoryButton
    ) {

        newDocumentCategoryButton.disabled =
            busy;
    }


    /* ========================================
       NEW INPUT
       ======================================== */

    if (
        documentCategoryNewInput
    ) {

        documentCategoryNewInput.disabled =
            busy ||
            documentCategoryMode !==
            "new";
    }


    /* ========================================
       BACK BUTTON
       ======================================== */

    if (
        backToExistingCategoriesButton
    ) {

        backToExistingCategoriesButton.disabled =
            busy;
    }


    /* ========================================
       CANCEL BUTTON
       ======================================== */

    if (
        cancelDocumentCategoryButton
    ) {

        cancelDocumentCategoryButton.disabled =
            documentCategorySaving;
    }


    /* ========================================
       SAVE BUTTON
       ======================================== */

    if (
        saveDocumentCategoryButton
    ) {

        let hasValue =
            false;

        if (
            documentCategoryMode ===
            "existing"
        ) {

            hasValue =
                Boolean(
                    documentCategorySelect &&
                    documentCategorySelect.value
                );

        } else {

            hasValue =
                Boolean(
                    documentCategoryNewInput &&
                    documentCategoryNewInput
                        .value
                        .trim()
                );
        }

        saveDocumentCategoryButton.disabled =
            busy ||
            !hasValue;
    }


    /* ========================================
       CHANGE BUTTON
       ======================================== */

    updateDocumentCategoryAction(
        Boolean(
            window.currentDocumentData
        )
    );
}


/* ========================================
   POPULATE CATEGORY SELECT
   ======================================== */

function populateDocumentCategorySelect(
    categories
) {

    if (
        !documentCategorySelect
    ) {

        return;
    }

    documentCategorySelect.innerHTML =
        "";

    const currentDocument =
        window.currentDocumentData ||
        null;


    /* ========================================
       NO CATEGORIES
       ======================================== */

    if (
        !Array.isArray(
            categories
        ) ||
        categories.length ===
        0
    ) {

        const emptyOption =
            document.createElement(
                "option"
            );

        emptyOption.value =
            "";

        emptyOption.textContent =
            "No categories available";

        documentCategorySelect.appendChild(
            emptyOption
        );

        documentCategorySelect.value =
            "";

        updateDocumentCategoryEditorControls();

        return;
    }


    /* ========================================
       OPTIONS
       ======================================== */

    categories.forEach(
        (category) => {

            const option =
                document.createElement(
                    "option"
                );

            option.value =
                String(
                    category.id
                );

            option.textContent =
                category.name;

            option.dataset.categoryName =
                category.name;

            documentCategorySelect.appendChild(
                option
            );
        }
    );


    /* ========================================
       SELECT CURRENT CATEGORY BY ID
       ======================================== */

    let currentOption =
        null;

    if (
        currentDocument &&
        currentDocument.categoryId !=
        null
    ) {

        currentOption =
            Array.from(
                documentCategorySelect.options
            ).find(
                (option) =>
                    option.value ===
                    String(
                        currentDocument.categoryId
                    )
            );
    }


    /* ========================================
       FALLBACK BY NAME
       ======================================== */

    if (
        !currentOption &&
        currentDocument &&
        currentDocument.categoryName
    ) {

        currentOption =
            Array.from(
                documentCategorySelect.options
            ).find(
                (option) => {

                    const optionName =
                        option.dataset
                            .categoryName ||
                        option.textContent ||
                        "";

                    return optionName
                        .trim()
                        .localeCompare(
                            currentDocument.categoryName
                                .trim(),
                            "el",
                            {
                                sensitivity:
                                    "base"
                            }
                        ) ===
                        0;
                }
            );
    }


    /* ========================================
       APPLY SELECTION
       ======================================== */

    if (currentOption) {

        documentCategorySelect.value =
            currentOption.value;

    } else {

        documentCategorySelect.selectedIndex =
            0;
    }

    updateDocumentCategoryEditorControls();
}


/* ========================================
   LOAD CATEGORIES
   ======================================== */

async function loadDocumentCategories() {

    if (
        !documentCategorySelect
    ) {

        return;
    }

    const currentLoadVersion =
        ++documentCategoryLoadVersion;

    documentCategoryLoading =
        true;

    clearDocumentCategoryMessage();

    documentCategorySelect.innerHTML =
        "";

    const loadingOption =
        document.createElement(
            "option"
        );

    loadingOption.value =
        "";

    loadingOption.textContent =
        "Loading categories...";

    documentCategorySelect.appendChild(
        loadingOption
    );

    updateDocumentCategoryEditorControls();

    try {

        const response =
            await fetch(
                "/cf_automation/categories",
                {
                    method:
                        "GET",

                    credentials:
                        "same-origin"
                }
            );


        /* ========================================
           LOAD CANCELLED
           ======================================== */

        if (
            currentLoadVersion !==
            documentCategoryLoadVersion
        ) {

            return;
        }


        /* ========================================
           AUTH CHECK
           ======================================== */

        if (
            response.status ===
            401 ||
            response.status ===
            403
        ) {

            window.location.href =
                "/automation/unauthorized";

            return;
        }


        /* ========================================
           RESPONSE CHECK
           ======================================== */

        if (
            !response.ok
        ) {

            throw new Error(
                "Unable to load categories."
            );
        }


        /* ========================================
           READ CATEGORIES
           ======================================== */

        const categories =
            await response.json();

        if (
            currentLoadVersion !==
            documentCategoryLoadVersion
        ) {

            return;
        }

        documentCategories =
            sortDocumentCategories(
                Array.isArray(
                    categories
                )
                    ? categories
                    : []
            );

        populateDocumentCategorySelect(
            documentCategories
        );

    } catch (error) {

        if (
            currentLoadVersion !==
            documentCategoryLoadVersion
        ) {

            return;
        }

        console.error(
            "Unable to load categories.",
            error
        );

        documentCategorySelect.innerHTML =
            "";

        const errorOption =
            document.createElement(
                "option"
            );

        errorOption.value =
            "";

        errorOption.textContent =
            "Unable to load categories";

        documentCategorySelect.appendChild(
            errorOption
        );

        showDocumentCategoryMessage(
            error.message ||
            "Unable to load categories.",
            "error"
        );

    } finally {

        if (
            currentLoadVersion ===
            documentCategoryLoadVersion
        ) {

            documentCategoryLoading =
                false;

            updateDocumentCategoryEditorControls();
        }
    }
}


/* ========================================
   SHOW EXISTING CATEGORY MODE
   ======================================== */

function showExistingDocumentCategoryMode() {

    documentCategoryMode =
        "existing";

    if (
        documentCategoryExistingSection
    ) {

        documentCategoryExistingSection.hidden =
            false;
    }

    if (
        documentCategoryNewSection
    ) {

        documentCategoryNewSection.hidden =
            true;
    }

    clearDocumentCategoryMessage();

    updateDocumentCategoryEditorControls();
}


/* ========================================
   SHOW NEW CATEGORY MODE
   ======================================== */

function showNewDocumentCategoryMode() {

    documentCategoryMode =
        "new";

    if (
        documentCategoryExistingSection
    ) {

        documentCategoryExistingSection.hidden =
            true;
    }

    if (
        documentCategoryNewSection
    ) {

        documentCategoryNewSection.hidden =
            false;
    }

    if (
        documentCategoryNewInput
    ) {

        documentCategoryNewInput.value =
            "";

        setTimeout(
            () => {

                documentCategoryNewInput.focus();
            },
            0
        );
    }

    clearDocumentCategoryMessage();

    updateDocumentCategoryEditorControls();
}


/* ========================================
   OPEN CATEGORY EDITOR
   ======================================== */

async function openDocumentCategoryEditor() {

    if (
        !documentCategoryEditor ||
        !documentCategoryDisplay
    ) {

        return;
    }

    if (
        !window.currentDocumentData
    ) {

        return;
    }


    /* ========================================
       EDIT MODE
       ======================================== */

    if (
        documentCategoryInfoItem
    ) {

        documentCategoryInfoItem.classList.add(
            "editing"
        );
    }

    documentCategoryDisplay.hidden =
        true;

    documentCategoryEditor.hidden =
        false;

    if (
        changeDocumentCategoryButton
    ) {

        changeDocumentCategoryButton.disabled =
            true;
    }

    if (
        documentCategoryNewInput
    ) {

        documentCategoryNewInput.value =
            "";
    }

    clearDocumentCategoryMessage();

    showExistingDocumentCategoryMode();

    await loadDocumentCategories();
}


/* ========================================
   CLOSE CATEGORY EDITOR
   ======================================== */

function closeDocumentCategoryEditor() {

    documentCategoryLoadVersion++;

    documentCategoryLoading =
        false;

    if (
        documentCategoryInfoItem
    ) {

        documentCategoryInfoItem.classList.remove(
            "editing"
        );
    }

    if (
        documentCategoryDisplay
    ) {

        documentCategoryDisplay.hidden =
            false;
    }

    if (
        documentCategoryEditor
    ) {

        documentCategoryEditor.hidden =
            true;
    }

    if (
        documentCategoryExistingSection
    ) {

        documentCategoryExistingSection.hidden =
            false;
    }

    if (
        documentCategoryNewSection
    ) {

        documentCategoryNewSection.hidden =
            true;
    }

    if (
        documentCategoryNewInput
    ) {

        documentCategoryNewInput.value =
            "";
    }

    documentCategoryMode =
        "existing";

    clearDocumentCategoryMessage();

    updateDocumentCategoryAction(
        Boolean(
            window.currentDocumentData
        )
    );
}


/* ========================================
   GET SELECTED CATEGORY NAME
   ======================================== */

function getSelectedDocumentCategoryName() {

    if (
        documentCategoryMode ===
        "new"
    ) {

        return documentCategoryNewInput
            ? documentCategoryNewInput
                .value
                .trim()
            : "";
    }

    if (
        !documentCategorySelect ||
        !documentCategorySelect.value
    ) {

        return "";
    }

    const selectedOption =
        documentCategorySelect.options[
            documentCategorySelect.selectedIndex
        ];

    if (!selectedOption) {

        return "";
    }

    return (
        selectedOption.dataset.categoryName ||
        selectedOption.textContent ||
        ""
    ).trim();
}


/* ========================================
   GET SELECTED CATEGORY ID
   ======================================== */

function getSelectedDocumentCategoryId() {

    if (
        documentCategoryMode !==
        "existing" ||
        !documentCategorySelect ||
        !documentCategorySelect.value
    ) {

        return null;
    }

    const categoryId =
        Number(
            documentCategorySelect.value
        );

    return Number.isInteger(
        categoryId
    )
        ? categoryId
        : null;
}


/* ========================================
   SAVE DOCUMENT CATEGORY
   ======================================== */

async function saveDocumentCategory() {

    if (
        !documentDetailsId ||
        documentCategorySaving
    ) {

        return;
    }

    const categoryName =
        getSelectedDocumentCategoryName();

    if (!categoryName) {

        showDocumentCategoryMessage(
            "Category name is required.",
            "error"
        );

        return;
    }

    documentCategorySaving =
        true;

    clearDocumentCategoryMessage();

    if (
        saveDocumentCategoryButton
    ) {

        saveDocumentCategoryButton.textContent =
            "Saving...";
    }

    updateDocumentCategoryEditorControls();

    try {

        const response =
            await fetch(
                `/cf_automation/documents/${documentDetailsId}/category`,
                {
                    method:
                        "PATCH",

                    credentials:
                        "same-origin",

                    headers: {
                        "Content-Type":
                            "application/json"
                    },

                    body:
                        JSON.stringify(
                            {
                                categoryName:
                                    categoryName
                            }
                        )
                }
            );


        /* ========================================
           AUTH CHECK
           ======================================== */

        if (
            response.status ===
            401 ||
            response.status ===
            403
        ) {

            window.location.href =
                "/automation/unauthorized";

            return;
        }


        /* ========================================
           RESPONSE CHECK
           ======================================== */

        if (
            !response.ok
        ) {

            let message =
                "Unable to change document category.";

            try {

                const errorData =
                    await response.json();

                message =
                    errorData.message ||
                    message;

            } catch {
                // Keep default message.
            }

            throw new Error(
                message
            );
        }


        /* ========================================
           RESPONSE DATA
           ======================================== */

        let assignmentData =
            null;

        try {

            assignmentData =
                await response.json();

        } catch {

            assignmentData =
                null;
        }


        /* ========================================
           RESOLVE CATEGORY DATA
           ======================================== */

        const selectedCategoryId =
            getSelectedDocumentCategoryId();

        const resolvedCategoryId =
            assignmentData &&
            assignmentData.categoryId !=
            null
                ? assignmentData.categoryId
                : selectedCategoryId;

        const resolvedCategoryName =
            assignmentData &&
            assignmentData.categoryName
                ? assignmentData.categoryName
                : categoryName;


        /* ========================================
           UPDATE CURRENT DOCUMENT CACHE
           ======================================== */

        if (
            window.currentDocumentData
        ) {

            window.currentDocumentData.categoryId =
                resolvedCategoryId;

            window.currentDocumentData.categoryName =
                resolvedCategoryName;
        }


        /* ========================================
           UPDATE CATEGORY DISPLAY
           ======================================== */

        if (
            documentInfoCategory
        ) {

            documentInfoCategory.textContent =
                resolvedCategoryName;

            documentInfoCategory.title =
                resolvedCategoryName;
        }


        /* ========================================
           CLOSE EDITOR
           ======================================== */

        closeDocumentCategoryEditor();


        /* ========================================
           PAGE SUCCESS MESSAGE
           ======================================== */

        showDocumentDetailsMessage(
            `Category changed to "${resolvedCategoryName}".`,
            "success"
        );


        /* ========================================
           REFRESH SIDEBAR CATEGORIES
           ======================================== */

        if (
            typeof window.loadCategories ===
            "function"
        ) {

            try {

                await window.loadCategories();

            } catch (error) {

                console.warn(
                    "Unable to refresh sidebar categories.",
                    error
                );
            }
        }

    } catch (error) {

        console.error(
            "Unable to change document category.",
            error
        );

        showDocumentCategoryMessage(
            error.message ||
            "Unable to change document category.",
            "error"
        );

    } finally {

        documentCategorySaving =
            false;

        if (
            saveDocumentCategoryButton
        ) {

            saveDocumentCategoryButton.textContent =
                "Save";
        }

        updateDocumentCategoryEditorControls();
    }
}



/* ========================================
   DISPLAY DOCUMENT CONTENT
   ======================================== */

function displayDocumentContent(
    documentData,
    formatsData
) {

    hideAllDocumentStates();

    /* ========================================
       AWAITING FILE
       ======================================== */

    if (documentData.status === "AWAITING_FILE") {

        clearDocumentFrame();
        showDocumentEmptyState();

        return;
    }

    /* ========================================
       PENDING
       ======================================== */

    if (documentData.status === "PENDING") {

        clearDocumentFrame();

        if (documentPendingState) {
            documentPendingState.hidden = false;
        }

        return;
    }

    /* ========================================
       FAILED
       ======================================== */

    if (documentData.status === "FAILED") {

        clearDocumentFrame();

        if (documentFailedMessage) {
            documentFailedMessage.textContent =
                documentData.errorMessage ||
                "The document could not be processed.";
        }

        if (documentFailedState) {
            documentFailedState.hidden = false;
        }

        return;
    }

    /* ========================================
       READY DOCUMENT
       ======================================== */

    const htmlContent =
        documentData.htmlContent;

    if (
        documentData.status === "READY" &&
        typeof htmlContent === "string" &&
        htmlContent.trim().length > 0
    ) {

        /* ========================================
           QUIZ DOCUMENT
           ======================================== */

        if (documentData.documentType === "QUIZ") {

            clearDocumentFrame();

            if (
                typeof window.renderDocumentQuiz ===
                "function"
            ) {

                window.renderDocumentQuiz(
                    documentData,
                    formatsData || []
                );

            } else {

                console.error(
                    "Document quiz renderer is not available."
                );

                showDocumentLoadError(
                    "Unable to load quiz renderer."
                );
            }

            return;
        }

        /* ========================================
           DOCUMENT / EXECUTABLE
           ======================================== */

        if (
            typeof window.closeDocumentQuiz ===
            "function"
        ) {
            window.closeDocumentQuiz();
        }

        showDocumentHtmlContent(
            htmlContent,
            formatsData || []
        );

        return;
    }

    /* ========================================
       UNKNOWN / EMPTY
       ======================================== */

    clearDocumentFrame();
    showDocumentEmptyState();
}



/* ========================================
   SHOW HTML CONTENT
   ======================================== */

function showDocumentHtmlContent(
    htmlContent,
    formatsData
) {

    if (
        !documentContentContainer ||
        !documentContentFrame
    ) {

        return;
    }


    /* ========================================
       FORMAT DOCUMENT
       ======================================== */

    let renderedContent =
        htmlContent;

    if (
        typeof window.renderFormattedDocument ===
        "function"
    ) {

        renderedContent =
            window.renderFormattedDocument(
                htmlContent,
                formatsData ||
                []
            );

    } else {

        console.warn(
            "Document format renderer is not available. Raw HTML will be displayed."
        );
    }


    /* ========================================
       STORE RENDERED DOCUMENT
       ======================================== */

    window.currentDocumentRenderedContent =
        renderedContent;


    /* ========================================
       DISPLAY IN IFRAME
       ======================================== */

    documentContentFrame.srcdoc =
        renderedContent;

    documentContentContainer.hidden =
        false;
}


/* ========================================
   CLEAR DOCUMENT FRAME
   ======================================== */

function clearDocumentFrame() {

    if (
        !documentContentFrame
    ) {

        return;
    }

    documentContentFrame.srcdoc =
        "";

    window.currentDocumentRenderedContent =
        "";

        if (typeof window.closeDocumentQuiz === "function") {

            window.closeDocumentQuiz();
        }
}


/* ========================================
   UPDATE DOCUMENT ACTIONS
   ======================================== */

function updateDocumentActions(
    documentData
) {

    const status =
        documentData.status;

    const hasContent =
        typeof documentData.htmlContent ===
        "string" &&
        documentData.htmlContent
            .trim()
            .length >
        0;


    /* ========================================
       REUPLOAD
       ======================================== */

    if (
        documentReuploadButton
    ) {

        if (
            status ===
            "PENDING"
        ) {

            documentReuploadButton.disabled =
                true;

            documentReuploadButton.title =
                "This document is currently being processed.";

        } else {

            documentReuploadButton.disabled =
                false;

            documentReuploadButton.removeAttribute(
                "title"
            );
        }
    }


    /* ========================================
       FORMAT DOCUMENT
       ======================================== */

    updateFormatAction(
        status ===
        "READY" &&
        hasContent
    );


    /* ========================================
       FULLSCREEN
       ======================================== */

    updateFullscreenAction(
        status ===
        "READY" &&
        hasContent
    );
}


/* ========================================
   UPDATE FORMAT ACTION
   ======================================== */

function updateFormatAction(
    isAvailable
) {

    if (
        !documentFormatButton
    ) {

        return;
    }

    documentFormatButton.disabled =
        !isAvailable;

    if (
        isAvailable
    ) {

        documentFormatButton.removeAttribute(
            "title"
        );

        return;
    }

    documentFormatButton.title =
        "Formatting is available when the document content is ready.";
}


/* ========================================
   UPDATE FULLSCREEN ACTION
   ======================================== */

function updateFullscreenAction(
    isAvailable
) {

    if (
        !documentFullscreenActionButton
    ) {

        return;
    }

    documentFullscreenActionButton.disabled =
        !isAvailable;

    if (
        isAvailable
    ) {

        documentFullscreenActionButton.removeAttribute(
            "title"
        );

        return;
    }

    documentFullscreenActionButton.title =
        "Fullscreen is available when the document content is ready.";
}


/* ========================================
   SHOW LOADING
   ======================================== */

function showDocumentLoading() {

    hideAllDocumentStates();

    updateDocumentCategoryAction(
        false
    );

    updateFormatAction(
        false
    );

    updateFullscreenAction(
        false
    );

    if (
        documentLoading
    ) {

        documentLoading.hidden =
            false;
    }
}


/* ========================================
   SHOW LOAD ERROR
   ======================================== */

function showDocumentLoadError(
    message
) {

    hideAllDocumentStates();

    clearDocumentFrame();

    updateDocumentCategoryAction(
        false
    );

    updateFormatAction(
        false
    );

    updateFullscreenAction(
        false
    );

    if (
        documentLoadErrorMessage
    ) {

        documentLoadErrorMessage.textContent =
            message;
    }

    if (
        documentLoadError
    ) {

        documentLoadError.hidden =
            false;
    }


    /* ========================================
       HEADER FALLBACK
       ======================================== */

    if (
        documentPageTitle
    ) {

        documentPageTitle.textContent =
            "Document unavailable";
    }


    /* ========================================
       DISABLE REUPLOAD
       ======================================== */

    if (
        documentReuploadButton
    ) {

        documentReuploadButton.disabled =
            true;
    }
}


/* ========================================
   SHOW EMPTY STATE
   ======================================== */

function showDocumentEmptyState() {

    hideAllDocumentStates();

    updateFormatAction(
        false
    );

    updateFullscreenAction(
        false
    );

    if (
        documentEmptyState
    ) {

        documentEmptyState.hidden =
            false;
    }
}


/* ========================================
   HIDE ALL DOCUMENT STATES
   ======================================== */

function hideAllDocumentStates() {

    if (
        documentLoading
    ) {

        documentLoading.hidden =
            true;
    }

    if (
        documentLoadError
    ) {

        documentLoadError.hidden =
            true;
    }

    if (
        documentPendingState
    ) {

        documentPendingState.hidden =
            true;
    }

    if (
        documentFailedState
    ) {

        documentFailedState.hidden =
            true;
    }

    if (
        documentEmptyState
    ) {

        documentEmptyState.hidden =
            true;
    }

    if (
        documentContentContainer
    ) {

        documentContentContainer.hidden =
            true;
    }

    const documentQuiz =
        document.getElementById("documentQuiz");

    if (documentQuiz) {

        documentQuiz.hidden = true;
    }
}


/* ========================================
   PAGE MESSAGE
   ======================================== */

function showDocumentDetailsMessage(
    message,
    type
) {

    if (
        !documentDetailsMessage
    ) {

        return;
    }

    documentDetailsMessage.textContent =
        message;

    documentDetailsMessage.classList.remove(
        "success",
        "error"
    );

    if (
        type
    ) {

        documentDetailsMessage.classList.add(
            type
        );
    }
}


/* ========================================
   CLEAR PAGE MESSAGE
   ======================================== */

function clearDocumentDetailsMessage() {

    if (
        !documentDetailsMessage
    ) {

        return;
    }

    documentDetailsMessage.textContent =
        "";

    documentDetailsMessage.classList.remove(
        "success",
        "error"
    );
}


/* ========================================
   CHANGE CATEGORY BUTTON
   ======================================== */

if (
    changeDocumentCategoryButton
) {

    changeDocumentCategoryButton.addEventListener(
        "click",
        () => {

            openDocumentCategoryEditor();
        }
    );
}


/* ========================================
   NEW CATEGORY BUTTON
   ======================================== */

if (
    newDocumentCategoryButton
) {

    newDocumentCategoryButton.addEventListener(
        "click",
        () => {

            showNewDocumentCategoryMode();
        }
    );
}


/* ========================================
   BACK TO EXISTING CATEGORIES
   ======================================== */

if (
    backToExistingCategoriesButton
) {

    backToExistingCategoriesButton.addEventListener(
        "click",
        () => {

            showExistingDocumentCategoryMode();
        }
    );
}


/* ========================================
   CANCEL CATEGORY CHANGE
   ======================================== */

if (
    cancelDocumentCategoryButton
) {

    cancelDocumentCategoryButton.addEventListener(
        "click",
        () => {

            closeDocumentCategoryEditor();
        }
    );
}


/* ========================================
   SAVE CATEGORY CHANGE
   ======================================== */

if (
    saveDocumentCategoryButton
) {

    saveDocumentCategoryButton.addEventListener(
        "click",
        () => {

            saveDocumentCategory();
        }
    );
}


/* ========================================
   CATEGORY SELECT CHANGE
   ======================================== */

if (
    documentCategorySelect
) {

    documentCategorySelect.addEventListener(
        "change",
        () => {

            clearDocumentCategoryMessage();

            updateDocumentCategoryEditorControls();
        }
    );
}


/* ========================================
   NEW CATEGORY INPUT
   ======================================== */

if (
    documentCategoryNewInput
) {

    documentCategoryNewInput.addEventListener(
        "input",
        () => {

            clearDocumentCategoryMessage();

            updateDocumentCategoryEditorControls();
        }
    );


    /* ========================================
       SAVE WITH ENTER
       ======================================== */

    documentCategoryNewInput.addEventListener(
        "keydown",
        (event) => {

            if (
                event.key !==
                "Enter"
            ) {

                return;
            }

            event.preventDefault();

            if (
                saveDocumentCategoryButton &&
                !saveDocumentCategoryButton.disabled
            ) {

                saveDocumentCategory();
            }
        }
    );
}


/* ========================================
   MAKE AVAILABLE TO OTHER COMPONENTS
   ======================================== */

window.loadDocumentDetails =
    loadDocumentDetails;


/* ========================================
   INITIAL CATEGORY STATE
   ======================================== */

updateDocumentCategoryAction(
    false
);

updateDocumentCategoryEditorControls();


/* ========================================
   INITIAL LOAD
   ======================================== */

loadDocumentDetails();