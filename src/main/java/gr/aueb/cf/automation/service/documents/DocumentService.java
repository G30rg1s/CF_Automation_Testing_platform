
package gr.aueb.cf.automation.service.documents;

import gr.aueb.cf.automation.database.DatabaseContextHolder;
import gr.aueb.cf.automation.dto.documents.DocumentCategoryAssignmentResponseDto;
import gr.aueb.cf.automation.dto.documents.DocumentDetailsDto;
import gr.aueb.cf.automation.dto.documents.DocumentParseRequestDto;
import gr.aueb.cf.automation.dto.documents.DocumentTitleDto;
import gr.aueb.cf.automation.entity.documents.CategoryEntity;
import gr.aueb.cf.automation.entity.documents.DocumentEntity;
import gr.aueb.cf.automation.entity.documents.DocumentStatus;
import gr.aueb.cf.automation.entity.documents.DocumentType;
import gr.aueb.cf.automation.exception.exceptions.CategoryNotFoundException;
import gr.aueb.cf.automation.exception.exceptions.DocumentAlreadyExistsException;
import gr.aueb.cf.automation.exception.exceptions.DocumentPendingException;
import gr.aueb.cf.automation.kafka.DocumentUploadedProducer;
import gr.aueb.cf.automation.queryrunner.DatabaseTarget;
import gr.aueb.cf.automation.repository.documents.CategoryRepository;
import gr.aueb.cf.automation.repository.documents.DocumentRepository;
import gr.aueb.cf.automation.service.format.DocumentFormatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private static final String FILE_DELETED_MESSAGE =
            "File has been deleted after parser response";

    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;
    private final DocumentUploadedProducer documentUploadedProducer;
    private final DocumentFormatService documentFormatService;

    public DocumentService(
            DocumentRepository documentRepository,
            CategoryRepository categoryRepository,
            DocumentUploadedProducer documentUploadedProducer,
            DocumentFormatService documentFormatService
    ) {
        this.documentRepository = documentRepository;
        this.categoryRepository = categoryRepository;
        this.documentUploadedProducer = documentUploadedProducer;
        this.documentFormatService = documentFormatService;
    }

    /* ========================================
       UPLOAD DOCUMENT
       ======================================== */

    public Long upload(String title, MultipartFile file) throws IOException {

        /*
         * Capture the database selected for this request.
         * The Kafka event must preserve this target.
         */
        DatabaseTarget databaseTarget = requireDatabaseTarget();

        /* ========================================
           VALIDATE TITLE
           ======================================== */

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }

        title = title.trim();

        /* ========================================
           CHECK DOCUMENT TITLE
           ======================================== */

        documentRepository.findByTitle(title).ifPresent(document -> {
            throw new DocumentAlreadyExistsException(
                    document.getId(),
                    document.getTitle()
            );
        });

        /* ========================================
           FIND DEFAULT CATEGORY
           ======================================== */

        CategoryEntity defaultCategory =
                categoryRepository.findByNameIgnoreCaseAndAccent("Διάφορα")
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Default category 'Διάφορα' was not found."
                                )
                        );

        /* ========================================
           CHECK FILE
           ======================================== */

        boolean hasFile = file != null && !file.isEmpty();

        /* ========================================
           CREATE DOCUMENT
           ======================================== */

        DocumentEntity document = new DocumentEntity();

        document.setTitle(title);
        document.setHtmlContent(null);
        document.setStatus(
                hasFile
                        ? DocumentStatus.PENDING
                        : DocumentStatus.AWAITING_FILE
        );
        document.setErrorMessage(null);
        document.setCategory(defaultCategory);

        /* ========================================
           FILE IS OPTIONAL
           ======================================== */

        if (!hasFile) {
            document.setFilePath(null);
            document = documentRepository.save(document);

            return document.getId();
        }

        /* ========================================
           UPLOAD DIRECTORY
           ======================================== */

        Path uploadDirectory = Paths.get("uploads")
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(uploadDirectory);

        /* ========================================
           ORIGINAL FILE NAME
           ======================================== */

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "document.docx";
        }

        originalFilename = Paths.get(originalFilename)
                .getFileName()
                .toString();

        /* ========================================
           STORED FILE NAME
           ======================================== */

        String storedFilename =
                UUID.randomUUID() + "-" + originalFilename;

        Path targetPath = uploadDirectory.resolve(storedFilename);

        /* ========================================
           SAVE FILE
           ======================================== */

        Files.copy(
                file.getInputStream(),
                targetPath,
                StandardCopyOption.REPLACE_EXISTING
        );

        /* ========================================
           SAVE DOCUMENT
           ======================================== */

        document.setFilePath(targetPath.toString());
        document = documentRepository.save(document);

        /* ========================================
           SEND TO PARSER
           ======================================== */

        DocumentParseRequestDto request =
                new DocumentParseRequestDto(
                        document.getId(),
                        document.getFilePath(),
                        databaseTarget.name()
                );

        documentUploadedProducer.send(request);

        return document.getId();
    }

    /* ========================================
       GET DOCUMENT BY ID
       ======================================== */

    @Transactional(readOnly = true)
    public DocumentDetailsDto getById(Long documentId) {

        DocumentEntity document =
                documentRepository.findById(documentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Document not found: " + documentId
                                )
                        );

        CategoryEntity category = document.getCategory();

        return new DocumentDetailsDto(
                document.getId(),
                document.getTitle(),
                document.getHtmlContent(),
                document.getStatus(),
                document.getErrorMessage(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                document.getDocumentType()
        );
    }

    /* ========================================
       GET DOCUMENTS BY CATEGORY
       ======================================== */

    @Transactional(readOnly = true)
    public Page<DocumentTitleDto> getByCategory(
            Long categoryId,
            int page,
            int size
    ) {

        if (!categoryRepository.existsById(categoryId)) {
            throw new CategoryNotFoundException(categoryId);
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<DocumentEntity> documents =
                documentRepository.findByCategory_IdOrderByTitleAsc(
                        categoryId,
                        pageable
                );

        return documents.map(document ->
                new DocumentTitleDto(
                        document.getId(),
                        document.getTitle()
                )
        );
    }

    /* ========================================
       REUPLOAD DOCUMENT
       ======================================== */

    public void reupload(
            Long documentId,
            String title,
            MultipartFile file
    ) throws IOException {

        /*
         * Capture the database of the current request.
         */
        DatabaseTarget databaseTarget = requireDatabaseTarget();

        /* ========================================
           FIND DOCUMENT
           ======================================== */

        DocumentEntity document =
                documentRepository.findById(documentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Document not found: " + documentId
                                )
                        );

        /* ========================================
           BLOCK FILE REUPLOAD WHILE PENDING
           ======================================== */

        if (document.getStatus() == DocumentStatus.PENDING
                && file != null
                && !file.isEmpty()) {

            throw new DocumentPendingException(documentId);
        }

        /* ========================================
           VALIDATE TITLE
           ======================================== */

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }

        title = title.trim();

        /* ========================================
           CHECK DOCUMENT TITLE
           ======================================== */

        documentRepository.findByTitle(title).ifPresent(existingDocument -> {
            if (!existingDocument.getId().equals(documentId)) {
                throw new DocumentAlreadyExistsException(
                        existingDocument.getId(),
                        existingDocument.getTitle()
                );
            }
        });

        /* ========================================
           UPDATE TITLE
           ======================================== */

        document.setTitle(title);

        /* ========================================
           FILE IS OPTIONAL
           ======================================== */

        if (file == null || file.isEmpty()) {
            documentRepository.save(document);
            return;
        }

        /* ========================================
           DELETE PREVIOUS FILE
           ======================================== */

        deleteUploadedFile(document.getFilePath());

        /* ========================================
           UPLOAD DIRECTORY
           ======================================== */

        Path uploadDirectory = Paths.get("uploads")
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(uploadDirectory);

        /* ========================================
           ORIGINAL FILE NAME
           ======================================== */

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "document.docx";
        }

        originalFilename = Paths.get(originalFilename)
                .getFileName()
                .toString();

        /* ========================================
           STORED FILE NAME
           ======================================== */

        String storedFilename =
                UUID.randomUUID() + "-" + originalFilename;

        Path targetPath = uploadDirectory.resolve(storedFilename);

        /* ========================================
           SAVE FILE
           ======================================== */

        Files.copy(
                file.getInputStream(),
                targetPath,
                StandardCopyOption.REPLACE_EXISTING
        );

        /* ========================================
           RESET DOCUMENT
           ======================================== */

        document.setFilePath(targetPath.toString());
        document.setHtmlContent(null);
        document.setErrorMessage(null);
        document.setStatus(DocumentStatus.PENDING);

        /* ========================================
           SAVE DOCUMENT
           ======================================== */

        documentRepository.save(document);

        /* ========================================
           SEND TO PARSER
           ======================================== */

        DocumentParseRequestDto request =
                new DocumentParseRequestDto(
                        documentId,
                        document.getFilePath(),
                        databaseTarget.name()
                );

        documentUploadedProducer.send(request);
    }

    /* ========================================
       MARK AS READY
       ======================================== */

    @Transactional
    public void markAsReady(
            Long documentId,
            String htmlContent
    ) {

        DocumentEntity document =
                documentRepository.findById(documentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Document not found: " + documentId
                                )
                        );

        String uploadedFilePath = document.getFilePath();

        document.setHtmlContent(htmlContent);
        document.setStatus(DocumentStatus.READY);
        document.setErrorMessage(null);

        /* ========================================
           INITIALIZE OR SYNC DOCUMENT FORMATS
           ======================================== */

        if (documentFormatService.hasFormats(documentId)) {
            documentFormatService.syncFormats(
                    documentId,
                    htmlContent
            );
        } else {
            documentFormatService.initializeFormats(
                    documentId,
                    htmlContent
            );
        }

        /* ========================================
           DELETE UPLOADED FILE
           ======================================== */

        deleteUploadedFile(uploadedFilePath);

        document.setFilePath(FILE_DELETED_MESSAGE);
    }

    /* ========================================
       MARK AS FAILED
       ======================================== */

    @Transactional
    public void markAsFailed(
            Long documentId,
            String errorMessage
    ) {

        if (documentId == null) {
            return;
        }

        DocumentEntity document =
                documentRepository.findById(documentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Document not found: " + documentId
                                )
                        );

        /* ========================================
           DELETE UPLOADED FILE
           ======================================== */

        deleteUploadedFile(document.getFilePath());

        document.setHtmlContent(null);
        document.setStatus(DocumentStatus.FAILED);
        document.setErrorMessage(errorMessage);
        document.setFilePath(FILE_DELETED_MESSAGE);
    }

    /* ========================================
       DELETE DOCUMENT
       ======================================== */

    @Transactional
    public void delete(Long documentId) {

        DocumentEntity document =
                documentRepository.findById(documentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Document not found: " + documentId
                                )
                        );

        /* ========================================
           DELETE DOCUMENT FORMATS
           ======================================== */

        documentFormatService.deleteByDocument(documentId);

        /* ========================================
           DELETE UPLOADED FILE
           ======================================== */

        deleteUploadedFile(document.getFilePath());

        documentRepository.delete(document);
    }

    /* ========================================
       GET DOCUMENTS BY STATUS
       ======================================== */

    public List<DocumentEntity> getByStatus(DocumentStatus status) {
        return documentRepository.findByStatus(status);
    }

    /* ========================================
       DATABASE TARGET HELPER
       ======================================== */

    private DatabaseTarget requireDatabaseTarget() {

        DatabaseTarget databaseTarget =
                DatabaseContextHolder.get();

        if (databaseTarget == null) {
            throw new IllegalStateException(
                    "No database has been selected for document operation."
            );
        }

        return databaseTarget;
    }

    /* ========================================
       DELETE UPLOADED FILE
       ======================================== */

    private void deleteUploadedFile(String filePath) {

        if (filePath == null
                || filePath.isBlank()
                || filePath.equals(FILE_DELETED_MESSAGE)) {
            return;
        }

        try {
            Files.deleteIfExists(Paths.get(filePath));

        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not delete uploaded file: " + filePath,
                    e
            );
        }
    }

    /* ========================================
       CHANGE DOCUMENT CATEGORY
       ======================================== */

    @Transactional
    public DocumentCategoryAssignmentResponseDto changeCategory(
            Long documentId,
            String categoryName
    ) {

        /* ========================================
           FIND DOCUMENT
           ======================================== */

        DocumentEntity document =
                documentRepository.findById(documentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Document not found: " + documentId
                                )
                        );

        /* ========================================
           VALIDATE CATEGORY NAME
           ======================================== */

        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException(
                    "Category name is required"
            );
        }

        categoryName = categoryName.trim();

        /* ========================================
           FIND EXISTING CATEGORY
           ======================================== */

        CategoryEntity category =
                categoryRepository.findByNameIgnoreCaseAndAccent(
                        categoryName
                ).orElse(null);

        String categoryResult;

        /* ========================================
           CREATE CATEGORY IF NOT FOUND
           ======================================== */

        if (category == null) {

            category = new CategoryEntity();
            category.setName(categoryName);

            category = categoryRepository.save(category);

            categoryResult = "CREATED";

        } else {

            categoryResult = "EXISTING";
        }

        /* ========================================
           ASSIGN CATEGORY TO DOCUMENT
           ======================================== */

        document.setCategory(category);

        document = documentRepository.save(document);

        /* ========================================
           RESPONSE
           ======================================== */

        return new DocumentCategoryAssignmentResponseDto(
                categoryResult,
                category.getId(),
                category.getName(),
                document.getId(),
                document.getTitle()
        );
    }

    /* ========================================
       UPDATE DOCUMENT TYPE
       ======================================== */

    @Transactional
    public void updateDocumentType(
            Long documentId,
            DocumentType documentType
    ) {

        if (documentType == null) {
            throw new IllegalArgumentException(
                    "Document type is required"
            );
        }

        DocumentEntity document =
                documentRepository.findById(documentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Document not found: " + documentId
                                )
                        );

        document.setDocumentType(documentType);

        documentRepository.save(document);
    }
}
