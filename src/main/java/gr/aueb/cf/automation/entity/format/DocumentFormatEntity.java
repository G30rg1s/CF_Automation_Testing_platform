package gr.aueb.cf.automation.entity.format;

import gr.aueb.cf.automation.entity.documents.DocumentEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "document_formats", uniqueConstraints =
        {@UniqueConstraint(name = "uk_document_format_paragraph", columnNames = {"document_id", "paragraph_index"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFormatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ========================================
       DOCUMENT
       ======================================== */

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, foreignKey = @ForeignKey(name = "fk_document_format_document"))
    private DocumentEntity document;

    /* ========================================
       PARAGRAPH INDEX
       ======================================== */

    @Column(name = "paragraph_index", nullable = false)
    private Integer paragraphIndex;

    /* ========================================
       FORMAT TYPE
       ======================================== */

    @Enumerated(EnumType.STRING)
    @Column(name = "format_type", nullable = false, length = 30)
    private DocumentFormatType formatType;

    /* ========================================
       CODE LANGUAGE
       ======================================== */

    @Enumerated(EnumType.STRING)
    @Column(name = "code_language", length = 30)
    private CodeLanguage codeLanguage;
}