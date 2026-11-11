package gr.aueb.cf.automation.service.documents;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import gr.aueb.cf.automation.dto.format.DocumentFormatDto;
import gr.aueb.cf.automation.entity.format.DocumentFormatType;
import gr.aueb.cf.automation.service.format.DocumentFormatService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DocumentPdfService {

    private static final String PDF_STYLES = """
            @page {
                size: A4;
                margin: 18mm;
            }

            * {
                box-sizing: border-box;
            }

            html, body {
                margin: 0;
                padding: 0;
                background: #ffffff;
                color: #1f2937;
                font-family: "Noto Sans", sans-serif;
                font-size: 11pt;
                line-height: 1.6;
                -webkit-print-color-adjust: exact;
                print-color-adjust: exact;
            }

            .document-format-title {
                margin: 0 0 18px;
                color: #0f172a;
                font-size: 24pt;
                font-weight: bold;
                line-height: 1.25;
            }

            .document-format-heading {
                margin: 22px 0 10px;
                color: #111827;
                font-size: 17pt;
                font-weight: bold;
                line-height: 1.35;
            }

            .document-format-paragraph {
                margin: 0 0 12px;
                white-space: pre-wrap;
            }

            .document-format-quote {
                margin: 16px 0;
                padding: 10px 14px;
                border-left: 4px solid #8b5cf6;
                background: #f8fafc;
                color: #475569;
                font-style: italic;
                white-space: pre-wrap;
            }

            .document-format-link-wrapper {
                margin: 0 0 12px;
            }

            .document-format-link {
                color: #2563eb;
                text-decoration: underline;
            }

            .document-format-table-wrapper {
                width: 100%;
                margin: 16px 0 22px;
            }

            .document-format-table {
                width: 100%;
                border-collapse: collapse;
                color: #334155;
                font-size: 10pt;
            }

            .document-format-table th,
            .document-format-table td {
                padding: 8px 10px;
                border: 1px solid #cbd5e1;
                vertical-align: top;
                text-align: left;
                white-space: pre-wrap;
            }

            .document-format-table th {
                background: #e2e8f0;
                color: #0f172a;
                font-weight: bold;
            }

            .document-format-code {
                margin: 14px 0;
                border: 1px solid #334155;
                background: #0f172a;
                color: #e2e8f0;
                page-break-inside: avoid;
            }

            .document-format-code-language {
                padding: 6px 10px;
                border-bottom: 1px solid #334155;
                color: #cbd5e1;
                font-size: 8pt;
                font-weight: bold;
                text-transform: uppercase;
            }

            .document-format-code pre {
                margin: 0;
                padding: 12px;
                color: #e2e8f0;
                font-family: "Noto Sans", monospace;
                font-size: 9pt;
                line-height: 1.45;
                white-space: pre-wrap;
            }

            h1, h2, h3, h4, h5, h6 {
                page-break-after: avoid;
            }

            img {
                max-width: 100%;
            }
            """;

    private final DocumentFormatService documentFormatService;

    public DocumentPdfService(
            DocumentFormatService documentFormatService
    ) {
        this.documentFormatService = documentFormatService;
    }

    public byte[] createPdf(Long documentId, String htmlContent) {
        if (htmlContent == null || htmlContent.isBlank()) {
            throw new IllegalArgumentException(
                    "Document has no content to export"
            );
        }

        try {
            List<DocumentFormatDto> formats =
                    documentFormatService.getByDocument(documentId);

            Map<Integer, DocumentFormatDto> formatsByParagraph =
                    new HashMap<>();

            for (DocumentFormatDto format : formats) {
                formatsByParagraph.put(
                        format.getParagraphIndex(),
                        format
                );
            }

            Document sourceDocument = Jsoup.parse(htmlContent);
            Document pdfDocument = Document.createShell("");

            pdfDocument.outputSettings()
                    .syntax(Document.OutputSettings.Syntax.xml)
                    .escapeMode(Entities.EscapeMode.xhtml)
                    .charset(StandardCharsets.UTF_8)
                    .prettyPrint(true);

            pdfDocument.head()
                    .appendElement("meta")
                    .attr("charset", "UTF-8");

            pdfDocument.head()
                    .appendElement("style")
                    .text(PDF_STYLES);

            int paragraphIndex = 0;

            for (Element sourceElement :
                    sourceDocument.body().children()) {

                String tagName =
                        sourceElement.tagName().toLowerCase(Locale.ROOT);

                if ("table".equals(tagName)) {
                    pdfDocument.body().appendChild(
                            createTableElement(sourceElement)
                    );
                    continue;
                }

                if (!"p".equals(tagName)) {
                    continue;
                }

                String text = getTextWithBreaks(sourceElement);

                DocumentFormatDto format =
                        formatsByParagraph.get(paragraphIndex);

                pdfDocument.body().appendChild(
                        createFormattedElement(text, format)
                );

                paragraphIndex++;
            }

            String xhtmlContent =
                    pdfDocument.outerHtml();

            try (ByteArrayOutputStream output =
                         new ByteArrayOutputStream()) {

                new PdfRendererBuilder()
                        .useFont(
                                () -> DocumentPdfService.class
                                        .getResourceAsStream(
                                                "/fonts/NotoSans.ttf"
                                        ),
                                "Noto Sans"
                        )
                        .withHtmlContent(xhtmlContent, null)
                        .toStream(output)
                        .run();

                return output.toByteArray();
            }
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not create PDF",
                    exception
            );
        }
    }

    private Element createFormattedElement(
            String text,
            DocumentFormatDto format
    ) {
        DocumentFormatType formatType =
                format == null || format.getFormatType() == null
                        ? DocumentFormatType.PARAGRAPH
                        : format.getFormatType();

        return switch (formatType) {
            case TITLE ->
                    new Element("h1")
                            .addClass("document-format-title")
                            .text(text);

            case HEADING ->
                    new Element("h2")
                            .addClass("document-format-heading")
                            .text(text);

            case QUOTE ->
                    new Element("blockquote")
                            .addClass("document-format-quote")
                            .text(text);

            case LINK ->
                    createLinkElement(text);

            case CODE ->
                    createCodeElement(
                            text,
                            format.getCodeLanguage() == null
                                    ? "TEXT"
                                    : format.getCodeLanguage()
                                    .name()
                    );

            case ARRAY, PARAGRAPH ->
                    new Element("p")
                            .addClass("document-format-paragraph")
                            .text(text);
        };
    }

    private Element createLinkElement(String text) {
        Element wrapper =
                new Element("p")
                        .addClass("document-format-link-wrapper");

        String url = normalizeLink(text);

        if (url == null) {
            return wrapper.text(text);
        }

        wrapper.appendElement("a")
                .addClass("document-format-link")
                .attr("href", url)
                .text(text);

        return wrapper;
    }

    private Element createCodeElement(
            String text,
            String language
    ) {
        String normalizedLanguage =
                language.toUpperCase(Locale.ROOT);

        Element wrapper =
                new Element("div")
                        .addClass("document-format-code")
                        .addClass(
                                "code-language-" +
                                        normalizedLanguage.toLowerCase(
                                                Locale.ROOT
                                        )
                        );

        wrapper.appendElement("div")
                .addClass("document-format-code-language")
                .text(normalizedLanguage);

        wrapper.appendElement("pre")
                .appendElement("code")
                .text(text);

        return wrapper;
    }

    private Element createTableElement(Element sourceTable) {
        Element wrapper =
                new Element("div")
                        .addClass("document-format-table-wrapper");

        Element table =
                wrapper.appendElement("table")
                        .addClass("document-format-table");

        for (Element sourceRow :
                sourceTable.getElementsByTag("tr")) {

            Element row =
                    table.appendElement("tr");

            for (Element sourceCell : sourceRow.children()) {
                String cellTag =
                        sourceCell.tagName().equalsIgnoreCase("th")
                                ? "th"
                                : "td";

                Element cell =
                        row.appendElement(cellTag)
                                .text(getTextWithBreaks(sourceCell));

                if (sourceCell.hasAttr("colspan")) {
                    cell.attr(
                            "colspan",
                            sourceCell.attr("colspan")
                    );
                }

                if (sourceCell.hasAttr("rowspan")) {
                    cell.attr(
                            "rowspan",
                            sourceCell.attr("rowspan")
                    );
                }
            }
        }

        return wrapper;
    }

    private String getTextWithBreaks(Element sourceElement) {
        Element clone = sourceElement.clone();

        for (Element lineBreak : clone.getElementsByTag("br")) {
            lineBreak.before(new TextNode("\n"));
            lineBreak.remove();
        }

        return clone.wholeText();
    }

    private String normalizeLink(String value) {
        String url = value == null ? "" : value.trim();

        if (url.isEmpty()) {
            return null;
        }

        if (url.matches("(?i)^https?://.*")) {
            return url;
        }

        if (url.matches("^[a-zA-Z][a-zA-Z\\d+.-]*:.*")) {
            return null;
        }

        return "https://" + url;
    }
}