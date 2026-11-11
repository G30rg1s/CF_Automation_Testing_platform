package gr.aueb.cf.automation.exception.exceptions;

import lombok.Getter;

@Getter
public class DocumentAlreadyExistsException extends RuntimeException {

    private final Long documentId;
    private final String title;

    public DocumentAlreadyExistsException(
            Long documentId,
            String title
    ) {
        super("Document already exists with title: " + title);

        this.documentId = documentId;
        this.title = title;
    }
}