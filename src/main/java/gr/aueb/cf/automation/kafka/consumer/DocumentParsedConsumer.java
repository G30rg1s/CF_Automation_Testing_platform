
package gr.aueb.cf.automation.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gr.aueb.cf.automation.database.DatabaseContextHolder;
import gr.aueb.cf.automation.dto.documents.ParsedDocumentResultDto;
import gr.aueb.cf.automation.queryrunner.DatabaseTarget;
import gr.aueb.cf.automation.service.documents.DocumentService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class DocumentParsedConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentParsedConsumer.class);

    private final ObjectMapper objectMapper;
    private final DocumentService documentService;

    public DocumentParsedConsumer(
            ObjectMapper objectMapper,
            DocumentService documentService
    ) {
        this.objectMapper = objectMapper;
        this.documentService = documentService;
    }

    @KafkaListener(
            topics = "document-parsed",
            groupId = "main-backend"
    )
    public void consume(String message) throws JsonProcessingException {

        long startedAt = System.nanoTime();

        log.info(
                "AUTOMATION KAFKA | document-parsed event received"
        );

        try {

            // ====================================
            // DESERIALIZE MESSAGE
            // ====================================

            ParsedDocumentResultDto result = objectMapper.readValue(
                    message,
                    ParsedDocumentResultDto.class
            );

            log.info(
                    "AUTOMATION KAFKA | Parsed document received | Document ID: {} | Database: {}",
                    result.getDocumentId(),
                    result.getDatabaseTarget()
            );

            // ====================================
            // VALIDATE DATABASE TARGET
            // ====================================

            if (result.getDatabaseTarget() == null ||
                    result.getDatabaseTarget().isBlank()) {

                throw new IllegalArgumentException(
                        "Database target is missing from parsed document result."
                );
            }

            DatabaseTarget databaseTarget;

            try {

                databaseTarget = DatabaseTarget.valueOf(
                        result.getDatabaseTarget()
                );

            } catch (IllegalArgumentException e) {

                throw new IllegalArgumentException(
                        "Invalid database target: " +
                                result.getDatabaseTarget(),
                        e
                );
            }

            // ====================================
            // UPDATE DOCUMENT
            // ====================================

            try {

                DatabaseContextHolder.set(databaseTarget);

                log.info(
                        "AUTOMATION KAFKA | Updating document | Document ID: {} | Database: {}",
                        result.getDocumentId(),
                        databaseTarget
                );

                documentService.markAsReady(
                        result.getDocumentId(),
                        result.getHtmlContent()
                );

                long durationMs =
                        TimeUnit.NANOSECONDS.toMillis(
                                System.nanoTime() - startedAt
                        );

                log.info(
                        "AUTOMATION KAFKA | Document marked as READY | Document ID: {} | Database: {} | Duration: {} ms",
                        result.getDocumentId(),
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
                    "AUTOMATION KAFKA | Failed to process document-parsed event | Duration: {} ms",
                    durationMs,
                    e
            );

            throw e;
        }
    }
}
