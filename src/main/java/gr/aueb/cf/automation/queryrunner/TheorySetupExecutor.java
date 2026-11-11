
package gr.aueb.cf.automation.queryrunner;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TheorySetupExecutor {

    private static final String ROOT = "theory-setup/";

    private static final String DOCUMENTS_CONTENT =
            ROOT + "documents_content/";

    private static final String FORMATS_CONTENT =
            ROOT + "formats_content/";

    private static final String DOCUMENT_PROPERTIES =
            ROOT + "total_documents_insert/";

    private static final String FORMAT_PROPERTIES =
            ROOT + "total_formats_insert/";

    private static final String SQL = ROOT + "sql/";

    private static final String CATEGORIES =
            ROOT + "categories/";

    private static final Pattern DOCUMENT_PATTERN =
            Pattern.compile(
                    "([1-9][0-9]*)_(.+)_document\\.properties"
            );

    private static final Pattern PARAGRAPH_PATTERN =
            Pattern.compile(
                    "<p(?:\\s[^>]*)?>",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Set<String> ALLOWED_FORMAT_TYPES =
            Set.of(
                    "TITLE",
                    "HEADING",
                    "PARAGRAPH",
                    "QUOTE",
                    "CODE",
                    "LINK"
            );

    private final QueryBatchRunner queryBatchRunner;

    private final PathMatchingResourcePatternResolver resolver =
            new PathMatchingResourcePatternResolver();

    public TheorySetupExecutor(
            QueryBatchRunner queryBatchRunner
    ) {
        this.queryBatchRunner = queryBatchRunner;
    }

    public boolean isConfigured() {
        return !buildTheorySetupBatch()
                .getSteps()
                .isEmpty();
    }

    public QueryExecutionContext execute(
            DatabaseTarget target
    ) {

        if (target == null) {
            throw new IllegalArgumentException(
                    "Database target is required."
            );
        }

        QueryBatch batch = buildTheorySetupBatch();

        return queryBatchRunner.run(batch, target);
    }

    private record TheoryDocumentResource(
            int id,
            String name,
            String documentPropertiesPath,
            String formatPropertiesPath
    ) {}

    private record PreparedDocument(
            String name,
            String documentSql,
            String formatsSql
    ) {}

    // Discover documents from total_documents_insert/.
    private List<TheoryDocumentResource> discoverDocuments() {

        List<TheoryDocumentResource> documents =
                new ArrayList<>();

        Map<Integer, String> usedIds = new HashMap<>();

        try {

            Resource[] resources = resolver.getResources(
                    "classpath*:"
                            + DOCUMENT_PROPERTIES
                            + "*_document.properties"
            );

            for (Resource resource : resources) {

                String filename = resource.getFilename();

                if (filename == null) {
                    throw new IllegalStateException(
                            "Document properties without filename."
                    );
                }

                Matcher matcher =
                        DOCUMENT_PATTERN.matcher(filename);

                if (!matcher.matches()) {
                    throw new IllegalStateException(
                            "Invalid document properties filename: "
                                    + filename
                    );
                }

                int id = Integer.parseInt(
                        matcher.group(1)
                );

                String name = matcher.group(2);

                String previous = usedIds.putIfAbsent(
                        id,
                        filename
                );

                if (previous != null) {
                    throw new IllegalStateException(
                            "Duplicate document ID "
                                    + id
                                    + ": "
                                    + previous
                                    + " / "
                                    + filename
                    );
                }

                String baseName = id + "_" + name;

                String documentPropertiesPath =
                        DOCUMENT_PROPERTIES
                                + baseName
                                + "_document.properties";

                String formatPropertiesPath =
                        FORMAT_PROPERTIES
                                + baseName
                                + "_formats.properties";

                requireResource(documentPropertiesPath);
                requireResource(formatPropertiesPath);

                documents.add(
                        new TheoryDocumentResource(
                                id,
                                name,
                                documentPropertiesPath,
                                formatPropertiesPath
                        )
                );
            }

            if (documents.isEmpty()) {
                throw new IllegalStateException(
                        "No document properties found in "
                                + DOCUMENT_PROPERTIES
                );
            }

            documents.sort(
                    Comparator.comparingInt(
                            TheoryDocumentResource::id
                    )
            );

            for (int i = 0; i < documents.size(); i++) {

                int expectedId = i + 1;

                if (documents.get(i).id() != expectedId) {
                    throw new IllegalStateException(
                            "Expected document ID "
                                    + expectedId
                                    + " but found "
                                    + documents.get(i).id()
                    );
                }
            }

            return documents;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to discover Theory documents.",
                    e
            );
        }
    }

    // Build the complete query batch.
    private QueryBatch buildTheorySetupBatch() {

        List<TheoryDocumentResource> documents =
                discoverDocuments();

        List<String> categorySqlStatements = buildCategorySql();

        String documentTemplate = loadText(
                SQL + "documents.sql"
        );

        String formatsTemplate = loadText(
                SQL + "document_formats.sql"
        );

        validateTemplate(
                documentTemplate,
                "${DOCUMENT_ID}",
                "${DOCUMENT_TITLE}",
                "${FILE_PATH}",
                "${HTML_HEX}",
                "${STATUS}",
                "${ERROR_MESSAGE}",
                "${DOCUMENT_TYPE}",
                "${CATEGORY_ID}"
        );

        validateTemplate(
                formatsTemplate,
                "${FORMAT_VALUES}"
        );

        List<PreparedDocument> preparedDocuments =
                new ArrayList<>();

        int nextFormatId = 1;

        for (TheoryDocumentResource document : documents) {

            String baseName =
                    document.id() + "_" + document.name();

            Properties documentProperties = loadProperties(
                    document.documentPropertiesPath()
            );

            Properties formatProperties = loadProperties(
                    document.formatPropertiesPath()
            );

            // Validate document ID.
            int propertyDocumentId = Integer.parseInt(
                    requiredPositiveInt(
                            documentProperties,
                            "id"
                    )
            );

            if (propertyDocumentId != document.id()) {
                throw new IllegalStateException(
                        "Document ID mismatch for "
                                + baseName
                                + ". Expected "
                                + document.id()
                                + ", found "
                                + propertyDocumentId
                );
            }

            // Validate format -> document relationship.
            int formatDocumentId = Integer.parseInt(
                    requiredPositiveInt(
                            formatProperties,
                            "document_id"
                    )
            );

            if (formatDocumentId != document.id()) {
                throw new IllegalStateException(
                        "Format document_id mismatch for "
                                + baseName
                                + ". Expected "
                                + document.id()
                                + ", found "
                                + formatDocumentId
                );
            }

            /*
             * Some properties may reference old content folders.
             * Validate the filename and load from the actual folders.
             */
            String htmlPath = resolveContentPath(
                    requiredProperty(
                            documentProperties,
                            "html_file"
                    ),
                    DOCUMENTS_CONTENT,
                    baseName + ".html"
            );

            String formatsPath = resolveContentPath(
                    requiredProperty(
                            formatProperties,
                            "format.types_file"
                    ),
                    FORMATS_CONTENT,
                    baseName + "_formats.txt"
            );

            String html = loadText(htmlPath);

            String documentSql = buildDocumentSql(
                    documentProperties,
                    documentTemplate,
                    html
            );

            List<String> formatTypes =
                    readFormatTypes(formatsPath);

            validateParagraphCount(
                    baseName,
                    html,
                    formatTypes.size()
            );

            String formatValues = buildFormatValues(
                    document.id(),
                    nextFormatId,
                    formatTypes,
                    formatProperties
            );

            String formatsSql = formatsTemplate.replace(
                    "${FORMAT_VALUES}",
                    formatValues
            );

            validateResolvedSql(formatsSql);

            preparedDocuments.add(
                    new PreparedDocument(
                            baseName,
                            documentSql,
                            formatsSql
                    )
            );

            nextFormatId += formatTypes.size();
        }

        QueryBatch batch =
                new QueryBatch("Initialize Theory");

        // Preserve existing setup behavior.
        // ALTER AUTO_INCREMENT does not delete existing rows.
        batch.add(
                "Reset documents AUTO_INCREMENT",
                "ALTER TABLE documents AUTO_INCREMENT = 1"
        );

        batch.add(
                "Reset document_formats AUTO_INCREMENT",
                "ALTER TABLE document_formats AUTO_INCREMENT = 1"
        );

        for (int i = 0; i < categorySqlStatements.size(); i++) {
            batch.add(
                    "Insert category " + (i + 1),
                    categorySqlStatements.get(i)
            );
        }

        for (PreparedDocument prepared : preparedDocuments) {

            batch.add(
                    "Insert document: " + prepared.name(),
                    prepared.documentSql()
            );

            batch.add(
                    "Insert formats: " + prepared.name(),
                    prepared.formatsSql()
            );
        }

        return batch;
    }

    // Resolve a content file using its expected filename.
    private String resolveContentPath(
            String declaredPath,
            String actualFolder,
            String expectedFilename
    ) {

        String normalized = declaredPath
                .replace('\\', '/')
                .trim();

        String declaredFilename = normalized.substring(
                normalized.lastIndexOf('/') + 1
        );

        if (!expectedFilename.equals(declaredFilename)) {
            throw new IllegalStateException(
                    "Content filename mismatch. Expected "
                            + expectedFilename
                            + ", found "
                            + declaredFilename
            );
        }

        String actualPath =
                actualFolder + expectedFilename;

        requireResource(actualPath);

        return actualPath;
    }

    // Build category INSERT.

    private List<String> buildCategorySql() {

        Properties properties = loadProperties(
                CATEGORIES + "categories.properties"
        );

        String template = loadText(
                SQL + "categories.sql"
        );

        validateTemplate(
                template,
                "${CATEGORY_ID}",
                "${CATEGORY_NAME}"
        );

        String[] categoryIds = requiredProperty(
                properties,
                "theory.category.ids"
        ).split(",");

        List<String> categorySqlStatements = new ArrayList<>();
        Set<Integer> usedCategoryIds = new java.util.HashSet<>();

        for (String categoryIdValue : categoryIds) {

            int categoryId;

            try {
                categoryId = Integer.parseInt(categoryIdValue.trim());
            } catch (NumberFormatException e) {
                throw new IllegalStateException(
                        "Invalid category ID: " + categoryIdValue,
                        e
                );
            }

            if (categoryId <= 1) {
                throw new IllegalStateException(
                        "Category ID must be greater than 1: " + categoryId
                );
            }

            if (!usedCategoryIds.add(categoryId)) {
                throw new IllegalStateException(
                        "Duplicate category ID: " + categoryId
                );
            }

            String categoryName = requiredProperty(
                    properties,
                    "theory.category." + categoryId + ".name"
            );

            String sql = template
                    .replace(
                            "${CATEGORY_ID}",
                            Integer.toString(categoryId)
                    )
                    .replace(
                            "${CATEGORY_NAME}",
                            sqlEscape(categoryName)
                    );

            validateResolvedSql(sql);

            categorySqlStatements.add(sql);
        }

        return categorySqlStatements;
    }


    // Build documents INSERT using metadata and HTML.
    private String buildDocumentSql(
            Properties properties,
            String template,
            String html
    ) {

        String documentId = requiredPositiveInt(
                properties,
                "id"
        );

        String title = requiredProperty(
                properties,
                "title"
        );

        String filePath = requiredProperty(
                properties,
                "file_path"
        );

        String status = requiredProperty(
                properties,
                "status"
        );

        String errorMessage = requiredProperty(
                properties,
                "error_message"
        );

        String documentType = requiredProperty(
                properties,
                "document_type"
        );

        String categoryId = requiredPositiveInt(
                properties,
                "category_id"
        );

        String htmlHex = HexFormat.of().formatHex(
                html.getBytes(StandardCharsets.UTF_8)
        );

        String errorMessageSql =
                "NULL".equalsIgnoreCase(errorMessage)
                        ? "NULL"
                        : "'" + sqlEscape(errorMessage) + "'";

        String sql = template
                .replace(
                        "${DOCUMENT_ID}",
                        documentId
                )
                .replace(
                        "${DOCUMENT_TITLE}",
                        sqlEscape(title)
                )
                .replace(
                        "${FILE_PATH}",
                        sqlEscape(filePath)
                )
                .replace(
                        "${HTML_HEX}",
                        htmlHex
                )
                .replace(
                        "${STATUS}",
                        sqlEscape(status)
                )
                .replace(
                        "${ERROR_MESSAGE}",
                        errorMessageSql
                )
                .replace(
                        "${DOCUMENT_TYPE}",
                        sqlEscape(documentType)
                )
                .replace(
                        "${CATEGORY_ID}",
                        categoryId
                );

        validateResolvedSql(sql);

        return sql;
    }

    // Read format types from formats_content/*.txt.
    private List<String> readFormatTypes(String formatPath) {

        String raw = loadText(formatPath).trim();

        if (raw.isEmpty()) {
            throw new IllegalStateException(
                    "Empty format types file: " + formatPath
            );
        }

        List<String> formats = Arrays.stream(
                        raw.split(",", -1)
                )
                .map(String::trim)
                .toList();

        for (int i = 0; i < formats.size(); i++) {

            String type = formats.get(i);

            if (!ALLOWED_FORMAT_TYPES.contains(type)) {
                throw new IllegalStateException(
                        "Invalid format type in "
                                + formatPath
                                + " at index "
                                + i
                                + ": "
                                + type
                );
            }
        }

        return formats;
    }

    private Map<Integer, String> resolveCodeLanguages(
            Properties properties,
            List<String> formatTypes
    ) {

        Map<Integer, String> languages = new HashMap<>();

        boolean hasNewProperty =
                properties.containsKey("format.code_languages");

        boolean hasOldIndex =
                properties.containsKey("format.code_language_index");

        boolean hasOldLanguage =
                properties.containsKey("format.code_language");

        if (hasNewProperty && (hasOldIndex || hasOldLanguage)) {
            throw new IllegalStateException(
                    "Do not combine format.code_languages with "
                            + "format.code_language_index or "
                            + "format.code_language."
            );
        }

        if (hasNewProperty) {

            String value = properties.getProperty(
                    "format.code_languages",
                    ""
            ).trim();

            if (!value.isEmpty()) {

                String[] entries = value.split(",", -1);

                for (String entry : entries) {

                    String trimmedEntry = entry.trim();

                    String[] parts = trimmedEntry.split(":", -1);

                    if (parts.length != 2) {
                        throw new IllegalStateException(
                                "Invalid format.code_languages entry: "
                                        + trimmedEntry
                                        + ". Expected index:LANGUAGE."
                        );
                    }

                    String indexText = parts[0].trim();
                    String language = parts[1].trim();

                    int index;

                    try {
                        index = Integer.parseInt(indexText);
                    } catch (NumberFormatException e) {
                        throw new IllegalStateException(
                                "Invalid CODE paragraph index: "
                                        + indexText,
                                e
                        );
                    }

                    validateCodeLanguage(
                            index,
                            language,
                            formatTypes
                    );

                    if (languages.putIfAbsent(index, language) != null) {
                        throw new IllegalStateException(
                                "Duplicate CODE paragraph index: "
                                        + index
                        );
                    }
                }
            }

        } else if (hasOldIndex || hasOldLanguage) {

            if (!hasOldIndex) {
                throw new IllegalStateException(
                        "Missing format.code_language_index "
                                + "for legacy format properties."
                );
            }

            int codeIndex = Integer.parseInt(
                    requiredInteger(
                            properties,
                            "format.code_language_index"
                    )
            );

            String codeLanguage = properties.getProperty(
                    "format.code_language",
                    "NONE"
            ).trim();

            if (codeIndex < -1 || codeIndex >= formatTypes.size()) {
                throw new IllegalStateException(
                        "Invalid code_language_index: " + codeIndex
                );
            }

            if (codeIndex >= 0) {

                validateCodeLanguage(
                        codeIndex,
                        codeLanguage,
                        formatTypes
                );

                languages.put(codeIndex, codeLanguage);

            } else if (!codeLanguage.isEmpty()
                    && !"NONE".equalsIgnoreCase(codeLanguage)) {

                throw new IllegalStateException(
                        "format.code_language must be NONE "
                                + "when format.code_language_index=-1."
                );
            }
        }

        // Every CODE paragraph must have exactly one language.
        // Non-CODE paragraphs must not have any language.
        for (int i = 0; i < formatTypes.size(); i++) {

            String type = formatTypes.get(i);

            if ("CODE".equals(type) && !languages.containsKey(i)) {
                throw new IllegalStateException(
                        "Missing code language for CODE "
                                + "at paragraph_index=" + i
                );
            }

            if (!"CODE".equals(type) && languages.containsKey(i)) {
                throw new IllegalStateException(
                        "Code language specified for non-CODE "
                                + "at paragraph_index=" + i
                );
            }
        }

        return languages;
    }

    private void validateCodeLanguage(
            int index,
            String language,
            List<String> formatTypes
    ) {

        if (index < 0 || index >= formatTypes.size()) {
            throw new IllegalStateException(
                    "Invalid CODE paragraph index: " + index
            );
        }

        if (!"CODE".equals(formatTypes.get(index))) {
            throw new IllegalStateException(
                    "CODE language index " + index
                            + " must point to CODE, found "
                            + formatTypes.get(index)
            );
        }

        if (language.isBlank()
                || "NONE".equalsIgnoreCase(language)
                || "NULL".equalsIgnoreCase(language)) {

            throw new IllegalStateException(
                    "CODE at paragraph_index=" + index
                            + " requires a valid language."
            );
        }

        if (!language.matches("[A-Za-z][A-Za-z0-9_+.#-]*")) {
            throw new IllegalStateException(
                    "Invalid code language at paragraph_index="
                            + index + ": " + language
            );
        }
    }

    // Generate globally sequential format IDs.
    private String buildFormatValues(
            int documentId,
            int firstFormatId,
            List<String> formatTypes,
            Properties properties
    ) {

        Map<Integer, String> codeLanguages =
                resolveCodeLanguages(
                        properties,
                        formatTypes
                );

        StringBuilder values = new StringBuilder();

        for (int i = 0; i < formatTypes.size(); i++) {

            if (i > 0) {
                values.append(",\n");
            }

            int formatId = firstFormatId + i;

            String formatType = formatTypes.get(i);

            String codeLanguage = codeLanguages.get(i);

            String languageSql = codeLanguage == null
                    ? "NULL"
                    : "'" + sqlEscape(codeLanguage) + "'";

            values.append("(")
                    .append(formatId)
                    .append(", ")
                    .append(documentId)
                    .append(", ")
                    .append(i)
                    .append(", '")
                    .append(formatType)
                    .append("', ")
                    .append(languageSql)
                    .append(")");
        }

        return values.toString();
    }

    private void validateParagraphCount(
            String documentName,
            String html,
            int formatCount
    ) {

        Matcher matcher =
                PARAGRAPH_PATTERN.matcher(html);

        int paragraphCount = 0;

        while (matcher.find()) {
            paragraphCount++;
        }

        if (paragraphCount != formatCount) {
            throw new IllegalStateException(
                    "Paragraph/format count mismatch for "
                            + documentName
                            + ". HTML paragraphs: "
                            + paragraphCount
                            + ", format entries: "
                            + formatCount
            );
        }
    }

    private void validateTemplate(
            String template,
            String... placeholders
    ) {

        for (String placeholder : placeholders) {

            if (!template.contains(placeholder)) {
                throw new IllegalStateException(
                        "Missing SQL placeholder: " + placeholder
                );
            }
        }
    }

    private void validateResolvedSql(String sql) {

        if (sql.contains("${")) {
            throw new IllegalStateException(
                    "SQL contains unresolved placeholders."
            );
        }
    }

    private void requireResource(String path) {

        Resource resource = resolver.getResource(
                "classpath:" + path
        );

        if (!resource.exists()) {
            throw new IllegalStateException(
                    "Missing resource: " + path
            );
        }
    }

    private String loadText(String path) {

        Resource resource = resolver.getResource(
                "classpath:" + path
        );

        try (InputStream input = resource.getInputStream()) {

            return new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load resource: " + path,
                    e
            );
        }
    }

    private Properties loadProperties(String path) {

        Resource resource = resolver.getResource(
                "classpath:" + path
        );

        Properties properties = new Properties();

        try (InputStream input = resource.getInputStream();
             InputStreamReader reader = new InputStreamReader(
                     input,
                     StandardCharsets.UTF_8
             )) {

            properties.load(reader);

            return properties;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load properties: " + path,
                    e
            );
        }
    }

    private String requiredProperty(
            Properties properties,
            String key
    ) {

        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing property: " + key
            );
        }

        return value.trim();
    }

    private String requiredInteger(
            Properties properties,
            String key
    ) {

        String value = requiredProperty(
                properties,
                key
        );

        try {
            return Integer.toString(
                    Integer.parseInt(value)
            );
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Invalid integer property: " + key,
                    e
            );
        }
    }

    private String requiredPositiveInt(
            Properties properties,
            String key
    ) {

        int number = Integer.parseInt(
                requiredInteger(properties, key)
        );

        if (number <= 0) {
            throw new IllegalStateException(
                    "Property must be positive: " + key
            );
        }

        return Integer.toString(number);
    }

    private String sqlEscape(String value) {
        return value.replace("'", "''");
    }
}
