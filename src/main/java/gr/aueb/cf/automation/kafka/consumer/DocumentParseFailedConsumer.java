
package gr.aueb.cf.automation.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gr.aueb.cf.automation.database.DatabaseContextHolder;
import gr.aueb.cf.automation.dto.documents.DocumentParseFailedDto;
import gr.aueb.cf.automation.queryrunner.DatabaseTarget;
import gr.aueb.cf.automation.service.documents.DocumentService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class DocumentParseFailedConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentParseFailedConsumer.class);

    private final ObjectMapper objectMapper;
    private final DocumentService documentService;

    public DocumentParseFailedConsumer(
            ObjectMapper objectMapper,
            DocumentService documentService
    ) {
        this.objectMapper = objectMapper;
        this.documentService = documentService;
    }

    @KafkaListener(
            topics = "document-parse-failed",
            groupId = "main-backend"
    )
    public void consume(String message) throws JsonProcessingException {

        long startedAt = System.nanoTime();

        log.info(
                "AUTOMATION KAFKA | document-parse-failed event received"
        );

        try {

            // ====================================
            // DESERIALIZE MESSAGE
            // ====================================

            DocumentParseFailedDto failedDto = objectMapper.readValue(
                    message,
                    DocumentParseFailedDto.class
            );

            log.warn(
                    "AUTOMATION KAFKA | Parse failure received | Document ID: {} | Database: {}",
                    failedDto.getDocumentId(),
                    failedDto.getDatabaseTarget()
            );

            // ====================================
            // VALIDATE DOCUMENT ID
            // ====================================

            // Messages without a document ID cannot update a document.
            if (failedDto.getDocumentId() == null) {

                log.warn(
                        "AUTOMATION KAFKA | Skipping failure event | Reason: Missing document ID"
                );

                return;
            }

            // ====================================
            // VALIDATE DATABASE TARGET
            // ====================================

            // Never select PRODUCTION automatically.
            if (failedDto.getDatabaseTarget() == null ||
                    failedDto.getDatabaseTarget().isBlank()) {

                throw new IllegalArgumentException(
                        "Database target is missing from failed document result."
                );
            }

            DatabaseTarget databaseTarget;

            try {

                databaseTarget = DatabaseTarget.valueOf(
                        failedDto.getDatabaseTarget()
                );

            } catch (IllegalArgumentException e) {

                throw new IllegalArgumentException(
                        "Invalid database target: " +
                                failedDto.getDatabaseTarget(),
                        e
                );
            }

            // ====================================
            // UPDATE DOCUMENT STATUS
            // ====================================

            try {

                DatabaseContextHolder.set(databaseTarget);

                log.info(
                        "AUTOMATION KAFKA | Marking document as FAILED | Document ID: {} | Database: {}",
                        failedDto.getDocumentId(),
                        databaseTarget
                );

                documentService.markAsFailed(
                        failedDto.getDocumentId(),
                        failedDto.getErrorMessage()
                );

                long durationMs =
                        TimeUnit.NANOSECONDS.toMillis(
                                System.nanoTime() - startedAt
                        );

                log.info(
                        "AUTOMATION KAFKA | Document marked as FAILED | Document ID: {} | Database: {} | Duration: {} ms",
                        failedDto.getDocumentId(),
                        databaseTarget,
                        durationMs
                );

            } finally {

                DatabaseContextHolder.clear();

            }

        } catch (JsonProcessingException | RuntimeException e) {

            long durationMs =
                    TimeUnit.NANOSECONDS.toMillis(
                            System.nanoTime() - startedAt
                    );

            log.error(
                    "AUTOMATION KAFKA | Failed to process document-parse-failed event | Duration: {} ms",
                    durationMs,
                    e
            );

            throw e;
        }
    }
}
