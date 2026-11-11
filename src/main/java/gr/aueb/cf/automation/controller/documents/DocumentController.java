package gr.aueb.cf.automation.controller.documents;

import gr.aueb.cf.automation.dto.documents.DocumentCategoryAssignmentResponseDto;
import gr.aueb.cf.automation.dto.documents.DocumentDetailsDto;
import gr.aueb.cf.automation.dto.documents.DocumentTitleDto;
import gr.aueb.cf.automation.dto.documents.UpdateDocumentCategoryDto;
import gr.aueb.cf.automation.entity.documents.DocumentEntity;
import gr.aueb.cf.automation.entity.documents.DocumentStatus;
import gr.aueb.cf.automation.entity.documents.DocumentType;
import gr.aueb.cf.automation.service.documents.DocumentPdfService;
import gr.aueb.cf.automation.service.documents.DocumentService;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/cf_automation")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentPdfService documentPdfService;

    public DocumentController(
            DocumentService documentService,
            DocumentPdfService documentPdfService
    ) {
        this.documentService = documentService;
        this.documentPdfService = documentPdfService;
    }

    /* ========================================
       UPLOAD DOCUMENT
       ======================================== */

    @PostMapping("/documents/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public Long upload(
            @RequestParam String title,
            @RequestParam(required = false) MultipartFile file
    ) throws IOException {
        return documentService.upload(title, file);
    }

    /* ========================================
       GET DOCUMENT BY ID
       ======================================== */

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<DocumentDetailsDto> getById(
            @PathVariable Long documentId
    ) {
        DocumentDetailsDto document =
                documentService.getById(documentId);

        return ResponseEntity.ok(document);
    }

    /* ========================================
       GET DOCUMENTS BY CATEGORY
       ======================================== */

    @GetMapping("/categories/{categoryId}/documents")
    public ResponseEntity<Page<DocumentTitleDto>> getByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        return ResponseEntity.ok(
                documentService.getByCategory(categoryId, page, size)
        );
    }

    /* ========================================
       REUPLOAD DOCUMENT
       ======================================== */

    @PostMapping("/documents/{documentId}/reupload")
    public ResponseEntity<Void> reupload(
            @PathVariable Long documentId,
            @RequestParam String title,
            @RequestParam(required = false) MultipartFile file
    ) throws IOException {
        documentService.reupload(documentId, title, file);

        return ResponseEntity.ok().build();
    }

    /* ========================================
       FAILED DOCUMENTS
       ======================================== */

    @GetMapping("/documents/failed")
    public ResponseEntity<List<DocumentEntity>> getFailedDocuments() {
        return ResponseEntity.ok(
                documentService.getByStatus(DocumentStatus.FAILED)
        );
    }

    /* ========================================
       PENDING DOCUMENTS
       ======================================== */

    @GetMapping("/documents/pending")
    public ResponseEntity<List<DocumentEntity>> getPendingDocuments() {
        return ResponseEntity.ok(
                documentService.getByStatus(DocumentStatus.PENDING)
        );
    }

    /* ========================================
       READY DOCUMENTS
       ======================================== */

    @GetMapping("/documents/ready")
    public ResponseEntity<List<DocumentEntity>> getReadyDocuments() {
        return ResponseEntity.ok(
                documentService.getByStatus(DocumentStatus.READY)
        );
    }

    /* ========================================
       DELETE DOCUMENT
       ======================================== */

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long documentId
    ) {
        documentService.delete(documentId);

        return ResponseEntity.noContent().build();
    }

    /* ========================================
       CHANGE DOCUMENT CATEGORY
       ======================================== */

    @PatchMapping("/documents/{documentId}/category")
    public DocumentCategoryAssignmentResponseDto changeCategory(
            @PathVariable Long documentId,
            @RequestBody UpdateDocumentCategoryDto request
    ) {
        return documentService.changeCategory(
                documentId,
                request.getCategoryName()
        );
    }

    /* ========================================
       UPDATE DOCUMENT TYPE
       ======================================== */

    @PatchMapping("/documents/{documentId}/type")
    public ResponseEntity<Void> updateDocumentType(
            @PathVariable Long documentId,
            @RequestBody DocumentType documentType
    ) {
        documentService.updateDocumentType(documentId, documentType);

        return ResponseEntity.noContent().build();
    }

    /* ========================================
       EXPORT DOCUMENT AS PDF
       ======================================== */

    @GetMapping("/documents/{documentId}/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable Long documentId
    ) {
        DocumentDetailsDto document =
                documentService.getById(documentId);

        byte[] pdf =
                documentPdfService.createPdf(
                        documentId,
                        document.getHtmlContent()
                );

        String filename =
                document.getTitle() + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename, StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(pdf);
    }
}