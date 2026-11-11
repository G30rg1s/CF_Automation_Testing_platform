
package gr.aueb.cf.parser.service;

import gr.aueb.cf.parser.dto.ParsedDocumentResultDto;
import gr.aueb.cf.parser.parsing.DocxParser;
import org.springframework.stereotype.Service;

@Service
public class DocumentParserService {

    private final DocxParser docxParser;

    public DocumentParserService(DocxParser docxParser) {
        this.docxParser = docxParser;
    }

    public ParsedDocumentResultDto parse(
            Long documentId,
            String filePath,
            String databaseTarget
    ) {

        String htmlContent = docxParser.parseToHtml(filePath);

        return new ParsedDocumentResultDto(
                documentId,
                htmlContent,
                databaseTarget
        );
    }
}
