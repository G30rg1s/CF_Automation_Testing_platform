package gr.aueb.cf.automation.exception.handlers;

import gr.aueb.cf.automation.dto.documents.ExistingDocumentDto;
import gr.aueb.cf.automation.exception.exceptions.DocumentAlreadyExistsException;
import gr.aueb.cf.automation.exception.exceptions.DocumentPendingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;


@RestControllerAdvice
public class DocumentExceptionHandler {

    /* ========================================
       DOCUMENT ALREADY EXISTS
       ======================================== */

    @ExceptionHandler(DocumentAlreadyExistsException.class)
    public ResponseEntity<ExistingDocumentDto> handleDocumentAlreadyExists(DocumentAlreadyExistsException exception) {
        ExistingDocumentDto response =
                new ExistingDocumentDto(
                        exception.getDocumentId(),
                        exception.getTitle()
                );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }


    /* ========================================
       DOCUMENT PENDING
       ======================================== */

    @ExceptionHandler(DocumentPendingException.class)
    public ResponseEntity<Map<String, String>> handleDocumentPending(DocumentPendingException exception) {
        return ResponseEntity
                .status(HttpStatus.LOCKED)
                .body(
                        Map.of(
                                "message",
                                exception.getMessage()
                        )
                );
    }
}