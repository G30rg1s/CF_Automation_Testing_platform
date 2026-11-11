package gr.aueb.cf.automation.service.format;

import gr.aueb.cf.automation.dto.format.DocumentFormatDto;
import gr.aueb.cf.automation.dto.format.UpdateDocumentFormatDto;
import gr.aueb.cf.automation.entity.documents.DocumentEntity;
import gr.aueb.cf.automation.entity.format.DocumentFormatEntity;
import gr.aueb.cf.automation.entity.format.DocumentFormatType;
import gr.aueb.cf.automation.repository.documents.DocumentRepository;
import gr.aueb.cf.automation.repository.format.DocumentFormatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DocumentFormatService {

    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("<p(?:\\s[^>]*)?>", Pattern.CASE_INSENSITIVE);

    private final DocumentFormatRepository documentFormatRepository;
    private final DocumentRepository documentRepository;


    /* ========================================
       INITIALIZE DOCUMENT FORMATS
       ======================================== */

    @Transactional
    public void initializeFormats(Long documentId, String htmlContent) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        documentFormatRepository.deleteByDocument_Id(documentId);

        if (htmlContent == null || htmlContent.isBlank()) {
            return;
        }

        int paragraphCount = countParagraphs(htmlContent);

        List<DocumentFormatEntity> formats = new ArrayList<>();

        for (int paragraphIndex = 0; paragraphIndex < paragraphCount; paragraphIndex++) {
            formats.add(new DocumentFormatEntity(
                    null,
                    document,
                    paragraphIndex,
                    DocumentFormatType.PARAGRAPH,
                    null
            ));
        }

        if (!formats.isEmpty()) {
            documentFormatRepository.saveAll(formats);
        }
    }


    /* ========================================
       SYNC DOCUMENT FORMATS
       ======================================== */

    @Transactional
    public void syncFormats(Long documentId, String htmlContent) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        int existingParagraphCount = Math.toIntExact(documentFormatRepository.countByDocument_Id(documentId));
        int newParagraphCount = countParagraphs(htmlContent);


        /* ========================================
           SAME PARAGRAPH COUNT
           ======================================== */

        if (newParagraphCount == existingParagraphCount) {
            return;
        }


        /* ========================================
           NEW DOCUMENT HAS FEWER PARAGRAPHS
           ======================================== */

        if (newParagraphCount < existingParagraphCount) {
            documentFormatRepository.deleteByDocument_IdAndParagraphIndexGreaterThanEqual(documentId, newParagraphCount);
            return;
        }


        /* ========================================
           NEW DOCUMENT HAS MORE PARAGRAPHS
           ======================================== */

        List<DocumentFormatEntity> newFormats = new ArrayList<>();

        for (int paragraphIndex = existingParagraphCount; paragraphIndex < newParagraphCount; paragraphIndex++) {
            newFormats.add(new DocumentFormatEntity(
                    null,
                    document,
                    paragraphIndex,
                    DocumentFormatType.PARAGRAPH,
                    null
            ));
        }

        documentFormatRepository.saveAll(newFormats);
    }


    /* ========================================
       GET DOCUMENT FORMATS
       ======================================== */

    @Transactional(readOnly = true)
    public List<DocumentFormatDto> getByDocument(Long documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new RuntimeException("Document not found: " + documentId);
        }

        return documentFormatRepository.findByDocument_IdOrderByParagraphIndexAsc(documentId)
                .stream()
                .map(format -> new DocumentFormatDto(
                        format.getId(),
                        format.getParagraphIndex(),
                        format.getFormatType(),
                        format.getCodeLanguage()
                ))
                .toList();
    }


    /* ========================================
       UPDATE DOCUMENT FORMATS
       ======================================== */

    @Transactional
    public void updateFormats(Long documentId, List<UpdateDocumentFormatDto> updates) {
        if (!documentRepository.existsById(documentId)) {
            throw new RuntimeException("Document not found: " + documentId);
        }

        for (UpdateDocumentFormatDto update : updates) {
            if (update.getFormatType() == null) {
                throw new IllegalArgumentException("Format type is required.");
            }

            DocumentFormatEntity format = documentFormatRepository
                    .findByDocument_IdAndParagraphIndex(documentId, update.getParagraphIndex())
                    .orElseThrow(() -> new RuntimeException(
                            "Document format not found for paragraph: " + update.getParagraphIndex()
                    ));

            format.setFormatType(update.getFormatType());

            if (update.getFormatType() == DocumentFormatType.CODE) {
                format.setCodeLanguage(update.getCodeLanguage());
            } else {
                format.setCodeLanguage(null);
            }
        }
    }


    /* ========================================
       CHECK DOCUMENT FORMATS
       ======================================== */

    @Transactional(readOnly = true)
    public boolean hasFormats(Long documentId) {
        return documentFormatRepository.existsByDocument_Id(documentId);
    }


    /* ========================================
       DELETE DOCUMENT FORMATS
       ======================================== */

    @Transactional
    public void deleteByDocument(Long documentId) {
        documentFormatRepository.deleteByDocument_Id(documentId);
    }


    /* ========================================
       COUNT PARAGRAPHS
       ======================================== */

    private int countParagraphs(String htmlContent) {
        if (htmlContent == null || htmlContent.isBlank()) {
            return 0;
        }

        Matcher matcher = PARAGRAPH_PATTERN.matcher(htmlContent);
        int count = 0;

        while (matcher.find()) {
            count++;
        }

        return count;
    }
}