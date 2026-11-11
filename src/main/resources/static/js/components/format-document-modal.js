(() => {

    /* ========================================
       ELEMENTS
       ======================================== */

    const formatModal =
        document.getElementById("formatDocumentModal");

    const formatModalOpenButton =
        document.getElementById("formatDocumentButton");

    const formatModalCloseButton =
        document.getElementById("closeFormatDocumentModalButton");

    const formatModalSelectedCount =
        document.getElementById("formatDocumentSelectedCount");

    const formatModalTypeButtons =
        document.querySelectorAll(".format-document-type-button");

    const formatModalLanguageContainer =
        document.getElementById("formatDocumentLanguageContainer");

    const formatModalCodeLanguage =
        document.getElementById("formatDocumentCodeLanguage");

    const formatModalPreviewButton =
        document.getElementById("previewDocumentFormattingButton");

    const formatModalSaveButton =
        document.getElementById("saveDocumentFormattingButton");

    const formatModalLoading =
        document.getElementById("formatDocumentLoading");

    const formatModalError =
        document.getElementById("formatDocumentError");

    const formatModalEditor =
        document.getElementById("formatDocumentEditor");

    const formatModalMessage =
        document.getElementById("formatDocumentMessage");


    /* ========================================
       STATE
       ======================================== */

    let formatDocumentData =
        null;

    let formatDocumentParagraphs =
        [];

    let persistedFormats =
        new Map();

    let pendingFormats =
        new Map();

    let selectedParagraphIndexes =
        new Set();

    let lastSelectedParagraphIndex =
        null;

    let previewMode =
        false;

    let loadingFormatting =
        false;

    let savingFormatting =
        false;

    let dragSelectionDetected =
        false;

    let previousBodyOverflow =
        "";

    let formatLoadVersion =
        0;


    /* ========================================
       DOCUMENT ID
       ======================================== */

    function getFormatDocumentId() {

        if (window.currentDocumentId) {

            return window.currentDocumentId;
        }

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

        if (
            !documentId ||
            !/^\d+$/.test(documentId)
        ) {

            return null;
        }

        return documentId;
    }


    /* ========================================
       OPEN MODAL
       ======================================== */

    async function openFormatDocumentModal() {

        if (!formatModal) {

            return;
        }

        if (loadingFormatting) {

            return;
        }

        formatModal.classList.add(
            "active"
        );

        formatModal.setAttribute(
            "aria-hidden",
            "false"
        );

        previousBodyOverflow =
            document.body.style.overflow;

        document.body.style.overflow =
            "hidden";

        await loadFormatDocument();
    }


    /* ========================================
       CLOSE MODAL
       ======================================== */

    function closeFormatDocumentModal() {

        if (!formatModal) {

            return;
        }

        if (savingFormatting) {

            return;
        }


        /* ========================================
           CANCEL CURRENT LOAD
           ======================================== */

        formatLoadVersion++;

        loadingFormatting =
            false;


        /* ========================================
           CLOSE
           ======================================== */

        formatModal.classList.remove(
            "active"
        );

        formatModal.setAttribute(
            "aria-hidden",
            "true"
        );

        document.body.style.overflow =
            previousBodyOverflow;

        resetFormatDocumentState();
    }


    /* ========================================
       RESET STATE
       ======================================== */

    function resetFormatDocumentState() {

        formatDocumentData =
            null;

        formatDocumentParagraphs =
            [];

        persistedFormats =
            new Map();

        pendingFormats =
            new Map();

        selectedParagraphIndexes =
            new Set();

        lastSelectedParagraphIndex =
            null;

        previewMode =
            false;

        dragSelectionDetected =
            false;

        if (formatModalEditor) {

            formatModalEditor.innerHTML =
                "";

            formatModalEditor.classList.remove(
                "preview-mode"
            );

            formatModalEditor.hidden =
                true;
        }

        if (formatModalPreviewButton) {

            formatModalPreviewButton.textContent =
                "Preview";
        }

        if (formatModalLoading) {

            formatModalLoading.hidden =
                true;
        }

        if (formatModalError) {

            formatModalError.hidden =
                true;
        }

        clearFormatMessage();

        updateFormatToolbar();
    }


    /* ========================================
       LOAD DOCUMENT
       ======================================== */

    async function loadFormatDocument() {

        const documentId =
            getFormatDocumentId();

        if (!documentId) {

            showFormatError(
                "Unable to determine document id."
            );

            return;
        }

        const currentLoadVersion =
            ++formatLoadVersion;

        setFormatLoading(
            true
        );

        try {

            const currentDocumentData =
                window.currentDocumentData;

            const currentFormats =
                window.currentDocumentFormats;


            /* ========================================
               USE ALREADY LOADED PAGE DATA
               ======================================== */

            if (
                currentDocumentData &&
                String(currentDocumentData.id) ===
                String(documentId) &&
                Array.isArray(currentFormats)
            ) {

                if (
                    currentDocumentData.status !==
                    "READY"
                ) {

                    throw new Error(
                        "Document formatting is available only when the document is ready."
                    );
                }

                if (
                    typeof currentDocumentData.htmlContent !==
                    "string" ||
                    !currentDocumentData.htmlContent.trim()
                ) {

                    throw new Error(
                        "The document does not contain content."
                    );
                }

                formatDocumentData =
                    currentDocumentData;

                storePersistedFormats(
                    currentFormats
                );

                parseDocumentParagraphs(
                    currentDocumentData.htmlContent
                );

                if (
                    currentLoadVersion !==
                    formatLoadVersion
                ) {

                    return;
                }

                renderFormatDocument();

                setFormatLoading(
                    false
                );

                return;
            }


            /* ========================================
               FALLBACK FETCH
               ======================================== */

            const [
                documentResponse,
                formatsResponse
            ] = await Promise.all([
                fetch(
                    `/cf_automation/documents/${documentId}`,
                    {
                        method:
                            "GET",

                        credentials:
                            "same-origin"
                    }
                ),

                fetch(
                    `/cf_automation/documents/${documentId}/formats`,
                    {
                        method:
                            "GET",

                        credentials:
                            "same-origin"
                    }
                )
            ]);

            if (
                currentLoadVersion !==
                formatLoadVersion
            ) {

                return;
            }


            /* ========================================
               AUTH CHECK
               ======================================== */

            if (
                documentResponse.status === 401 ||
                documentResponse.status === 403 ||
                formatsResponse.status === 401 ||
                formatsResponse.status === 403
            ) {

                window.location.href =
                    "/automation/unauthorized";

                return;
            }


            /* ========================================
               RESPONSE CHECK
               ======================================== */

            if (!documentResponse.ok) {

                throw new Error(
                    "Unable to load document."
                );
            }

            if (!formatsResponse.ok) {

                throw new Error(
                    "Unable to load document formats."
                );
            }


            /* ========================================
               RESPONSE DATA
               ======================================== */

            const documentData =
                await documentResponse.json();

            const formatsData =
                await formatsResponse.json();

            if (
                currentLoadVersion !==
                formatLoadVersion
            ) {

                return;
            }


            /* ========================================
               DOCUMENT STATUS
               ======================================== */

            if (
                documentData.status !==
                "READY"
            ) {

                throw new Error(
                    "Document formatting is available only when the document is ready."
                );
            }


            /* ========================================
               HTML CONTENT
               ======================================== */

            if (
                typeof documentData.htmlContent !==
                "string" ||
                !documentData.htmlContent.trim()
            ) {

                throw new Error(
                    "The document does not contain content."
                );
            }


            /* ========================================
               STORE DATA
               ======================================== */

            formatDocumentData =
                documentData;

            storePersistedFormats(
                formatsData
            );

            parseDocumentParagraphs(
                documentData.htmlContent
            );

            renderFormatDocument();

            setFormatLoading(
                false
            );

        } catch (error) {

            if (
                currentLoadVersion !==
                formatLoadVersion
            ) {

                return;
            }

            console.error(
                "Unable to load document formatting.",
                error
            );

            showFormatError(
                error.message ||
                "Unable to load document formatting."
            );
        }
    }


    /* ========================================
       STORE PERSISTED FORMATS
       ======================================== */

    function storePersistedFormats(formatsData) {

        persistedFormats =
            new Map();

        if (!Array.isArray(formatsData)) {

            return;
        }

        formatsData.forEach(
            (format) => {

                const paragraphIndex =
                    Number(
                        format.paragraphIndex
                    );

                if (
                    !Number.isInteger(paragraphIndex) ||
                    paragraphIndex < 0
                ) {

                    return;
                }

                persistedFormats.set(
                    paragraphIndex,
                    {
                        paragraphIndex:
                            paragraphIndex,

                        formatType:
                            format.formatType ||
                            "PARAGRAPH",

                        codeLanguage:
                            format.codeLanguage ||
                            null
                    }
                );
            }
        );
    }


    /* ========================================
       PARSE HTML PARAGRAPHS
       ======================================== */

    function parseDocumentParagraphs(
        htmlContent
    ) {

        const parser =
            new DOMParser();

        const parsedDocument =
            parser.parseFromString(
                htmlContent,
                "text/html"
            );

        const paragraphElements =
            parsedDocument.body.querySelectorAll(
                "p"
            );

        formatDocumentParagraphs =
            Array.from(
                paragraphElements
            ).map(
                (paragraph, index) => ({
                    paragraphIndex:
                        index,

                    text:
                        paragraph.textContent ||
                        ""
                })
            );
    }


    /* ========================================
       RENDER EDITOR
       ======================================== */

    function renderFormatDocument() {

        if (!formatModalEditor) {

            return;
        }

        formatModalEditor.innerHTML =
            "";

        selectedParagraphIndexes.clear();

        lastSelectedParagraphIndex =
            null;

        pendingFormats.clear();

        previewMode =
            false;

        formatModalEditor.classList.remove(
            "preview-mode"
        );

        if (formatModalPreviewButton) {

            formatModalPreviewButton.textContent =
                "Preview";
        }

        formatDocumentParagraphs.forEach(
            (paragraphData) => {

                const paragraph =
                    document.createElement(
                        "div"
                    );

                paragraph.className =
                    "format-document-paragraph";

                paragraph.dataset.paragraphIndex =
                    paragraphData.paragraphIndex;


                /* ========================================
                   PARAGRAPH CONTENT
                   ======================================== */

                const content =
                    document.createElement(
                        "div"
                    );

                content.className =
                    "format-document-paragraph-content";

                content.textContent =
                    paragraphData.text;

                paragraph.appendChild(
                    content
                );

                formatModalEditor.appendChild(
                    paragraph
                );

                updateParagraphVisual(
                    paragraphData.paragraphIndex
                );
            }
        );

        formatModalEditor.hidden =
            false;

        updateFormatToolbar();
    }


    /* ========================================
       GET EFFECTIVE FORMAT
       ======================================== */

    function getEffectiveFormat(
        paragraphIndex
    ) {

        if (
            pendingFormats.has(
                paragraphIndex
            )
        ) {

            return pendingFormats.get(
                paragraphIndex
            );
        }

        if (
            persistedFormats.has(
                paragraphIndex
            )
        ) {

            return persistedFormats.get(
                paragraphIndex
            );
        }

        return {
            paragraphIndex:
                paragraphIndex,

            formatType:
                "PARAGRAPH",

            codeLanguage:
                null
        };
    }


    /* ========================================
       GET PERSISTED FORMAT
       ======================================== */

    function getPersistedFormat(
        paragraphIndex
    ) {

        if (
            persistedFormats.has(
                paragraphIndex
            )
        ) {

            return persistedFormats.get(
                paragraphIndex
            );
        }

        return {
            paragraphIndex:
                paragraphIndex,

            formatType:
                "PARAGRAPH",

            codeLanguage:
                null
        };
    }


    /* ========================================
       SET PENDING FORMAT
       ======================================== */

    function setPendingFormat(
        paragraphIndex,
        formatType,
        codeLanguage
    ) {

        const persisted =
            getPersistedFormat(
                paragraphIndex
            );

        const normalizedLanguage =
            formatType === "CODE"
                ? codeLanguage
                : null;

        if (
            persisted.formatType ===
            formatType &&
            persisted.codeLanguage ===
            normalizedLanguage
        ) {

            pendingFormats.delete(
                paragraphIndex
            );

            return;
        }

        pendingFormats.set(
            paragraphIndex,
            {
                paragraphIndex:
                    paragraphIndex,

                formatType:
                    formatType,

                codeLanguage:
                    normalizedLanguage
            }
        );
    }


    /* ========================================
       UPDATE PARAGRAPH VISUAL
       ======================================== */

    function updateParagraphVisual(
        paragraphIndex
    ) {

        if (!formatModalEditor) {

            return;
        }

        const paragraph =
            formatModalEditor.querySelector(
                `[data-paragraph-index="${paragraphIndex}"]`
            );

        if (!paragraph) {

            return;
        }


        /* ========================================
           SELECTION
           ======================================== */

        paragraph.classList.toggle(
            "selected",
            selectedParagraphIndexes.has(
                paragraphIndex
            )
        );


        /* ========================================
           REMOVE OLD BADGES
           ======================================== */

        paragraph
            .querySelectorAll(
                ".format-document-format-badge, .format-document-language-badge"
            )
            .forEach(
                (badge) => {

                    badge.remove();
                }
            );


        /* ========================================
           FORMAT INFO
           ======================================== */

        const format =
            getEffectiveFormat(
                paragraphIndex
            );

        const isPending =
            pendingFormats.has(
                paragraphIndex
            );

        const shouldShowBadge =
            isPending ||
            format.formatType !==
            "PARAGRAPH";

        paragraph.classList.toggle(
            "has-pending-format",
            shouldShowBadge
        );

        if (shouldShowBadge) {

            const formatBadge =
                document.createElement(
                    "span"
                );

            formatBadge.className =
                "format-document-format-badge";

            formatBadge.textContent =
                isPending
                    ? `${format.formatType} • Pending`
                    : format.formatType;

            paragraph.appendChild(
                formatBadge
            );
        }


        /* ========================================
           CODE LANGUAGE BADGE
           ======================================== */

        if (
            format.formatType === "CODE" &&
            format.codeLanguage
        ) {

            const languageBadge =
                document.createElement(
                    "span"
                );

            languageBadge.className =
                "format-document-language-badge";

            languageBadge.textContent =
                format.codeLanguage;

            paragraph.appendChild(
                languageBadge
            );
        }


        /* ========================================
           PREVIEW FORMAT CLASS
           ======================================== */

        updateParagraphPreviewClass(
            paragraph,
            format
        );


        /* ========================================
           RENDER CONTENT
           ======================================== */

        renderParagraphContent(
            paragraphIndex
        );
    }


    /* ========================================
       PRISM LANGUAGE
       ======================================== */

    function getPrismLanguage(
        codeLanguage
    ) {

        switch (codeLanguage) {

            case "JAVA":

                return "java";


            case "JAVASCRIPT":

                return "javascript";


            case "TYPESCRIPT":

                return "typescript";


            case "HTML":

                return "markup";


            case "CSS":

                return "css";


            case "JSON":

                return "json";


            case "SQL":

                return "sql";


            case "XML":

                return "markup";


            case "PYTHON":

                return "python";


            case "BASH":

                return "bash";


            case "TEXT":
            default:

                return null;
        }
    }


    /* ========================================
       RENDER PARAGRAPH CONTENT
       ======================================== */

    function renderParagraphContent(
        paragraphIndex
    ) {

        if (!formatModalEditor) {

            return;
        }

        const paragraph =
            formatModalEditor.querySelector(
                `[data-paragraph-index="${paragraphIndex}"]`
            );

        if (!paragraph) {

            return;
        }

        const content =
            paragraph.querySelector(
                ".format-document-paragraph-content"
            );

        if (!content) {

            return;
        }

        const paragraphData =
            formatDocumentParagraphs.find(
                (item) =>
                    item.paragraphIndex ===
                    paragraphIndex
            );

        if (!paragraphData) {

            return;
        }

        const format =
            getEffectiveFormat(
                paragraphIndex
            );


        /* ========================================
           REMOVE OLD CODE LANGUAGE CLASS
           ======================================== */

        Array.from(
            paragraph.classList
        ).forEach(
            (className) => {

                if (
                    className.startsWith(
                        "code-language-"
                    )
                ) {

                    paragraph.classList.remove(
                        className
                    );
                }
            }
        );


        /* ========================================
           RESET CONTENT
           ======================================== */

        content.innerHTML =
            "";


        /* ========================================
           LINK
           ======================================== */

        if (
            previewMode &&
            format.formatType ===
            "LINK"
        ) {

            const link =
                document.createElement(
                    "a"
                );

            const linkUrl =
                normalizeLinkUrl(
                    paragraphData.text
                );

            if (!linkUrl) {

                content.textContent =
                    paragraphData.text;

                return;
            }

            link.href =
                linkUrl;

            link.textContent =
                paragraphData.text;

            link.target =
                "_blank";

            link.rel =
                "noopener noreferrer";

            link.className =
                "format-document-preview-link";

            content.appendChild(
                link
            );

            return;
        }


        /* ========================================
           CODE
           ======================================== */

        if (
            previewMode &&
            format.formatType ===
            "CODE"
        ) {

            const codeLanguage =
                format.codeLanguage ||
                "TEXT";

            paragraph.classList.add(
                `code-language-${codeLanguage.toLowerCase()}`
            );

            const pre =
                document.createElement(
                    "pre"
                );

            pre.className =
                "format-document-code-block";

            const code =
                document.createElement(
                    "code"
                );

            const prismLanguage =
                getPrismLanguage(
                    codeLanguage
                );

            if (prismLanguage) {

                code.className =
                    `language-${prismLanguage}`;
            }

            code.textContent =
                paragraphData.text;

            pre.appendChild(
                code
            );

            content.appendChild(
                pre
            );


            /* ========================================
               SYNTAX HIGHLIGHTING
               ======================================== */

            if (
                prismLanguage &&
                window.Prism &&
                typeof window.Prism.highlightElement ===
                "function"
            ) {

                window.Prism.highlightElement(
                    code
                );
            }

            return;
        }


        /* ========================================
           NORMAL TEXT
           ======================================== */

        content.textContent =
            paragraphData.text;
    }


    /* ========================================
       PREVIEW FORMAT CLASS
       ======================================== */

    function updateParagraphPreviewClass(
        paragraph,
        format
    ) {

        paragraph.classList.remove(
            "format-type-title",
            "format-type-heading",
            "format-type-paragraph",
            "format-type-code",
            "format-type-quote",
            "format-type-link",
            "format-type-array"
        );

        if (!previewMode) {

            return;
        }

        const formatClass =
            `format-type-${format.formatType.toLowerCase()}`;

        paragraph.classList.add(
            formatClass
        );
    }


    /* ========================================
       UPDATE ALL PARAGRAPHS
       ======================================== */

    function updateAllParagraphVisuals() {

        formatDocumentParagraphs.forEach(
            (paragraphData) => {

                updateParagraphVisual(
                    paragraphData.paragraphIndex
                );
            }
        );
    }


    /* ========================================
       CLEAR SELECTION
       ======================================== */

    function clearParagraphSelection() {

        selectedParagraphIndexes.clear();

        updateAllParagraphVisuals();

        updateFormatToolbar();
    }


    /* ========================================
       SELECT ONE PARAGRAPH
       ======================================== */

    function selectSingleParagraph(
        paragraphIndex
    ) {

        selectedParagraphIndexes.clear();

        selectedParagraphIndexes.add(
            paragraphIndex
        );

        lastSelectedParagraphIndex =
            paragraphIndex;

        updateAllParagraphVisuals();

        updateFormatToolbar();
    }


    /* ========================================
       TOGGLE PARAGRAPH
       ======================================== */

    function toggleParagraphSelection(
        paragraphIndex
    ) {

        if (
            selectedParagraphIndexes.has(
                paragraphIndex
            )
        ) {

            selectedParagraphIndexes.delete(
                paragraphIndex
            );

        } else {

            selectedParagraphIndexes.add(
                paragraphIndex
            );
        }

        lastSelectedParagraphIndex =
            paragraphIndex;

        updateAllParagraphVisuals();

        updateFormatToolbar();
    }


    /* ========================================
       SELECT RANGE
       ======================================== */

    function selectParagraphRange(
        startIndex,
        endIndex
    ) {

        const rangeStart =
            Math.min(
                startIndex,
                endIndex
            );

        const rangeEnd =
            Math.max(
                startIndex,
                endIndex
            );

        selectedParagraphIndexes.clear();

        for (
            let paragraphIndex =
                rangeStart;

            paragraphIndex <=
            rangeEnd;

            paragraphIndex++
        ) {

            selectedParagraphIndexes.add(
                paragraphIndex
            );
        }

        lastSelectedParagraphIndex =
            endIndex;

        updateAllParagraphVisuals();

        updateFormatToolbar();
    }


    /* ========================================
       PARAGRAPH CLICK
       ======================================== */

    function handleParagraphClick(
        event
    ) {

        if (
            previewMode ||
            dragSelectionDetected
        ) {

            dragSelectionDetected =
                false;

            return;
        }

        const paragraph =
            event.target.closest(
                ".format-document-paragraph"
            );

        if (
            !paragraph ||
            !formatModalEditor.contains(
                paragraph
            )
        ) {

            return;
        }

        const paragraphIndex =
            Number(
                paragraph.dataset.paragraphIndex
            );

        if (
            !Number.isInteger(
                paragraphIndex
            )
        ) {

            return;
        }


        /* ========================================
           SHIFT RANGE
           ======================================== */

        if (
            event.shiftKey &&
            lastSelectedParagraphIndex !==
            null
        ) {

            selectParagraphRange(
                lastSelectedParagraphIndex,
                paragraphIndex
            );

            return;
        }


        /* ========================================
           CTRL / CMD MULTI SELECT
           ======================================== */

        if (
            event.ctrlKey ||
            event.metaKey
        ) {

            toggleParagraphSelection(
                paragraphIndex
            );

            return;
        }


        /* ========================================
           SINGLE SELECT
           ======================================== */

        selectSingleParagraph(
            paragraphIndex
        );
    }


    /* ========================================
       GET PARAGRAPH INDEX FROM NODE
       ======================================== */

    function getParagraphIndexFromNode(
        node
    ) {

        if (!node) {

            return null;
        }

        const element =
            node.nodeType ===
            Node.ELEMENT_NODE
                ? node
                : node.parentElement;

        if (!element) {

            return null;
        }

        const paragraph =
            element.closest(
                ".format-document-paragraph"
            );

        if (
            !paragraph ||
            !formatModalEditor.contains(
                paragraph
            )
        ) {

            return null;
        }

        const paragraphIndex =
            Number(
                paragraph.dataset.paragraphIndex
            );

        return Number.isInteger(
            paragraphIndex
        )
            ? paragraphIndex
            : null;
    }


    /* ========================================
       MOUSE TEXT SELECTION
       ======================================== */

    function handleMouseTextSelection() {

        if (previewMode) {

            return;
        }

        const selection =
            window.getSelection();

        if (
            !selection ||
            selection.isCollapsed ||
            selection.rangeCount === 0
        ) {

            return;
        }

        const startIndex =
            getParagraphIndexFromNode(
                selection.anchorNode
            );

        const endIndex =
            getParagraphIndexFromNode(
                selection.focusNode
            );

        if (
            startIndex === null ||
            endIndex === null
        ) {

            return;
        }

        dragSelectionDetected =
            true;

        selectParagraphRange(
            startIndex,
            endIndex
        );

        selection.removeAllRanges();
    }


    /* ========================================
       APPLY FORMAT TO SELECTION
       ======================================== */

    function applyFormatToSelection(
        formatType
    ) {

        if (
            previewMode ||
            selectedParagraphIndexes.size ===
            0
        ) {

            return;
        }

        let codeLanguage =
            null;

        if (
            formatType ===
            "CODE"
        ) {

            codeLanguage =
                formatModalCodeLanguage
                    ? formatModalCodeLanguage.value ||
                      "TEXT"
                    : "TEXT";
        }

        selectedParagraphIndexes.forEach(
            (paragraphIndex) => {

                setPendingFormat(
                    paragraphIndex,
                    formatType,
                    codeLanguage
                );

                updateParagraphVisual(
                    paragraphIndex
                );
            }
        );

        showFormatMessage(
            `${selectedParagraphIndexes.size} paragraph${selectedParagraphIndexes.size === 1 ? "" : "s"} marked as ${formatType}.`,
            ""
        );

        updateFormatToolbar();
    }


    /* ========================================
       UPDATE CODE LANGUAGE
       ======================================== */

    function updateSelectedCodeLanguage() {

        if (
            previewMode ||
            selectedParagraphIndexes.size ===
            0
        ) {

            return;
        }

        if (!formatModalCodeLanguage) {

            return;
        }

        const codeLanguage =
            formatModalCodeLanguage.value;

        if (!codeLanguage) {

            return;
        }

        selectedParagraphIndexes.forEach(
            (paragraphIndex) => {

                const currentFormat =
                    getEffectiveFormat(
                        paragraphIndex
                    );

                if (
                    currentFormat.formatType !==
                    "CODE"
                ) {

                    return;
                }

                setPendingFormat(
                    paragraphIndex,
                    "CODE",
                    codeLanguage
                );

                updateParagraphVisual(
                    paragraphIndex
                );
            }
        );

        showFormatMessage(
            `Code language set to ${codeLanguage}.`,
            ""
        );

        updateFormatToolbar();
    }


    /* ========================================
       UPDATE TOOLBAR
       ======================================== */

    function updateFormatToolbar() {

        const selectedCount =
            selectedParagraphIndexes.size;


        /* ========================================
           SELECTED COUNT
           ======================================== */

        if (formatModalSelectedCount) {

            formatModalSelectedCount.textContent =
                `${selectedCount} paragraph${selectedCount === 1 ? "" : "s"}`;
        }


        /* ========================================
           FORMAT BUTTONS
           ======================================== */

        formatModalTypeButtons.forEach(
            (button) => {

                button.disabled =
                    selectedCount === 0 ||
                    previewMode ||
                    loadingFormatting ||
                    savingFormatting;

                button.classList.remove(
                    "active"
                );
            }
        );


        /* ========================================
           SELECTED FORMATS
           ======================================== */

        if (selectedCount > 0) {

            const selectedFormats =
                Array.from(
                    selectedParagraphIndexes
                ).map(
                    (paragraphIndex) =>
                        getEffectiveFormat(
                            paragraphIndex
                        )
                );

            const firstFormatType =
                selectedFormats[0]
                    .formatType;

            const sameFormatType =
                selectedFormats.every(
                    (format) =>
                        format.formatType ===
                        firstFormatType
                );


            /* ========================================
               ACTIVE FORMAT BUTTON
               ======================================== */

            if (sameFormatType) {

                const activeButton =
                    Array.from(
                        formatModalTypeButtons
                    ).find(
                        (button) =>
                            button.dataset.formatType ===
                            firstFormatType
                    );

                if (activeButton) {

                    activeButton.classList.add(
                        "active"
                    );
                }
            }


            /* ========================================
               CODE LANGUAGE
               ======================================== */

            const allCode =
                selectedFormats.every(
                    (format) =>
                        format.formatType ===
                        "CODE"
                );

            if (formatModalCodeLanguage) {

                formatModalCodeLanguage.disabled =
                    !allCode ||
                    previewMode ||
                    loadingFormatting ||
                    savingFormatting;

                if (allCode) {

                    const firstLanguage =
                        selectedFormats[0]
                            .codeLanguage ||
                        "TEXT";

                    const sameLanguage =
                        selectedFormats.every(
                            (format) =>
                                (
                                    format.codeLanguage ||
                                    "TEXT"
                                ) ===
                                firstLanguage
                        );

                    formatModalCodeLanguage.value =
                        sameLanguage
                            ? firstLanguage
                            : "";

                } else {

                    formatModalCodeLanguage.value =
                        "";
                }
            }

        } else if (
            formatModalCodeLanguage
        ) {

            formatModalCodeLanguage.disabled =
                true;

            formatModalCodeLanguage.value =
                "";
        }


        /* ========================================
           PREVIEW BUTTON
           ======================================== */

        if (formatModalPreviewButton) {

            formatModalPreviewButton.disabled =
                loadingFormatting ||
                savingFormatting ||
                formatDocumentParagraphs.length ===
                0;
        }


        /* ========================================
           SAVE BUTTON
           ======================================== */

        if (formatModalSaveButton) {

            formatModalSaveButton.disabled =
                loadingFormatting ||
                savingFormatting ||
                pendingFormats.size ===
                0;
        }
    }


    /* ========================================
       TOGGLE PREVIEW
       ======================================== */

    function toggleFormattingPreview() {

        if (
            !formatModalEditor ||
            formatDocumentParagraphs.length ===
            0
        ) {

            return;
        }

        previewMode =
            !previewMode;

        formatModalEditor.classList.toggle(
            "preview-mode",
            previewMode
        );

        if (formatModalPreviewButton) {

            formatModalPreviewButton.textContent =
                previewMode
                    ? "Back to Edit"
                    : "Preview";
        }

        updateAllParagraphVisuals();

        updateFormatToolbar();

        if (previewMode) {

            showFormatMessage(
                "Preview mode. Changes have not been saved yet.",
                ""
            );

        } else {

            showFormatMessage(
                "Back to formatting mode.",
                ""
            );
        }
    }


    /* ========================================
       SAVE FORMATTING
       ======================================== */

    async function saveDocumentFormatting() {

        const documentId =
            getFormatDocumentId();

        if (
            !documentId ||
            pendingFormats.size === 0
        ) {

            return;
        }

        savingFormatting =
            true;

        clearFormatMessage();

        updateFormatToolbar();

        if (formatModalSaveButton) {

            formatModalSaveButton.textContent =
                "Saving...";
        }

        const updates =
            Array.from(
                pendingFormats.values()
            ).sort(
                (first, second) =>
                    first.paragraphIndex -
                    second.paragraphIndex
            );

        try {

            const response =
                await fetch(
                    `/cf_automation/documents/${documentId}/formats`,
                    {
                        method:
                            "PUT",

                        credentials:
                            "same-origin",

                        headers: {
                            "Content-Type":
                                "application/json"
                        },

                        body:
                            JSON.stringify(
                                updates
                            )
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

                throw new Error(
                    "Unable to save document formatting."
                );
            }


            /* ========================================
               MERGE SAVED FORMATS
               ======================================== */

            pendingFormats.forEach(
                (
                    format,
                    paragraphIndex
                ) => {

                    persistedFormats.set(
                        paragraphIndex,
                        {
                            paragraphIndex:
                                paragraphIndex,

                            formatType:
                                format.formatType,

                            codeLanguage:
                                format.codeLanguage
                        }
                    );
                }
            );

            pendingFormats.clear();

            updateAllParagraphVisuals();


            /* ========================================
               UPDATE SHARED FORMAT CACHE
               ======================================== */

            window.currentDocumentFormats =
                Array.from(
                    persistedFormats.values()
                ).sort(
                    (first, second) =>
                        first.paragraphIndex -
                        second.paragraphIndex
                );


            /* ========================================
               REFRESH DOCUMENT DETAILS
               ======================================== */

            if (
                typeof window.loadDocumentDetails ===
                "function"
            ) {

                await window.loadDocumentDetails();
            }


            /* ========================================
               SUCCESS MESSAGE
               ======================================== */

            showFormatMessage(
                "Document formatting saved successfully.",
                "success"
            );

        } catch (error) {

            console.error(
                "Unable to save document formatting.",
                error
            );

            showFormatMessage(
                error.message ||
                "Unable to save document formatting.",
                "error"
            );

        } finally {

            savingFormatting =
                false;

            if (formatModalSaveButton) {

                formatModalSaveButton.textContent =
                    "Save Formatting";
            }

            updateFormatToolbar();
        }
    }


    /* ========================================
       LOADING STATE
       ======================================== */

    function setFormatLoading(
        isLoading
    ) {

        loadingFormatting =
            isLoading;

        if (formatModalLoading) {

            formatModalLoading.hidden =
                !isLoading;
        }

        if (formatModalError) {

            formatModalError.hidden =
                true;
        }

        if (
            formatModalEditor &&
            isLoading
        ) {

            formatModalEditor.hidden =
                true;
        }

        updateFormatToolbar();
    }


    /* ========================================
       ERROR STATE
       ======================================== */

    function showFormatError(
        message
    ) {

        loadingFormatting =
            false;

        if (formatModalLoading) {

            formatModalLoading.hidden =
                true;
        }

        if (formatModalEditor) {

            formatModalEditor.hidden =
                true;
        }

        if (formatModalError) {

            formatModalError.textContent =
                message;

            formatModalError.hidden =
                false;
        }

        updateFormatToolbar();
    }


    /* ========================================
       MESSAGE
       ======================================== */

    function showFormatMessage(
        message,
        type
    ) {

        if (!formatModalMessage) {

            return;
        }

        formatModalMessage.textContent =
            message;

        formatModalMessage.classList.remove(
            "success",
            "error"
        );

        if (type) {

            formatModalMessage.classList.add(
                type
            );
        }
    }


    /* ========================================
       CLEAR MESSAGE
       ======================================== */

    function clearFormatMessage() {

        if (!formatModalMessage) {

            return;
        }

        formatModalMessage.textContent =
            "";

        formatModalMessage.classList.remove(
            "success",
            "error"
        );
    }


    /* ========================================
       OPEN BUTTON
       ======================================== */

    if (formatModalOpenButton) {

        formatModalOpenButton.addEventListener(
            "click",
            () => {

                openFormatDocumentModal();
            }
        );
    }


    /* ========================================
       CLOSE BUTTON
       ======================================== */

    if (formatModalCloseButton) {

        formatModalCloseButton.addEventListener(
            "click",
            () => {

                closeFormatDocumentModal();
            }
        );
    }


    /* ========================================
       FORMAT TYPE BUTTONS
       ======================================== */

    formatModalTypeButtons.forEach(
        (button) => {

            button.addEventListener(
                "click",
                () => {

                    const formatType =
                        button.dataset.formatType;

                    if (!formatType) {

                        return;
                    }

                    applyFormatToSelection(
                        formatType
                    );
                }
            );
        }
    );


    /* ========================================
       CODE LANGUAGE
       ======================================== */

    if (formatModalCodeLanguage) {

        formatModalCodeLanguage.addEventListener(
            "change",
            () => {

                updateSelectedCodeLanguage();
            }
        );
    }


    /* ========================================
       PREVIEW
       ======================================== */

    if (formatModalPreviewButton) {

        formatModalPreviewButton.addEventListener(
            "click",
            () => {

                toggleFormattingPreview();
            }
        );
    }


    /* ========================================
       SAVE
       ======================================== */

    if (formatModalSaveButton) {

        formatModalSaveButton.addEventListener(
            "click",
            () => {

                saveDocumentFormatting();
            }
        );
    }


    /* ========================================
       EDITOR CLICK
       ======================================== */

    if (formatModalEditor) {

        formatModalEditor.addEventListener(
            "click",
            handleParagraphClick
        );


        /* ========================================
           MOUSE DRAG SELECTION
           ======================================== */

        formatModalEditor.addEventListener(
            "mouseup",
            () => {

                handleMouseTextSelection();
            }
        );
    }


    /* ========================================
       ESCAPE
       ======================================== */

    document.addEventListener(
        "keydown",
        (event) => {

            if (
                event.key ===
                "Escape" &&
                formatModal &&
                formatModal.classList.contains(
                    "active"
                )
            ) {

                closeFormatDocumentModal();
            }
        }
    );


    /* ========================================
       NORMALIZE LINK URL
       ======================================== */

    function normalizeLinkUrl(
        value
    ) {

        if (!value) {

            return "";
        }

        const url =
            value.trim();

        if (!url) {

            return "";
        }

        if (
            url.startsWith(
                "http://"
            ) ||
            url.startsWith(
                "https://"
            )
        ) {

            return url;
        }


        /* ========================================
           REJECT OTHER SCHEMES
           ======================================== */

        if (
            /^[a-zA-Z][a-zA-Z\d+\-.]*:/.test(
                url
            )
        ) {

            return "";
        }

        return `https://${url}`;
    }


    /* ========================================
       INITIAL TOOLBAR STATE
       ======================================== */

    updateFormatToolbar();

})();


/* ========================================
   DOCUMENT TYPE MANAGEMENT
   ======================================== */

(() => {

    /* ========================================
       ELEMENTS
       ======================================== */

    const formatModal =
        document.getElementById("formatDocumentModal");

    const openFormatButton =
        document.getElementById("formatDocumentButton");

    const currentTypeElement =
        document.getElementById("formatDocumentCurrentType");

    const documentTypeButtons =
        document.querySelectorAll(
            ".format-document-type-action-button"
        );

    const paragraphFormatButtons =
        document.querySelectorAll(
            ".format-document-type-button"
        );

    const changeTypeModal =
        document.getElementById("changeDocumentTypeModal");

    const currentTypeValue =
        document.getElementById("changeDocumentTypeCurrentValue");

    const newTypeValue =
        document.getElementById("changeDocumentTypeNewValue");

    const cancelButton =
        document.getElementById("cancelDocumentTypeChangeButton");

    const confirmButton =
        document.getElementById("confirmDocumentTypeChangeButton");

    const messageElement =
        document.getElementById("formatDocumentMessage");


    /* ========================================
       STATE
       ======================================== */

    let currentDocumentType = null;

    let pendingDocumentType = null;

    let updatingDocumentType = false;

    let loadingDocumentType = false;

    let requestVersion = 0;


    /* ========================================
       ALLOWED QUIZ FORMATS
       ======================================== */

    const quizAllowedFormats =
        new Set([
            "CODE"
        ]);


    /* ========================================
       DOCUMENT ID
       ======================================== */

    function getDocumentId() {

        if (window.currentDocumentId) {
            return window.currentDocumentId;
        }

        const pathParts =
            window.location.pathname
                .split("/")
                .filter(Boolean);

        const index =
            pathParts.indexOf("documents");

        if (index === -1) {
            return null;
        }

        const documentId =
            pathParts[index + 1];

        if (!documentId || !/^\d+$/.test(documentId)) {
            return null;
        }

        return documentId;
    }


    /* ========================================
       UPDATE TYPE BAR
       ======================================== */

    function updateDocumentTypeBar() {

        if (currentTypeElement) {

            currentTypeElement.textContent =
                currentDocumentType || "Loading...";
        }

        documentTypeButtons.forEach(button => {

            const buttonType =
                button.dataset.documentType;

            const isCurrent =
                buttonType === currentDocumentType;

            button.disabled =
                isCurrent ||
                updatingDocumentType ||
                loadingDocumentType ||
                !currentDocumentType;

            button.classList.toggle(
                "active",
                isCurrent
            );

            button.setAttribute(
                "aria-pressed",
                String(isCurrent)
            );
        });
    }


    /* ========================================
       UPDATE PARAGRAPH FORMAT BUTTONS
       ======================================== */

    function updateAvailableFormats() {

        paragraphFormatButtons.forEach(button => {

            const formatType =
                button.dataset.formatType;

            const isRestricted =
                currentDocumentType === "QUIZ" &&
                !quizAllowedFormats.has(formatType);

            button.hidden =
                isRestricted;

            button.style.display =
                isRestricted ? "none" : "";

            if (isRestricted) {

                button.classList.remove("active");
            }
        });
    }


    /* ========================================
       SYNC UI
       ======================================== */

    function updateDocumentTypeUI() {

        updateDocumentTypeBar();

        updateAvailableFormats();
    }


    /* ========================================
       LOAD CURRENT DOCUMENT TYPE
       ======================================== */

    async function loadDocumentType() {

        const documentId =
            getDocumentId();

        if (!documentId) {
            return;
        }

        const thisRequest =
            ++requestVersion;

        loadingDocumentType = true;

        currentDocumentType = null;

        updateDocumentTypeUI();

        try {

            const response =
                await fetch(
                    `/cf_automation/documents/${documentId}`,
                    {
                        method: "GET",
                        credentials: "same-origin"
                    }
                );

            if (
                response.status === 401 ||
                response.status === 403
            ) {

                window.location.href =
                    "/automation/unauthorized";

                return;
            }

            if (!response.ok) {

                throw new Error(
                    "Unable to load document type."
                );
            }

            const documentData =
                await response.json();

            if (thisRequest !== requestVersion) {
                return;
            }

            currentDocumentType =
                documentData.documentType || "DOCUMENT";

            if (
                window.currentDocumentData &&
                String(window.currentDocumentData.id) ===
                String(documentId)
            ) {

                window.currentDocumentData.documentType =
                    currentDocumentType;
            }

        } catch (error) {

            if (thisRequest !== requestVersion) {
                return;
            }

            console.error(
                "Unable to load document type.",
                error
            );

            showMessage(
                "Unable to load document type.",
                true
            );

        } finally {

            if (thisRequest === requestVersion) {

                loadingDocumentType = false;

                updateDocumentTypeUI();
            }
        }
    }


    /* ========================================
       SHOW MESSAGE
       ======================================== */

    function showMessage(text, isError = false) {

        if (!messageElement) {
            return;
        }

        messageElement.textContent = text;

        messageElement.classList.toggle(
            "error",
            isError
        );
    }


    /* ========================================
       OPEN CONFIRMATION MODAL
       ======================================== */

    function openChangeTypeModal(newType) {

        if (
            !currentDocumentType ||
            updatingDocumentType ||
            loadingDocumentType ||
            newType === currentDocumentType
        ) {
            return;
        }

        pendingDocumentType =
            newType;

        if (currentTypeValue) {

            currentTypeValue.textContent =
                currentDocumentType;
        }

        if (newTypeValue) {

            newTypeValue.textContent =
                newType;
        }

        if (changeTypeModal) {

            changeTypeModal.hidden = false;

            changeTypeModal.style.display =
                "flex";
        }
    }


    /* ========================================
       CLOSE CONFIRMATION MODAL
       ======================================== */

    function closeChangeTypeModal() {

        if (updatingDocumentType) {
            return;
        }

        pendingDocumentType = null;

        if (changeTypeModal) {

            changeTypeModal.hidden = true;

            changeTypeModal.style.display =
                "none";
        }
    }


    /* ========================================
       UPDATE DOCUMENT TYPE
       ======================================== */

    async function confirmDocumentTypeChange() {

        const documentId =
            getDocumentId();

        if (
            !documentId ||
            !pendingDocumentType ||
            updatingDocumentType
        ) {
            return;
        }

        const newType =
            pendingDocumentType;

        updatingDocumentType = true;

        updateDocumentTypeBar();

        if (confirmButton) {

            confirmButton.disabled = true;

            confirmButton.textContent =
                "Updating...";
        }

        try {

            const response =
                await fetch(
                    `/cf_automation/documents/${documentId}/type`,
                    {
                        method: "PATCH",

                        credentials: "same-origin",

                        headers: {
                            "Content-Type":
                                "application/json"
                        },

                        body:
                            JSON.stringify(newType)
                    }
                );

            if (
                response.status === 401 ||
                response.status === 403
            ) {

                window.location.href =
                    "/automation/unauthorized";

                return;
            }

            if (!response.ok) {

                throw new Error(
                    "Unable to update document type."
                );
            }

            currentDocumentType =
                newType;

            if (
                window.currentDocumentData &&
                String(window.currentDocumentData.id) ===
                String(documentId)
            ) {

                window.currentDocumentData.documentType =
                    newType;
            }

            pendingDocumentType = null;

            if (changeTypeModal) {

                changeTypeModal.hidden = true;

                changeTypeModal.style.display =
                    "none";
            }

            showMessage(
                `Document type updated to ${newType}.`
            );

        } catch (error) {

            console.error(
                "Unable to update document type.",
                error
            );

            showMessage(
                error.message ||
                "Unable to update document type.",
                true
            );

        } finally {

            updatingDocumentType = false;

            if (confirmButton) {

                confirmButton.disabled = false;

                confirmButton.textContent =
                    "Confirm Change";
            }

            updateDocumentTypeUI();
        }
    }


    /* ========================================
       DOCUMENT TYPE BUTTON EVENTS
       ======================================== */

    documentTypeButtons.forEach(button => {

        button.addEventListener(
            "click",
            () => {

                const newType =
                    button.dataset.documentType;

                if (!newType) {
                    return;
                }

                openChangeTypeModal(newType);
            }
        );
    });


    /* ========================================
       CONFIRMATION EVENTS
       ======================================== */

    if (cancelButton) {

        cancelButton.addEventListener(
            "click",
            closeChangeTypeModal
        );
    }

    if (confirmButton) {

        confirmButton.addEventListener(
            "click",
            confirmDocumentTypeChange
        );
    }


    /* ========================================
       PREVENT DISALLOWED QUIZ FORMATS
       ======================================== */

    paragraphFormatButtons.forEach(button => {

        button.addEventListener(
            "click",
            event => {

                const formatType =
                    button.dataset.formatType;

                if (
                    !currentDocumentType ||
                    (
                        currentDocumentType === "QUIZ" &&
                        !quizAllowedFormats.has(formatType)
                    )
                ) {

                    event.preventDefault();

                    event.stopImmediatePropagation();
                }

            },
            true
        );
    });


    /* ========================================
       LOAD WHEN FORMAT PAGE OPENS
       ======================================== */

    if (openFormatButton) {

        openFormatButton.addEventListener(
            "click",
            () => {

                loadDocumentType();
            }
        );
    }


    /* ========================================
       CLOSE CONFIRMATION WITH ESCAPE
       ======================================== */

    document.addEventListener(
        "keydown",
        event => {

            if (
                event.key === "Escape" &&
                changeTypeModal &&
                !changeTypeModal.hidden
            ) {

                event.stopImmediatePropagation();

                closeChangeTypeModal();
            }
        },
        true
    );


    /* ========================================
       INITIAL STATE
       ======================================== */

    if (changeTypeModal) {

        changeTypeModal.hidden = true;

        changeTypeModal.style.display =
            "none";
    }

    updateDocumentTypeUI();

})();
