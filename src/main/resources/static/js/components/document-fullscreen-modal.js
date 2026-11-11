
/* ========================================
   ELEMENTS
   ======================================== */

const documentFullscreenModal =
    document.getElementById("documentFullscreenModal");

const closeDocumentFullscreenModalButton =
    document.getElementById("closeDocumentFullscreenModalButton");

const documentFullscreenFrame =
    document.getElementById("documentFullscreenFrame");

const openDocumentFullscreenButton =
    document.getElementById("openDocumentFullscreenButton");


/* ========================================
   QUIZ FULLSCREEN ELEMENTS
   ======================================== */

const documentFullscreenQuizContainer =
    document.getElementById("documentFullscreenQuizContainer");

const documentQuiz =
    document.getElementById("documentQuiz");

const documentQuizOriginalParent =
    documentQuiz
        ? documentQuiz.parentElement
        : null;

const documentQuizOriginalNextSibling =
    documentQuiz
        ? documentQuiz.nextSibling
        : null;


/* ========================================
   RESTORE QUIZ TO NORMAL VIEW
   ======================================== */

function restoreDocumentQuizToNormalView() {

    if (
        !documentQuiz ||
        !documentQuizOriginalParent
    ) {
        return;
    }

    if (
        documentQuizOriginalNextSibling &&
        documentQuizOriginalNextSibling.parentNode ===
        documentQuizOriginalParent
    ) {

        documentQuizOriginalParent.insertBefore(
            documentQuiz,
            documentQuizOriginalNextSibling
        );

    } else {

        documentQuizOriginalParent.appendChild(
            documentQuiz
        );
    }
}


/* ========================================
   OPEN FULLSCREEN MODAL
   ======================================== */

function openDocumentFullscreenModal() {

    if (
        !documentFullscreenModal ||
        !documentFullscreenFrame
    ) {
        return;
    }

    const isQuiz =
        window.currentDocumentData?.documentType ===
        "QUIZ";


    /* ========================================
       QUIZ FULLSCREEN
       ======================================== */

    if (isQuiz) {

        if (
            !documentQuiz ||
            !documentFullscreenQuizContainer ||
            documentQuiz.hidden
        ) {

            console.error(
                "Quiz component is not available for fullscreen."
            );

            return;
        }

        /* ========================================
           HIDE NORMAL DOCUMENT FRAME
           ======================================== */

        documentFullscreenFrame.hidden =
            true;

        documentFullscreenFrame.srcdoc =
            "";


        /* ========================================
           MOVE EXISTING QUIZ
           ======================================== */

        documentFullscreenQuizContainer.appendChild(
            documentQuiz
        );

        documentFullscreenQuizContainer.hidden =
            false;

    } else {

        /* ========================================
           NORMAL DOCUMENT FULLSCREEN
           ======================================== */

        const sourceFrame =
            document.getElementById(
                "documentContentFrame"
            );

        if (!sourceFrame) {
            return;
        }

        const renderedContent =
            window.currentDocumentRenderedContent ||
            sourceFrame.srcdoc ||
            "";

        if (!renderedContent) {
            return;
        }

        /* ========================================
           HIDE QUIZ CONTAINER
           ======================================== */

        if (documentFullscreenQuizContainer) {

            documentFullscreenQuizContainer.hidden =
                true;
        }


        /* ========================================
           SHOW NORMAL DOCUMENT FRAME
           ======================================== */

        documentFullscreenFrame.hidden =
            false;

        documentFullscreenFrame.srcdoc =
            renderedContent;
    }


    /* ========================================
       OPEN MODAL
       ======================================== */

    documentFullscreenModal.classList.add(
        "active"
    );

    documentFullscreenModal.setAttribute(
        "aria-hidden",
        "false"
    );

    document.body.style.overflow =
        "hidden";
}


/* ========================================
   CLOSE FULLSCREEN MODAL
   ======================================== */

function closeDocumentFullscreenModal() {

    if (!documentFullscreenModal) {
        return;
    }


    /* ========================================
       RESTORE QUIZ
       ======================================== */

    restoreDocumentQuizToNormalView();

    if (documentFullscreenQuizContainer) {

        documentFullscreenQuizContainer.hidden =
            true;
    }


    /* ========================================
       RESET FULLSCREEN FRAME
       ======================================== */

    if (documentFullscreenFrame) {

        documentFullscreenFrame.srcdoc =
            "";

        documentFullscreenFrame.hidden =
            false;
    }


    /* ========================================
       CLOSE MODAL
       ======================================== */

    documentFullscreenModal.classList.remove(
        "active"
    );

    documentFullscreenModal.setAttribute(
        "aria-hidden",
        "true"
    );

    document.body.style.overflow =
        "";
}


/* ========================================
   OPEN BUTTON
   ======================================== */

if (openDocumentFullscreenButton) {

    openDocumentFullscreenButton.addEventListener(
        "click",
        () => {

            openDocumentFullscreenModal();
        }
    );
}


/* ========================================
   CLOSE BUTTON
   ======================================== */

if (closeDocumentFullscreenModalButton) {

    closeDocumentFullscreenModalButton.addEventListener(
        "click",
        () => {

            closeDocumentFullscreenModal();
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
            documentFullscreenModal &&
            documentFullscreenModal.classList.contains(
                "active"
            )
        ) {

            closeDocumentFullscreenModal();
        }
    }
);
