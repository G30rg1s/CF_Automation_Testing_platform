
/* ========================================
   DOCUMENT QUIZ RENDERER
   ======================================== */

(() => {

    "use strict";


    /* ========================================
       STATE
       ======================================== */

    let currentDocumentId = null;

    let questions = [];

    let correctAnswers = {};

    let userAnswers = {};

    let submitted = false;


    /* ========================================
       STORAGE
       ======================================== */

    const STORAGE_PREFIX =
        "document-quiz-";

    const ACTIVE_QUIZ_KEY =
        "automation-active-quiz-id";


    /* ========================================
       ELEMENTS
       ======================================== */

    const quizRoot =
        document.getElementById("documentQuiz");

    const quizTitle =
        document.getElementById("documentQuizTitle");

    const quizProgress =
        document.getElementById("documentQuizProgress");

    const quizQuestions =
        document.getElementById("documentQuizQuestions");

    const quizActions =
        document.getElementById("documentQuizActions");

    const quizLoading =
        document.getElementById("documentQuizLoading");

    const quizError =
        document.getElementById("documentQuizError");

    const quizResults =
        document.getElementById("documentQuizResults");

    const submitButton =
        document.getElementById("documentQuizSubmitButton");

    const discardButton =
        document.getElementById("documentQuizDiscardButton");

    const scoreElement =
        document.getElementById("documentQuizScore");

    const correctCountElement =
        document.getElementById("documentQuizCorrectCount");

    const totalCountElement =
        document.getElementById("documentQuizTotalCount");


    /* ========================================
       PATTERNS
       ======================================== */

    const QUESTION_PATTERN =
        /^(\d+)[.)]\s*(.+)$/s;

    const OPTION_PATTERN =
        /^([A-D])[.)]\s*(.+)$/is;

    const ANSWER_PATTERN =
        /^#\s*(\d+)\s*[-–]\s*([A-D])\s*$/i;


    /* ========================================
       STORAGE HELPERS
       ======================================== */

    function getStorageKey(documentId = currentDocumentId) {

        return STORAGE_PREFIX + documentId;
    }


    function readStoredQuiz(documentId) {

        if (documentId == null) {
            return null;
        }

        try {

            const raw =
                localStorage.getItem(
                    getStorageKey(documentId)
                );

            if (!raw) {
                return null;
            }

            const parsed =
                JSON.parse(raw);

            if (
                !parsed ||
                typeof parsed !== "object" ||
                Array.isArray(parsed)
            ) {
                return null;
            }

            /* ========================================
               SUPPORT OLD SAVED ANSWER FORMAT
               ======================================== */

            if (
                !Object.prototype.hasOwnProperty.call(
                    parsed,
                    "answers"
                )
            ) {

                return {
                    answers: parsed,
                    submitted: false
                };
            }

            return {
                answers:
                    parsed.answers &&
                    typeof parsed.answers === "object" &&
                    !Array.isArray(parsed.answers)
                        ? parsed.answers
                        : {},

                submitted:
                    parsed.submitted === true
            };

        } catch (error) {

            console.warn(
                "Unable to restore quiz progress.",
                error
            );

            return null;
        }
    }


    function saveQuizState() {

        if (currentDocumentId == null) {
            return;
        }

        try {

            localStorage.setItem(
                getStorageKey(),
                JSON.stringify({
                    answers: userAnswers,
                    submitted: submitted
                })
            );

        } catch (error) {

            console.warn(
                "Unable to save quiz progress.",
                error
            );
        }
    }


    function clearQuizStorage(documentId) {

        if (documentId == null) {
            return;
        }

        try {

            localStorage.removeItem(
                getStorageKey(documentId)
            );

        } catch (error) {

            console.warn(
                "Unable to clear quiz progress.",
                error
            );
        }
    }


    /* ========================================
       ACTIVATE QUIZ DOCUMENT
       ======================================== */

    function activateQuizDocument(documentId) {

        const nextId =
            String(documentId);

        try {

            const previousId =
                localStorage.getItem(
                    ACTIVE_QUIZ_KEY
                );

            /* ========================================
               DIFFERENT QUIZ SELECTED
               ======================================== */

            if (
                previousId &&
                previousId !== nextId
            ) {

                clearQuizStorage(
                    previousId
                );
            }

            /* ========================================
               KEEP ONLY CURRENT QUIZ DATA
               ======================================== */

            const staleKeys = [];

            for (
                let index = 0;
                index < localStorage.length;
                index++
            ) {

                const key =
                    localStorage.key(index);

                if (
                    key &&
                    key.startsWith(STORAGE_PREFIX) &&
                    key !== getStorageKey(nextId)
                ) {

                    staleKeys.push(key);
                }
            }

            staleKeys.forEach(key => {

                localStorage.removeItem(key);
            });

            localStorage.setItem(
                ACTIVE_QUIZ_KEY,
                nextId
            );

        } catch (error) {

            console.warn(
                "Unable to activate quiz session.",
                error
            );
        }
    }


    /* ========================================
       CLEAR ALL QUIZ STORAGE
       ======================================== */

    function clearAllQuizStorage() {

        try {

            const keysToRemove = [];

            for (
                let index = 0;
                index < localStorage.length;
                index++
            ) {

                const key =
                    localStorage.key(index);

                if (
                    key === ACTIVE_QUIZ_KEY ||
                    (
                        key &&
                        key.startsWith(STORAGE_PREFIX)
                    )
                ) {

                    keysToRemove.push(key);
                }
            }

            keysToRemove.forEach(key => {

                localStorage.removeItem(key);
            });

        } catch (error) {

            console.warn(
                "Unable to clear quiz session.",
                error
            );
        }
    }


    /* ========================================
       PARAGRAPH FORMATS
       ======================================== */

    function createFormatMap(formats) {

        const formatMap =
            new Map();

        if (!Array.isArray(formats)) {
            return formatMap;
        }

        formats.forEach(format => {

            const index =
                Number(format.paragraphIndex);

            if (
                !Number.isInteger(index) ||
                index < 0
            ) {
                return;
            }

            formatMap.set(index, {
                type:
                    format.formatType ||
                    "PARAGRAPH",

                language:
                    format.codeLanguage ||
                    "TEXT"
            });
        });

        return formatMap;
    }



/* ========================================
   EXTRACT PARAGRAPHS
   ======================================== */

function extractParagraphs(htmlContent, formats) {

    const parser =
        new DOMParser();

    const parsed =
        parser.parseFromString(
            htmlContent,
            "text/html"
        );

    const formatMap =
        createFormatMap(formats);

    return Array.from(
        parsed.body.querySelectorAll("p")
    ).map((element, index) => {

        const format =
            formatMap.get(index) || {
                type: "PARAGRAPH",
                language: "TEXT"
            };

        /*
         * Clone the paragraph so that replacing
         * <br> elements does not modify the
         * original parsed HTML.
         */
        const clone =
            element.cloneNode(true);

        /*
         * Convert HTML line breaks into actual
         * newline characters.
         */
        clone.querySelectorAll("br").forEach(br => {

            br.replaceWith(
                document.createTextNode("\n")
            );
        });

        return {
            text:
                (clone.textContent || "")
                    .trim(),

            type:
                format.type,

            language:
                format.language
        };
    });
}



    /* ========================================
       PARSE QUIZ
       ======================================== */

    function parseQuiz(htmlContent, formats) {

        const paragraphs =
            extractParagraphs(
                htmlContent,
                formats
            );

        const parsedQuestions = [];

        const answers = {};

        let activeQuestion = null;

        paragraphs.forEach(paragraph => {

            const text =
                paragraph.text;

            if (!text) {
                return;
            }


            /* ========================================
               CORRECT ANSWER
               ======================================== */

            const answerMatch =
                text.match(ANSWER_PATTERN);

            if (answerMatch) {

                const number =
                    String(Number(answerMatch[1]));

                answers[number] =
                    answerMatch[2].toUpperCase();

                return;
            }


            /* ========================================
               CODE BLOCK
               ======================================== */

            if (paragraph.type === "CODE") {

                if (activeQuestion) {

                    activeQuestion.content.push({
                        type: "CODE",
                        text: text,
                        language: paragraph.language
                    });
                }

                return;
            }


            /* ========================================
               QUESTION
               ======================================== */

            const questionMatch =
                text.match(QUESTION_PATTERN);

            if (questionMatch) {

                const number =
                    String(Number(questionMatch[1]));

                activeQuestion = {
                    number: number,
                    title: questionMatch[2].trim(),
                    content: [],
                    options: []
                };

                parsedQuestions.push(
                    activeQuestion
                );

                return;
            }


            /* ========================================
               ANSWER OPTION
               ======================================== */

            const optionMatch =
                text.match(OPTION_PATTERN);

            if (
                optionMatch &&
                activeQuestion
            ) {

                activeQuestion.options.push({
                    letter:
                        optionMatch[1].toUpperCase(),

                    text:
                        optionMatch[2].trim()
                });

                return;
            }


            /* ========================================
               OTHER CONTENT
               ======================================== */

            if (activeQuestion) {

                activeQuestion.content.push({
                    type: paragraph.type,
                    text: text,
                    language: paragraph.language
                });
            }
        });

        return {
            questions: parsedQuestions,
            answers: answers
        };
    }


    /* ========================================
       CREATE ELEMENT
       ======================================== */

    function createElement(tag, className, text) {

        const element =
            document.createElement(tag);

        if (className) {
            element.className = className;
        }

        if (text != null) {
            element.textContent = text;
        }

        return element;
    }


/* ========================================
   QUIZ CODE LANGUAGE / HIGHLIGHTING
   ======================================== */

function getQuizPrismLanguage(codeLanguage) {

    const languages = {
        JAVA: "java",
        JAVASCRIPT: "javascript",
        TYPESCRIPT: "typescript",
        HTML: "markup",
        CSS: "css",
        JSON: "json",
        SQL: "sql",
        XML: "markup",
        PYTHON: "python",
        BASH: "bash"
    };

    return languages[
        String(codeLanguage || "TEXT").toUpperCase()
    ] || null;
}

function highlightQuizCode(code, text, codeLanguage) {

    const language =
        getQuizPrismLanguage(codeLanguage);

    code.textContent = text;

    if (
        !language ||
        !window.Prism ||
        !window.Prism.languages ||
        !window.Prism.languages[language]
    ) {
        return;
    }

    code.classList.add("language-" + language);

    code.innerHTML = window.Prism.highlight(
        text,
        window.Prism.languages[language],
        language
    );
}




/* ========================================
   RENDER QUESTION CONTENT
   ======================================== */

function renderQuestionContent(question, container) {

    question.content.forEach(item => {

        if (item.type === "CODE") {

            const language =
                String(item.language || "TEXT").toUpperCase();

            const wrapper =
                createElement(
                    "div",
                    "document-format-code code-language-" +
                    language.toLowerCase()
                );

            const languageBadge =
                createElement(
                    "div",
                    "document-format-code-language",
                    language
                );

            const pre =
                createElement("pre");

            pre.classList.add("document-quiz-code");

            const code =
                createElement("code");

            highlightQuizCode(
                code,
                item.text,
                language
            );

            pre.appendChild(code);

            wrapper.appendChild(languageBadge);
            wrapper.appendChild(pre);

            container.appendChild(wrapper);

            return;
        }

        const paragraph =
            createElement(
                "p",
                "document-quiz-content-paragraph",
                item.text
            );

        container.appendChild(paragraph);
    });
}



    /* ========================================
       CREATE ANSWER OPTION
       ======================================== */

    function createOption(question, option) {

        const label =
            createElement(
                "label",
                "document-quiz-option"
            );

        label.dataset.question =
            question.number;

        label.dataset.answer =
            option.letter;

        const input =
            document.createElement("input");

        input.type = "radio";

        input.name =
            "document-quiz-question-" +
            currentDocumentId + "-" +
            question.number;

        input.value =
            option.letter;

        input.checked =
            userAnswers[question.number] ===
            option.letter;

        if (input.checked) {

            label.classList.add(
                "is-selected"
            );
        }

        const letter =
            createElement(
                "span",
                "document-quiz-option-letter",
                option.letter + ")"
            );

        const text =
            createElement(
                "span",
                "document-quiz-option-text",
                option.text
            );

        input.addEventListener(
            "change",
            () => {

                if (
                    submitted ||
                    !input.checked
                ) {
                    return;
                }

                userAnswers[question.number] =
                    option.letter;

                saveQuizState();

                updateQuestionSelection(
                    question.number
                );

                updateProgress();
            }
        );

        label.appendChild(input);
        label.appendChild(letter);
        label.appendChild(text);

        return label;
    }


    /* ========================================
       RENDER QUESTION
       ======================================== */

    function renderQuestion(question) {

        const card =
            createElement(
                "section",
                "document-quiz-question"
            );

        card.dataset.question =
            question.number;

        const header =
            createElement(
                "div",
                "document-quiz-question-header"
            );

        const number =
            createElement(
                "span",
                "document-quiz-question-number",
                question.number
            );

        const title =
            createElement(
                "h3",
                "document-quiz-question-title",
                question.title
            );

        header.appendChild(number);
        header.appendChild(title);

        card.appendChild(header);

        if (question.content.length > 0) {

            const content =
                createElement(
                    "div",
                    "document-quiz-question-content"
                );

            renderQuestionContent(
                question,
                content
            );

            card.appendChild(content);
        }

        const options =
            createElement(
                "fieldset",
                "document-quiz-options"
            );

        options.setAttribute(
            "aria-label",
            "Question " + question.number
        );

        question.options.forEach(option => {

            options.appendChild(
                createOption(
                    question,
                    option
                )
            );
        });

        card.appendChild(options);

        return card;
    }


    /* ========================================
       RENDER ALL QUESTIONS
       ======================================== */

    function renderQuestions() {

        quizQuestions.replaceChildren();

        questions.forEach(question => {

            quizQuestions.appendChild(
                renderQuestion(question)
            );
        });
    }


    /* ========================================
       UPDATE SELECTED STYLES
       ======================================== */

    function updateQuestionSelection(questionNumber) {

        const cards =
            quizQuestions.querySelectorAll(
                ".document-quiz-question"
            );

        cards.forEach(card => {

            if (
                card.dataset.question !==
                questionNumber
            ) {
                return;
            }

            card.querySelectorAll(
                ".document-quiz-option"
            ).forEach(option => {

                option.classList.toggle(
                    "is-selected",
                    option.dataset.answer ===
                    userAnswers[questionNumber]
                );
            });
        });
    }


    /* ========================================
       UPDATE PROGRESS
       ======================================== */

    function updateProgress() {

        const answered =
            questions.filter(question =>
                Boolean(
                    userAnswers[question.number]
                )
            ).length;

        if (quizProgress) {

            quizProgress.textContent =
                answered + " / " +
                questions.length;
        }
    }


    /* ========================================
       SCORE CALCULATION
       ======================================== */

    function calculateScore() {

        const total =
            questions.length;

        let answered = 0;

        let correct = 0;

        questions.forEach(question => {

            const selected =
                userAnswers[question.number];

            const expected =
                correctAnswers[question.number];

            if (!selected) {
                return;
            }

            answered++;

            if (selected === expected) {
                correct++;
            }
        });

        const incorrect =
            answered - correct;

        const unanswered =
            total - answered;

        const answeredPercentage =
            answered > 0
                ? (correct / answered) * 100
                : 0;

        const totalPercentage =
            total > 0
                ? (correct / total) * 100
                : 0;

        return {
            total,
            answered,
            correct,
            incorrect,
            unanswered,
            answeredPercentage,
            totalPercentage
        };
    }


    /* ========================================
       SHOW CORRECT AND INCORRECT ANSWERS
       ======================================== */

    function highlightAnswers() {

        questions.forEach(question => {

            const expected =
                correctAnswers[question.number];

            const selected =
                userAnswers[question.number];

            const card =
                Array.from(
                    quizQuestions.querySelectorAll(
                        ".document-quiz-question"
                    )
                ).find(element =>
                    element.dataset.question ===
                    question.number
                );

            if (!card) {
                return;
            }

            if (!selected) {

                card.classList.add(
                    "is-unanswered"
                );

            } else if (selected === expected) {

                card.classList.add(
                    "is-answered-correct"
                );

            } else {

                card.classList.add(
                    "is-answered-incorrect"
                );
            }

            card.querySelectorAll(
                ".document-quiz-option"
            ).forEach(option => {

                const letter =
                    option.dataset.answer;

                const input =
                    option.querySelector(
                        'input[type="radio"]'
                    );

                option.classList.remove(
                    "is-selected"
                );

                if (letter === expected) {

                    option.classList.add(
                        "is-correct"
                    );
                }

                if (
                    letter === selected &&
                    selected !== expected
                ) {

                    option.classList.add(
                        "is-incorrect"
                    );
                }

                option.classList.add(
                    "is-locked"
                );

                if (input) {
                    input.disabled = true;
                }
            });
        });
    }


    /* ========================================
       RENDER SCORE DETAILS
       ======================================== */

    function renderScoreDetails(result) {

        if (scoreElement) {

            scoreElement.textContent =
                result.totalPercentage.toFixed(1) + "%";
        }

        if (correctCountElement) {

            correctCountElement.textContent =
                result.correct;
        }

        if (totalCountElement) {

            totalCountElement.textContent =
                result.total;
        }

        const details =
            quizResults.querySelector(
                ".document-quiz-results-details"
            );

        if (!details) {
            return;
        }

        details.replaceChildren();

        const statistics = [
            ["Answered Questions", result.answered],
            ["Correct Answers", result.correct],
            ["Incorrect Answers", result.incorrect],
            ["Unanswered Questions", result.unanswered],
            ["Total Questions", result.total],
            [
                "Success Rate (Answered)",
                result.answeredPercentage.toFixed(1) + "%"
            ],
            [
                "Success Rate (Total)",
                result.totalPercentage.toFixed(1) + "%"
            ]
        ];

        statistics.forEach(([label, value]) => {

            const paragraph =
                createElement(
                    "p",
                    "document-quiz-result-stat"
                );

            paragraph.appendChild(
                document.createTextNode(
                    label + ": "
                )
            );

            paragraph.appendChild(
                createElement(
                    "strong",
                    "",
                    String(value)
                )
            );

            details.appendChild(
                paragraph
            );
        });
    }


    /* ========================================
       SHOW SUBMITTED STATE
       ======================================== */

    function showSubmittedState() {

        if (
            !quizResults ||
            !submitButton
        ) {
            return;
        }

        const result =
            calculateScore();

        highlightAnswers();

        renderScoreDetails(
            result
        );

        quizResults.hidden = false;

        submitButton.disabled = true;

        submitButton.textContent =
            "Quiz Submitted";

        updateProgress();
    }


    /* ========================================
       SUBMIT QUIZ
       ======================================== */

    function submitQuiz() {

        if (
            submitted ||
            questions.length === 0
        ) {
            return;
        }

        submitted = true;

        saveQuizState();

        showSubmittedState();

        quizResults.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
    }


    /* ========================================
       VALIDATE QUIZ
       ======================================== */

    function validateQuiz() {

        if (questions.length === 0) {

            throw new Error(
                "No quiz questions were found."
            );
        }

        const numbers =
            new Set(
                questions.map(question =>
                    question.number
                )
            );

        if (numbers.size !== questions.length) {

            throw new Error(
                "Duplicate question numbers were found."
            );
        }

        const missingAnswers =
            questions.filter(question =>
                !correctAnswers[question.number]
            );

        if (missingAnswers.length > 0) {

            throw new Error(
                "Missing correct answers for questions: " +
                missingAnswers
                    .map(question => question.number)
                    .join(", ")
            );
        }

        const invalidOptions =
            questions.filter(question => {

                const letters =
                    question.options.map(
                        option => option.letter
                    );

                return (
                    letters.length < 2 ||
                    new Set(letters).size !==
                    letters.length ||
                    !letters.includes(
                        correctAnswers[question.number]
                    )
                );
            });

        if (invalidOptions.length > 0) {

            throw new Error(
                "Invalid options for questions: " +
                invalidOptions
                    .map(question => question.number)
                    .join(", ")
            );
        }
    }


    /* ========================================
       RESTORE SAVED ANSWERS
       ======================================== */

    function restoreQuizProgress(savedState) {

        userAnswers = {};

        submitted = false;

        if (!savedState) {
            return;
        }

        const savedAnswers =
            savedState.answers || {};

        questions.forEach(question => {

            const selected =
                savedAnswers[question.number];

            const availableLetters =
                question.options.map(
                    option => option.letter
                );

            if (
                typeof selected === "string" &&
                availableLetters.includes(selected)
            ) {

                userAnswers[question.number] =
                    selected;
            }
        });

        submitted =
            savedState.submitted === true;
    }


    /* ========================================
       RENDER DOCUMENT QUIZ
       ======================================== */

    function renderDocumentQuiz(
        documentData,
        formats = []
    ) {

        if (
            !quizRoot ||
            !documentData ||
            documentData.documentType !== "QUIZ"
        ) {
            return;
        }

        if (
            documentData.id == null
        ) {

            console.error(
                "Quiz document ID is missing."
            );

            return;
        }

        currentDocumentId =
            String(documentData.id);

        /* ========================================
           ACTIVATE OR RESTORE SESSION
           ======================================== */

        activateQuizDocument(
            currentDocumentId
        );

        const savedState =
            readStoredQuiz(
                currentDocumentId
            );

        /* ========================================
           RESET IN-MEMORY DATA
           ======================================== */

        questions = [];

        correctAnswers = {};

        userAnswers = {};

        submitted = false;


        /* ========================================
           INITIAL UI
           ======================================== */

        quizRoot.hidden = false;

        quizResults.hidden = true;

        quizActions.hidden = true;

        quizError.hidden = true;

        quizLoading.hidden = false;

        submitButton.disabled = false;

        submitButton.textContent =
            "Submit Quiz";

        if (quizTitle) {

            quizTitle.textContent =
                documentData.title || "Quiz";
        }

        try {

            /* ========================================
               PARSE HTML DOCUMENT
               ======================================== */

            const parsed =
                parseQuiz(
                    documentData.htmlContent || "",
                    formats
                );

            questions =
                parsed.questions;

            correctAnswers =
                parsed.answers;


            /* ========================================
               VALIDATION
               ======================================== */

            validateQuiz();


            /* ========================================
               RESTORE SAVED PROGRESS
               ======================================== */

            restoreQuizProgress(
                savedState
            );


            /* ========================================
               RENDER QUESTIONS
               ======================================== */

            renderQuestions();

            updateProgress();

            quizActions.hidden = false;


            /* ========================================
               RESTORE SUBMITTED RESULT
               ======================================== */

            if (submitted) {

                showSubmittedState();
            }

        } catch (error) {

            console.error(
                "Unable to render document quiz.",
                error
            );

            quizQuestions.replaceChildren();

            quizError.textContent =
                error.message ||
                "Unable to load quiz.";

            quizError.hidden = false;

        } finally {

            quizLoading.hidden = true;
        }
    }


    /* ========================================
       CLOSE DOCUMENT QUIZ
       ======================================== */

    function closeDocumentQuiz() {

        /*
         * IMPORTANT:
         * Closing the renderer clears in-memory data
         * but does NOT delete saved quiz progress.
         */

        currentDocumentId = null;

        questions = [];

        correctAnswers = {};

        userAnswers = {};

        submitted = false;

        if (quizQuestions) {

            quizQuestions.replaceChildren();
        }

        if (quizResults) {

            quizResults.hidden = true;
        }

        if (quizActions) {

            quizActions.hidden = true;
        }

        if (quizRoot) {

            quizRoot.hidden = true;
        }
    }


    /* ========================================
       DISCARD QUIZ
       ======================================== */

    function discardDocumentQuiz() {

        const discardedDocumentId =
            currentDocumentId;

        /* ========================================
           CLOSE FULLSCREEN IF ACTIVE
           ======================================== */

        const fullscreenModal =
            document.getElementById(
                "documentFullscreenModal"
            );

        if (
            fullscreenModal &&
            fullscreenModal.classList.contains("active") &&
            typeof window.closeDocumentFullscreenModal ===
                "function"
        ) {

            window.closeDocumentFullscreenModal();

        } else if (
            fullscreenModal &&
            fullscreenModal.classList.contains("active") &&
            typeof closeDocumentFullscreenModal ===
                "function"
        ) {

            closeDocumentFullscreenModal();
        }


        /* ========================================
           CLEAR SAVED QUIZ
           ======================================== */

        clearQuizStorage(
            discardedDocumentId
        );

        try {

            const activeId =
                localStorage.getItem(
                    ACTIVE_QUIZ_KEY
                );

            if (
                activeId ===
                String(discardedDocumentId)
            ) {

                localStorage.removeItem(
                    ACTIVE_QUIZ_KEY
                );
            }

        } catch (error) {

            console.warn(
                "Unable to clear active quiz.",
                error
            );
        }


        /* ========================================
           CLEAR OBJECTS AND UI
           ======================================== */

        closeDocumentQuiz();


        /* ========================================
           EXIT DOCUMENT
           ======================================== */

        window.history.back();
    }


    /* ========================================
       LOGOUT CLEANUP
       ======================================== */

    function clearAllDocumentQuizData() {

        clearAllQuizStorage();

        closeDocumentQuiz();
    }


    /* ========================================
       EVENTS
       ======================================== */

    if (submitButton) {

        submitButton.addEventListener(
            "click",
            submitQuiz
        );
    }

    if (discardButton) {

        discardButton.addEventListener(
            "click",
            discardDocumentQuiz
        );
    }


    /* ========================================
       PUBLIC API
       ======================================== */

    window.renderDocumentQuiz =
        renderDocumentQuiz;

    window.closeDocumentQuiz =
        closeDocumentQuiz;

    window.clearAllDocumentQuizData =
        clearAllDocumentQuizData;

})();
