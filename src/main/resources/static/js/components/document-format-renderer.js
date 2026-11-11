
(() => {
    "use strict";

    /* ========================================
       LANGUAGE MAP
       ======================================== */

    function getDocumentPrismLanguage(codeLanguage) {
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

    /* ========================================
       NORMALIZE LINK
       ======================================== */

    function normalizeDocumentLinkUrl(value) {
        const url = String(value || "").trim();

        if (!url) {
            return null;
        }

        if (/^https?:\/\//i.test(url)) {
            return url;
        }

        if (/^[a-zA-Z][a-zA-Z\d+\-.]*:/.test(url)) {
            return null;
        }

        return `https://${url}`;
    }

    /* ========================================
       FORMAT MAP
       ======================================== */

    function createFormatMap(formats) {
        const formatMap = new Map();

        if (!Array.isArray(formats)) {
            return formatMap;
        }

        formats.forEach(format => {
            const paragraphIndex =
                Number(format.paragraphIndex);

            if (
                !Number.isInteger(paragraphIndex) ||
                paragraphIndex < 0
            ) {
                return;
            }

            formatMap.set(paragraphIndex, {
                formatType:
                    format.formatType || "PARAGRAPH",

                codeLanguage:
                    format.codeLanguage || null
            });
        });

        return formatMap;
    }

    function getDocumentFormat(formatMap, paragraphIndex) {
        return formatMap.get(paragraphIndex) || {
            formatType: "PARAGRAPH",
            codeLanguage: null
        };
    }

    /* ========================================
       PRESERVE <br> LINE BREAKS
       ======================================== */

    function getElementTextWithBreaks(element) {
        const clone = element.cloneNode(true);

        clone.querySelectorAll("br").forEach(br => {
            br.replaceWith(
                document.createTextNode("\n")
            );
        });

        return clone.textContent || "";
    }

    /* ========================================
       PRISM CODE HIGHLIGHTING
       ======================================== */

    function highlightDocumentCode(text, codeLanguage) {
        const prismLanguage =
            getDocumentPrismLanguage(codeLanguage);

        if (
            !prismLanguage ||
            !window.Prism ||
            !window.Prism.languages ||
            !window.Prism.languages[prismLanguage]
        ) {
            const temporary =
                document.createElement("div");

            temporary.textContent = text;

            return temporary.innerHTML;
        }

        return window.Prism.highlight(
            text,
            window.Prism.languages[prismLanguage],
            prismLanguage
        );
    }

    /* ========================================
       CREATE NORMAL PARAGRAPH
       ======================================== */

    function createParagraphElement(text) {
        const paragraph = document.createElement("p");

        paragraph.className =
            "document-format-paragraph";

        paragraph.textContent = text;

        return paragraph;
    }

    /* ========================================
       CREATE TITLE
       ======================================== */

    function createTitleElement(text) {
        const title = document.createElement("h1");

        title.className = "document-format-title";
        title.textContent = text;

        return title;
    }

    /* ========================================
       CREATE HEADING
       ======================================== */

    function createHeadingElement(text) {
        const heading = document.createElement("h2");

        heading.className = "document-format-heading";
        heading.textContent = text;

        return heading;
    }

    /* ========================================
       CREATE QUOTE
       ======================================== */

    function createQuoteElement(text) {
        const quote =
            document.createElement("blockquote");

        quote.className = "document-format-quote";
        quote.textContent = text;

        return quote;
    }

    /* ========================================
       CREATE LINK
       ======================================== */

    function createLinkElement(text) {
        const wrapper = document.createElement("p");

        wrapper.className =
            "document-format-link-wrapper";

        const url = normalizeDocumentLinkUrl(text);

        if (!url) {
            wrapper.textContent = text;
            return wrapper;
        }

        const link = document.createElement("a");

        link.className = "document-format-link";
        link.href = url;
        link.target = "_blank";
        link.rel = "noopener noreferrer";
        link.textContent = text;

        wrapper.appendChild(link);

        return wrapper;
    }

    /* ========================================
       CREATE CODE BLOCK
       ======================================== */

    function createCodeElement(text, codeLanguage) {
        const language =
            String(codeLanguage || "TEXT").toUpperCase();

        const wrapper = document.createElement("div");

        wrapper.className =
            "document-format-code code-language-" +
            language.toLowerCase();

        const languageBadge =
            document.createElement("div");

        languageBadge.className =
            "document-format-code-language";

        languageBadge.textContent = language;

        const pre = document.createElement("pre");
        const code = document.createElement("code");

        const prismLanguage =
            getDocumentPrismLanguage(language);

        if (prismLanguage) {
            code.className = "language-" + prismLanguage;
        }

        code.innerHTML =
            highlightDocumentCode(text, language);

        pre.appendChild(code);

        wrapper.appendChild(languageBadge);
        wrapper.appendChild(pre);

        return wrapper;
    }

    /* ========================================
       CREATE READ-ONLY HTML TABLE
       ======================================== */

    function createTableElement(sourceTable) {
        const wrapper = document.createElement("div");

        wrapper.className =
            "document-format-table-wrapper";

        const table = document.createElement("table");

        table.className = "document-format-table";

        Array.from(sourceTable.rows).forEach(sourceRow => {
            const row = document.createElement("tr");

            Array.from(sourceRow.cells).forEach(sourceCell => {
                const tag =
                    sourceCell.tagName.toLowerCase() === "th"
                        ? "th"
                        : "td";

                const cell = document.createElement(tag);

                // Text only: no executable HTML from Word cells.
                cell.textContent =
                    getElementTextWithBreaks(sourceCell);

                // Support rowspan / colspan when present.
                if (sourceCell.colSpan > 1) {
                    cell.colSpan = sourceCell.colSpan;
                }

                if (sourceCell.rowSpan > 1) {
                    cell.rowSpan = sourceCell.rowSpan;
                }

                row.appendChild(cell);
            });

            table.appendChild(row);
        });

        wrapper.appendChild(table);

        return wrapper;
    }

    /* ========================================
       CREATE FORMATTED ELEMENT
       ======================================== */

    function createFormattedElement(text, format) {
        switch (format.formatType) {
            case "TITLE":
                return createTitleElement(text);

            case "HEADING":
                return createHeadingElement(text);

            case "QUOTE":
                return createQuoteElement(text);

            case "LINK":
                return createLinkElement(text);

            case "CODE":
                return createCodeElement(
                    text,
                    format.codeLanguage
                );

            case "ARRAY":
            case "PARAGRAPH":
            default:
                return createParagraphElement(text);
        }
    }

    /* ========================================
       DOCUMENT STYLES
       ======================================== */

    function getFormattedDocumentStyles() {
        return `
            * {
                box-sizing: border-box;
            }

            html {
                background: #ffffff;
            }

            body {
                max-width: 1100px;
                margin: 0 auto;
                padding: 36px 42px;
                background: #ffffff;
                color: #1f2937;
                font-family: Arial, Helvetica, sans-serif;
                font-size: 15px;
                line-height: 1.75;
            }

            /* ========================================
               TEXT FORMATS
               ======================================== */

            .document-format-title {
                margin: 0 0 22px;
                color: #0f172a;
                font-size: 30px;
                font-weight: 800;
                line-height: 1.25;
            }

            .document-format-heading {
                margin: 28px 0 12px;
                color: #111827;
                font-size: 21px;
                font-weight: 700;
                line-height: 1.35;
            }

            .document-format-paragraph {
                margin: 0 0 14px;
                color: #374151;
                white-space: pre-wrap;
            }

            .document-format-quote {
                margin: 18px 0;
                padding: 12px 16px;
                border-left: 4px solid #8b5cf6;
                background: #f8fafc;
                color: #475569;
                font-style: italic;
                white-space: pre-wrap;
            }

            .document-format-link-wrapper {
                margin: 0 0 14px;
            }

            .document-format-link {
                color: #2563eb;
                font-weight: 600;
                text-decoration: underline;
                text-decoration-thickness: 1px;
                text-underline-offset: 3px;
                cursor: pointer;
            }

            .document-format-link:hover {
                color: #1d4ed8;
            }

            /* ========================================
               READ-ONLY WORD TABLES
               ======================================== */

            .document-format-table-wrapper {
                width: 100%;
                margin: 18px 0 24px;
                overflow-x: auto;
                border: 1px solid #cbd5e1;
                border-radius: 10px;
            }

            .document-format-table {
                width: 100%;
                min-width: max-content;
                border-collapse: collapse;
                color: #334155;
                font-size: 14px;
                line-height: 1.55;
            }

            .document-format-table th,
            .document-format-table td {
                padding: 11px 14px;
                border: 1px solid #cbd5e1;
                vertical-align: top;
                text-align: left;
                white-space: pre-wrap;
                overflow-wrap: anywhere;
            }

            .document-format-table th {
                background: #e2e8f0;
                color: #0f172a;
                font-weight: 700;
            }

            .document-format-table tbody tr:nth-child(even),
            .document-format-table > tr:nth-child(even) {
                background: #f8fafc;
            }

            /* ========================================
               CODE BASE
               ======================================== */

            .document-format-code {
                --code-background: #0f172a;
                --code-border: #1e293b;
                --code-text: #e2e8f0;

                --code-comment: #64748b;
                --code-keyword: #c084fc;
                --code-string: #86efac;
                --code-number: #fdba74;
                --code-function: #93c5fd;
                --code-class: #fbbf24;
                --code-property: #67e8f9;
                --code-operator: #f8fafc;
                --code-punctuation: #94a3b8;
                --code-boolean: #f472b6;
                --code-variable: #fca5a5;

                position: relative;
                margin: 16px 0;
                border: 1px solid var(--code-border);
                border-radius: 10px;
                background: var(--code-background);
                color: var(--code-text);
                overflow: hidden;
            }

            .document-format-code-language {
                padding: 7px 14px;
                border-bottom: 1px solid var(--code-border);
                color: #94a3b8;
                font-family: Consolas, "Courier New", monospace;
                font-size: 10px;
                font-weight: 700;
                letter-spacing: 0.08em;
                text-transform: uppercase;
            }

            .document-format-code pre {
                margin: 0;
                padding: 18px 20px;
                background: transparent;
                color: inherit;
                font-family: Consolas, "Courier New", monospace;
                font-size: 13px;
                line-height: 1.7;
                white-space: pre-wrap;
                overflow-x: auto;
                tab-size: 4;
            }

            .document-format-code code {
                font-family: inherit;
                font-size: inherit;
                line-height: inherit;
                color: var(--code-text);
            }

            /* ========================================
               PRISM TOKENS
               ======================================== */

            .document-format-code .token.comment,
            .document-format-code .token.prolog,
            .document-format-code .token.doctype,
            .document-format-code .token.cdata {
                color: var(--code-comment);
                font-style: italic;
            }

            .document-format-code .token.keyword {
                color: var(--code-keyword);
                font-weight: 700;
            }

            .document-format-code .token.string,
            .document-format-code .token.char,
            .document-format-code .token.attr-value {
                color: var(--code-string);
            }

            .document-format-code .token.number {
                color: var(--code-number);
            }

            .document-format-code .token.boolean,
            .document-format-code .token.null {
                color: var(--code-boolean);
            }

            .document-format-code .token.function {
                color: var(--code-function);
            }

            .document-format-code .token.class-name,
            .document-format-code .token.builtin {
                color: var(--code-class);
            }

            .document-format-code .token.property,
            .document-format-code .token.attr-name,
            .document-format-code .token.constant {
                color: var(--code-property);
            }

            .document-format-code .token.operator {
                color: var(--code-operator);
            }

            .document-format-code .token.punctuation {
                color: var(--code-punctuation);
            }

            .document-format-code .token.variable,
            .document-format-code .token.parameter {
                color: var(--code-variable);
            }

            .document-format-code .token.tag,
            .document-format-code .token.selector {
                color: var(--code-keyword);
            }

            /* ========================================
               LANGUAGE THEMES
               ======================================== */

            .code-language-java {
                --code-background: #18181b;
                --code-border: #3f3f46;
                --code-keyword: #c084fc;
                --code-string: #86efac;
                --code-number: #fb923c;
                --code-function: #60a5fa;
                --code-class: #facc15;
            }

            .code-language-javascript {
                --code-background: #18181b;
                --code-border: #52525b;
                --code-keyword: #f472b6;
                --code-string: #a3e635;
                --code-number: #fb923c;
                --code-function: #fde047;
            }

            .code-language-typescript {
                --code-background: #0f172a;
                --code-border: #1d4ed8;
                --code-keyword: #60a5fa;
                --code-string: #86efac;
                --code-function: #93c5fd;
            }

            .code-language-html {
                --code-background: #1c1917;
                --code-border: #c2410c;
                --code-keyword: #fb7185;
                --code-string: #86efac;
                --code-property: #fbbf24;
            }

            .code-language-css {
                --code-background: #172554;
                --code-border: #2563eb;
                --code-keyword: #c084fc;
                --code-string: #86efac;
                --code-property: #7dd3fc;
            }

            .code-language-json {
                --code-background: #052e16;
                --code-border: #15803d;
                --code-string: #fde68a;
                --code-number: #fb923c;
                --code-property: #67e8f9;
                --code-boolean: #f472b6;
            }

            .code-language-sql {
                --code-background: #172554;
                --code-border: #1d4ed8;
                --code-keyword: #7dd3fc;
                --code-string: #86efac;
                --code-number: #fdba74;
                --code-function: #fbbf24;
            }

            .code-language-xml {
                --code-background: #292524;
                --code-border: #ea580c;
                --code-keyword: #fb7185;
                --code-string: #86efac;
                --code-property: #fbbf24;
            }

            .code-language-python {
                --code-background: #172554;
                --code-border: #eab308;
                --code-keyword: #60a5fa;
                --code-string: #fde047;
                --code-number: #fb923c;
                --code-function: #93c5fd;
                --code-class: #facc15;
            }

            .code-language-bash {
                --code-background: #052e16;
                --code-border: #16a34a;
                --code-keyword: #86efac;
                --code-string: #fde68a;
                --code-number: #fb923c;
                --code-function: #67e8f9;
                --code-variable: #f0abfc;
            }

            .code-language-text {
                --code-background: #1e293b;
                --code-border: #475569;
            }

            /* ========================================
               MOBILE
               ======================================== */

            @media (max-width: 700px) {
                body {
                    padding: 22px 18px;
                }

                .document-format-title {
                    font-size: 25px;
                }

                .document-format-heading {
                    font-size: 19px;
                }

                .document-format-code pre {
                    padding: 14px;
                }

                .document-format-table th,
                .document-format-table td {
                    padding: 9px 11px;
                    font-size: 12px;
                }
            }
        `;
    }

    /* ========================================
       RENDER FORMATTED DOCUMENT
       ======================================== */

    function renderFormattedDocument(htmlContent, formats) {
        if (
            typeof htmlContent !== "string" ||
            !htmlContent.trim()
        ) {
            return "";
        }

        const parser = new DOMParser();

        const parsedDocument = parser.parseFromString(
            htmlContent,
            "text/html"
        );

        const formatMap = createFormatMap(formats);

        const output = document.createElement("div");

        output.className = "formatted-document";

        /*
         * IMPORTANT:
         * Tables do not consume paragraph indices.
         */
        let paragraphIndex = 0;

        /*
         * Read paragraphs and tables in document order.
         */
        Array.from(parsedDocument.body.children).forEach(
            sourceElement => {

                const tag =
                    sourceElement.tagName.toLowerCase();

                /* ========================================
                   WORD TABLE
                   ======================================== */

                if (tag === "table") {
                    output.appendChild(
                        createTableElement(sourceElement)
                    );

                    return;
                }

                /* ========================================
                   NORMAL PARAGRAPH
                   ======================================== */

                if (tag !== "p") {
                    return;
                }

                const format = getDocumentFormat(
                    formatMap,
                    paragraphIndex
                );

                const text =
                    getElementTextWithBreaks(sourceElement);

                output.appendChild(
                    createFormattedElement(text, format)
                );

                paragraphIndex++;
            }
        );

        /* ========================================
           CREATE IFRAME HTML
           ======================================== */

        return `
            <!DOCTYPE html>
            <html lang="en">

            <head>
                <meta charset="UTF-8">

                <meta name="viewport"
                      content="width=device-width, initial-scale=1.0">

                <style>
                    ${getFormattedDocumentStyles()}
                </style>
            </head>

            <body>
                ${output.innerHTML}
            </body>

            </html>
        `;
    }

    /* ========================================
       PUBLIC API
       ======================================== */

    window.renderFormattedDocument =
        renderFormattedDocument;

})();
