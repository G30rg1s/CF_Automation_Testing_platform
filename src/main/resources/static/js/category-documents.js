document.addEventListener("DOMContentLoaded", () => {

    /* ========================================
       ELEMENTS
       ======================================== */

    const selectedCategoryName = document.getElementById("selectedCategoryName");
    const categorySummaryName = document.getElementById("categorySummaryName");
    const totalDocumentsCount = document.getElementById("totalDocumentsCount");

    const documentsLoading = document.getElementById("documentsLoading");
    const documentsError = document.getElementById("documentsError");
    const documentsEmpty = document.getElementById("documentsEmpty");
    const documentsContainer = document.getElementById("documentsContainer");

    const documentsPagination = document.getElementById("documentsPagination");
    const previousPageButton = document.getElementById("previousPageButton");
    const nextPageButton = document.getElementById("nextPageButton");
    const pageIndicator = document.getElementById("pageIndicator");


    /* ========================================
       PAGINATION
       ======================================== */

    const pageSize = 6;

    let currentPage = 0;
    let totalPages = 0;


    /* ========================================
       CATEGORY ID FROM URL
       ======================================== */

    function getCategoryIdFromUrl() {
        const pathParts = window.location.pathname.split("/").filter(Boolean);

        const categoriesIndex = pathParts.indexOf("categories");

        if (categoriesIndex === -1) {
            return null;
        }

        const categoryId = pathParts[categoriesIndex + 1];

        if (!categoryId || !/^\d+$/.test(categoryId)) {
            return null;
        }

        return categoryId;
    }


    const categoryId = getCategoryIdFromUrl();


    /* ========================================
       UNAUTHORIZED
       ======================================== */

    function handleUnauthorized(response) {
        if (response.status === 401 || response.status === 403) {
            window.location.href = "/automation/unauthorized";
            return true;
        }

        return false;
    }


    /* ========================================
       LOAD CATEGORY NAME
       ======================================== */

    async function loadCategoryName() {
        const response = await fetch(
            "/cf_automation/categories",
            {
                method: "GET",
                credentials: "same-origin"
            }
        );

        if (handleUnauthorized(response)) {
            return;
        }

        if (!response.ok) {
            throw new Error("Unable to load category.");
        }

        const categories = await response.json();

        const category = categories.find(category =>
            String(category.id) === String(categoryId)
        );

        if (!category) {
            throw new Error("Category not found.");
        }

        selectedCategoryName.textContent = category.name;
        categorySummaryName.textContent = category.name;

        document.title =
            `${category.name} - Coding Factory Automation`;
    }


    /* ========================================
       CREATE DOCUMENT BUTTON
       ======================================== */

    function createDocumentButton(documentData) {
        const button = document.createElement("button");

        button.type = "button";
        button.className = "document-title-button";

        button.innerHTML = `
            <span class="document-title-text"></span>
            <span class="document-title-arrow">→</span>
        `;

        const titleElement =
            button.querySelector(".document-title-text");

        titleElement.textContent =
            documentData.title || "Untitled Document";


        button.addEventListener("click", () => {
            window.location.href =
                `/automation/documents/${documentData.id}`;
        });


        return button;
    }


    /* ========================================
       RENDER DOCUMENTS
       ======================================== */

    function renderDocuments(documents) {
        documentsContainer.innerHTML = "";

        if (!documents || documents.length === 0) {
            documentsEmpty.hidden = false;
            return;
        }

        documentsEmpty.hidden = true;

        documents.forEach(documentData => {
            const documentButton =
                createDocumentButton(documentData);

            documentsContainer.appendChild(documentButton);
        });
    }


    /* ========================================
       UPDATE PAGINATION
       ======================================== */

    function updatePagination() {
        if (totalPages <= 1) {
            documentsPagination.hidden = true;
            return;
        }

        documentsPagination.hidden = false;

        pageIndicator.textContent =
            `Page ${currentPage + 1} of ${totalPages}`;

        previousPageButton.disabled =
            currentPage === 0;

        nextPageButton.disabled =
            currentPage >= totalPages - 1;
    }


    /* ========================================
       LOAD DOCUMENTS
       ======================================== */

    async function loadDocuments(page) {
        documentsLoading.hidden = false;
        documentsError.textContent = "";
        documentsEmpty.hidden = true;
        documentsContainer.innerHTML = "";
        documentsPagination.hidden = true;

        try {
            const response = await fetch(
                `/cf_automation/categories/${categoryId}/documents?page=${page}&size=${pageSize}`,
                {
                    method: "GET",
                    credentials: "same-origin"
                }
            );

            if (handleUnauthorized(response)) {
                return;
            }

            if (!response.ok) {
                throw new Error("Unable to load documents.");
            }

            const pageData = await response.json();

            currentPage = pageData.number ?? page;
            totalPages = pageData.totalPages ?? 0;

            totalDocumentsCount.textContent =
                pageData.totalElements ?? 0;

            renderDocuments(
                pageData.content ?? []
            );

            updatePagination();

        } catch (error) {
            documentsError.textContent =
                error.message || "Unable to load documents.";

            totalDocumentsCount.textContent = "0";

            console.error(
                "Unable to load category documents.",
                error
            );

        } finally {
            documentsLoading.hidden = true;
        }
    }


    /* ========================================
       PREVIOUS PAGE
       ======================================== */

    previousPageButton.addEventListener("click", () => {
        if (currentPage <= 0) {
            return;
        }

        loadDocuments(currentPage - 1);
    });


    /* ========================================
       NEXT PAGE
       ======================================== */

    nextPageButton.addEventListener("click", () => {
        if (currentPage >= totalPages - 1) {
            return;
        }

        loadDocuments(currentPage + 1);
    });


    /* ========================================
       INITIAL LOAD
       ======================================== */

    async function initializePage() {
        if (!categoryId) {
            documentsLoading.hidden = true;
            documentsError.textContent =
                "Invalid category.";

            return;
        }

        try {
            await loadCategoryName();
            await loadDocuments(0);

        } catch (error) {
            documentsLoading.hidden = true;

            documentsError.textContent =
                error.message || "Unable to load category.";

            console.error(
                "Unable to initialize category page.",
                error
            );
        }
    }


    initializePage();

});