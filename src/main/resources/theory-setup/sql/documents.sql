
INSERT INTO documents (
    id,
    title,
    file_path,
    html_content,
    status,
    error_message,
    document_type,
    category_id
)
VALUES (
           ${DOCUMENT_ID},
           '${DOCUMENT_TITLE}',
           '${FILE_PATH}',
           CONVERT(0x${HTML_HEX} USING utf8mb4),
           '${STATUS}',
           ${ERROR_MESSAGE},
           '${DOCUMENT_TYPE}',
           ${CATEGORY_ID}
       );
