package gr.aueb.cf.automation.entity.documents;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /* ========================================
       TITLE
       ======================================== */

    @Column(
            nullable = false,
            unique = true
    )
    private String title;


    /* ========================================
       FILE PATH
       ======================================== */

    private String filePath;


    /* ========================================
       HTML CONTENT
       ======================================== */

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String htmlContent;


    /* ========================================
       DOCUMENT STATUS
       ======================================== */

    @Enumerated(EnumType.STRING)
    private DocumentStatus status;


    /* ========================================
       ERROR MESSAGE
       ======================================== */

    private String errorMessage;


    /* ========================================
       DOCUMENT TYPE
       ======================================== */

    @Enumerated(EnumType.STRING)
    @Column(
            name = "document_type",
            nullable = false,
            length = 30
    )
    private DocumentType documentType =
            DocumentType.DOCUMENT;


    /* ========================================
       CATEGORY
       ======================================== */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "category_id",
            nullable = true,
            foreignKey = @ForeignKey(
                    name = "fk_document_category"
            )
    )
    private CategoryEntity category;


    /* ========================================
       DEFAULT DOCUMENT TYPE
       ======================================== */

    @PrePersist
    private void applyDefaults() {

        if (documentType == null) {

            documentType =
                    DocumentType.DOCUMENT;
        }
    }
}