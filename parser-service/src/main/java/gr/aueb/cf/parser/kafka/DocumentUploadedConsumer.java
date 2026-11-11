
package gr.aueb.cf.parser.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gr.aueb.cf.parser.dto.DocumentParseFailedDto;
import gr.aueb.cf.parser.dto.DocumentParseRequestDto;
import gr.aueb.cf.parser.dto.ParsedDocumentResultDto;

import gr.aueb.cf.parser.exception.exceptions.DocumentFileNotFoundException;
import gr.aueb.cf.parser.exception.exceptions.DocumentParsingException;

import gr.aueb.cf.parser.service.DocumentParserService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class DocumentUploadedConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    DocumentUploadedConsumer.class
            );

    private final ObjectMapper objectMapper;
    private final DocumentParserService documentParserService;
    private final DocumentParsedProducer documentParsedProducer;
    private final DocumentParseFailedProducer documentParseFailedProducer;
    private final Validator validator;

    public DocumentUploadedConsumer(
            ObjectMapper objectMapper,
            DocumentParserService documentParserService,
            DocumentParsedProducer documentParsedProducer,
            DocumentParseFailedProducer documentParseFailedProducer,
            Validator validator
    ) {
        this.objectMapper = objectMapper;
        this.documentParserService = documentParserService;
        this.documentParsedProducer = documentParsedProducer;
        this.documentParseFailedProducer = documentParseFailedProducer;
        this.validator = validator;
    }

    // ========================================
    // KAFKA DOCUMENT UPLOADED CONSUMER
    // ========================================

    @KafkaListener(
            topics = "document-uploaded",
            groupId = "parser-service"
    )
    public void consume(String message) throws JsonProcessingException {

        long startedAt = System.nanoTime();

        log.info(
                "PARSER | Kafka document-uploaded event received"
        );

        DocumentParseRequestDto request;

        // ====================================
        // DESERIALIZE MESSAGE
        // ====================================

        try {

            request = objectMapper.readValue(
                    message,
                    DocumentParseRequestDto.class
            );

        } catch (JsonProcessingException e) {

            log.error(
                    "PARSER | Invalid JSON message | Error: {}",
                    e.getOriginalMessage()
            );

            // Invalid JSON: the document ID and database
            // cannot be reliably determined.

            DocumentParseFailedDto failedDto =
                    new DocumentParseFailedDto(
                            null,
                            "Invalid JSON message",
                            null
                    );

            documentParseFailedProducer.send(failedDto);

            log.warn(
                    "PARSER | Failure event sent | Reason: Invalid JSON"
            );

            return;
        }

        log.info(
                "PARSER | Document received | ID: {} | Database: {}",
                request.getDocumentId(),
                request.getDatabaseTarget()
        );

        // ====================================
        // VALIDATE REQUEST
        // ====================================

        Set<ConstraintViolation<DocumentParseRequestDto>> violations =
                validator.validate(request);

        if (!violations.isEmpty()) {

            String errorMessage = violations.iterator()
                    .next()
                    .getMessage();

            log.warn(
                    "PARSER | Validation failed | Document ID: {} | Database: {} | Reason: {}",
                    request.getDocumentId(),
                    request.getDatabaseTarget(),
                    errorMessage
            );

            DocumentParseFailedDto failedDto =
                    new DocumentParseFailedDto(
                            request.getDocumentId(),
                            errorMessage,
                            request.getDatabaseTarget()
                    );

            documentParseFailedProducer.send(failedDto);

            log.info(
                    "PARSER | Failure event sent | Document ID: {}",
                    request.getDocumentId()
            );

            return;
        }

        // ====================================
        // VALIDATE FILE PATH
        // ====================================

        if (
                request.getFilePath() == null ||
                        request.getFilePath().isBlank()
        ) {

            log.warn(
                    "PARSER | Missing file path | Document ID: {} | Database: {}",
                    request.getDocumentId(),
                    request.getDatabaseTarget()
            );

            DocumentParseFailedDto failedDto =
                    new DocumentParseFailedDto(
                            request.getDocumentId(),
                            "Document file path is required",
                            request.getDatabaseTarget()
                    );

            documentParseFailedProducer.send(failedDto);

            log.info(
                    "PARSER | Failure event sent | Document ID: {}",
                    request.getDocumentId()
            );

            return;
        }

        // ====================================
        // PROCESS DOCUMENT
        // ====================================

        try {

            log.info(
                    "PARSER | Processing started | Document ID: {} | Database: {}",
                    request.getDocumentId(),
                    request.getDatabaseTarget()
            );

            ParsedDocumentResultDto result =
                    documentParserService.parse(
                            request.getDocumentId(),
                            request.getFilePath(),
                            request.getDatabaseTarget()
                    );

            long durationMs =
                    TimeUnit.NANOSECONDS.toMillis(
                            System.nanoTime() - startedAt
                    );

            log.info(
                    "PARSER | Processing completed | Document ID: {} | Database: {} | Duration: {} ms",
                    request.getDocumentId(),
                    request.getDatabaseTarget(),
                    durationMs
            );

            documentParsedProducer.send(result);

            log.info(
                    "PARSER | Parsed result event sent | Document ID: {}",
                    request.getDocumentId()
            );

        } catch (
                DocumentFileNotFoundException |
                DocumentParsingException e
        ) {

            long durationMs =
                    TimeUnit.NANOSECONDS.toMillis(
                            System.nanoTime() - startedAt
                    );

            log.error(
                    "PARSER | Processing failed | Document ID: {} | Database: {} | Duration: {} ms | Error: {}",
                    request.getDocumentId(),
                    request.getDatabaseTarget(),
                    durationMs,
                    e.getMessage()
            );

            DocumentParseFailedDto failedDto =
                    new DocumentParseFailedDto(
                            request.getDocumentId(),
                            e.getMessage(),
                            request.getDatabaseTarget()
                    );

            documentParseFailedProducer.send(failedDto);

            log.info(
                    "PARSER | Failure event sent | Document ID: {}",
                    request.getDocumentId()
            );
        }
    }
}
