
package gr.aueb.cf.parser.parsing;

import gr.aueb.cf.parser.exception.exceptions.DocumentFileNotFoundException;
import gr.aueb.cf.parser.exception.exceptions.DocumentParsingException;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@Component
public class DocxParser {

    public String parseToHtml(String filePath) {

        File file = new File(filePath);

        if (!file.exists()) {
            throw new DocumentFileNotFoundException(
                    "Document file not found: " + filePath
            );
        }

        StringBuilder html = new StringBuilder();
        StringBuilder codeBlock = new StringBuilder();

        boolean insideCodeBlock = false;

        try (
                FileInputStream inputStream = new FileInputStream(file);
                XWPFDocument document = new XWPFDocument(inputStream)
        ) {

            /*
             * Read paragraphs AND tables
             * in their original Word order.
             */
            for (IBodyElement bodyElement : document.getBodyElements()) {

                /* ========================================
                   WORD TABLE
                   ======================================== */

                if (bodyElement instanceof XWPFTable table) {

                    /*
                     * Tables outside ### blocks become
                     * real HTML tables.
                     *
                     * Tables encountered inside an open
                     * block are preserved as text.
                     */
                    if (insideCodeBlock) {
                        appendTableAsBlockText(codeBlock, table);
                    } else {
                        appendHtmlTable(html, table);
                    }

                    continue;
                }

                /* ========================================
                   WORD PARAGRAPH
                   ======================================== */

                if (!(bodyElement instanceof XWPFParagraph paragraph)) {
                    continue;
                }

                String text = paragraph.getText();

                if (text == null) {
                    text = "";
                }

                String[] lines = text.split("\\R", -1);

                StringBuilder normalParagraph = new StringBuilder();

                for (String line : lines) {

                    /* ========================================
                       ### DELIMITER
                       ======================================== */

                    if (line.trim().equals("###")) {

                        if (insideCodeBlock) {

                            appendCodeBlock(html, codeBlock);

                            codeBlock.setLength(0);
                            insideCodeBlock = false;

                        } else {

                            appendNormalParagraph(
                                    html,
                                    normalParagraph
                            );

                            normalParagraph.setLength(0);
                            codeBlock.setLength(0);

                            insideCodeBlock = true;
                        }

                        continue;
                    }

                    /* ========================================
                       INSIDE ### BLOCK
                       ======================================== */

                    if (insideCodeBlock) {

                        if (codeBlock.length() > 0) {
                            codeBlock.append("\n");
                        }

                        codeBlock.append(line);

                    } else {

                        /* ========================================
                           NORMAL WORD PARAGRAPH
                           ======================================== */

                        if (normalParagraph.length() > 0) {
                            normalParagraph.append("\n");
                        }

                        normalParagraph.append(line);
                    }
                }

                if (!insideCodeBlock) {

                    appendNormalParagraph(
                            html,
                            normalParagraph
                    );

                } else if (normalParagraph.length() > 0) {

                    appendNormalParagraph(
                            html,
                            normalParagraph
                    );
                }

                /*
                 * Preserve separator between Word
                 * paragraphs inside ### blocks.
                 */
                if (insideCodeBlock && codeBlock.length() > 0) {
                    codeBlock.append("\n");
                }
            }

            /*
             * Preserve content if the closing
             * ### delimiter is missing.
             */
            if (insideCodeBlock) {
                appendCodeBlock(html, codeBlock);
            }

            return html.toString();

        } catch (IOException e) {

            throw new DocumentParsingException(
                    "Failed to parse DOCX file",
                    e
            );
        }
    }

    /* ========================================
       NORMAL PARAGRAPH
       ======================================== */

    private void appendNormalParagraph(
            StringBuilder html,
            StringBuilder text
    ) {

        if (text.isEmpty() || text.toString().isBlank()) {
            return;
        }

        html.append("<p>")
                .append(text)
                .append("</p>");
    }

    /* ========================================
       ### CODE / TEXT BLOCK
       ======================================== */

    private void appendCodeBlock(
            StringBuilder html,
            StringBuilder codeBlock
    ) {

        if (codeBlock.isEmpty()) {
            return;
        }

        String content = codeBlock.toString();

        if (content.endsWith("\n")) {
            content = content.substring(
                    0,
                    content.length() - 1
            );
        }

        html.append("<p>")
                .append(
                        escapeHtml(content)
                                .replace("\n", "<br>")
                )
                .append("</p>");
    }

    /* ========================================
       WORD TABLE TO HTML TABLE
       ======================================== */

    private void appendHtmlTable(
            StringBuilder html,
            XWPFTable table
    ) {

        List<XWPFTableRow> rows = table.getRows();

        if (rows == null || rows.isEmpty()) {
            return;
        }

        html.append("<table>");

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {

            XWPFTableRow row = rows.get(rowIndex);

            html.append("<tr>");

            List<XWPFTableCell> cells = row.getTableCells();

            for (XWPFTableCell cell : cells) {

                /*
                 * Convention:
                 * First row = table header.
                 * Remaining rows = normal data.
                 */
                String tag = rowIndex == 0 ? "th" : "td";

                html.append("<")
                        .append(tag)
                        .append(">");

                html.append(
                        getTableCellHtml(cell)
                );

                html.append("</")
                        .append(tag)
                        .append(">");
            }

            html.append("</tr>");
        }

        html.append("</table>");
    }

    /* ========================================
       TABLE CELL TEXT
       ======================================== */

    private String getTableCellHtml(XWPFTableCell cell) {

        StringBuilder content = new StringBuilder();

        List<XWPFParagraph> paragraphs = cell.getParagraphs();

        for (XWPFParagraph paragraph : paragraphs) {

            String text = paragraph.getText();

            if (text == null) {
                text = "";
            }

            if (content.length() > 0) {
                content.append("\n");
            }

            content.append(text);
        }

        return escapeHtml(content.toString())
                .replace("\n", "<br>");
    }

    /* ========================================
       TABLE INSIDE ### BLOCK
       ======================================== */

    private void appendTableAsBlockText(
            StringBuilder codeBlock,
            XWPFTable table
    ) {

        for (XWPFTableRow row : table.getRows()) {

            if (codeBlock.length() > 0 &&
                    codeBlock.charAt(codeBlock.length() - 1) != '\n') {
                codeBlock.append("\n");
            }

            List<XWPFTableCell> cells = row.getTableCells();

            for (int index = 0; index < cells.size(); index++) {

                if (index > 0) {
                    codeBlock.append(" | ");
                }

                codeBlock.append(
                        getTableCellPlainText(cells.get(index))
                );
            }

            codeBlock.append("\n");
        }
    }

    /* ========================================
       TABLE CELL PLAIN TEXT
       ======================================== */

    private String getTableCellPlainText(XWPFTableCell cell) {

        StringBuilder content = new StringBuilder();

        for (XWPFParagraph paragraph : cell.getParagraphs()) {

            if (content.length() > 0) {
                content.append("\n");
            }

            String text = paragraph.getText();

            content.append(text == null ? "" : text);
        }

        return content.toString();
    }

    /* ========================================
       HTML ESCAPING
       ======================================== */

    private String escapeHtml(String text) {

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
