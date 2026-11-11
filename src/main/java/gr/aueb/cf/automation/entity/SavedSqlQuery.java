
package gr.aueb.cf.automation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "saved_sql_queries",
        indexes = {
                @Index(
                        name = "idx_saved_sql_owner",
                        columnList = "owner"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_saved_sql_query_uuid",
                        columnNames = "query_uuid"
                )
        }
)
public class SavedSqlQuery {

    // ========================================
    // LOCAL DATABASE ID
    // ========================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================
    // SHARED QUERY UUID
    // ========================================

    @Column(
            name = "query_uuid",
            nullable = false,
            length = 36,
            updatable = false
    )
    private String queryUuid;

    // ========================================
    // TITLE
    // ========================================

    @Column(
            name = "title",
            nullable = false,
            length = 200
    )
    private String title;

    // ========================================
    // SQL CONTENT
    // ========================================

    @Lob
    @Column(
            name = "sql_text",
            nullable = false,
            columnDefinition = "LONGTEXT"
    )
    private String sqlText;

    // ========================================
    // OWNER
    // ========================================

    @Column(
            name = "owner",
            nullable = false,
            length = 255
    )
    private String owner;

    // ========================================
    // TIMESTAMPS
    // ========================================

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    // ========================================
    // CONSTRUCTOR
    // ========================================

    public SavedSqlQuery() {
    }

    // ========================================
    // BEFORE INSERT
    // ========================================

    @PrePersist
    public void onCreate() {

        if (queryUuid == null || queryUuid.isBlank()) {
            queryUuid = UUID.randomUUID().toString();
        }

        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    // ========================================
    // BEFORE UPDATE
    // ========================================

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========================================
    // GETTERS AND SETTERS
    // ========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQueryUuid() {
        return queryUuid;
    }

    public void setQueryUuid(String queryUuid) {
        this.queryUuid = queryUuid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
