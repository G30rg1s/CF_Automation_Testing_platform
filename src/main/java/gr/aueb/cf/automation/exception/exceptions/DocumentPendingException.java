package gr.aueb.cf.automation.exception.exceptions;

public class DocumentPendingException extends RuntimeException {

    public DocumentPendingException(Long documentId) {
        super("Document with id " + documentId + " is currently being processed and cannot be reuploaded.");
    }
}