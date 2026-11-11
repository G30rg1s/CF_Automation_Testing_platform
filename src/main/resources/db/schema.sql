
-- ==========================================
-- CODING FACTORY AUTOMATION
-- DATABASE SCHEMA
-- ==========================================
--
-- Executed by DatabaseSchemaInitializer
-- against each physical database:
--
-- PRODUCTION
-- FRONTEND TESTING
-- API TESTING
--
-- Database creation and user permissions
-- are configured separately.
-- ==========================================


-- ==========================================
-- USERS
-- ==========================================

CREATE TABLE IF NOT EXISTS users
(
    id BIGINT NOT NULL AUTO_INCREMENT,

    first_name VARCHAR(100) NOT NULL,

    last_name VARCHAR(100) NOT NULL,

    email VARCHAR(254)
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_bin
    NOT NULL,

    phone VARCHAR(16) NOT NULL,

    father_name VARCHAR(100) NOT NULL,

    mother_name VARCHAR(100) NOT NULL,

    age INT NOT NULL,

    password_hash VARCHAR(100) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_users_email
    UNIQUE (email)

    ) ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- ==========================================
-- CATEGORIES
-- ==========================================

CREATE TABLE IF NOT EXISTS categories
(
    id BIGINT NOT NULL AUTO_INCREMENT,

    name VARCHAR(100) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_category_name
    UNIQUE (name)

    ) ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- ==========================================
-- DOCUMENTS
-- ==========================================

CREATE TABLE IF NOT EXISTS documents
(
    id BIGINT NOT NULL AUTO_INCREMENT,

    title VARCHAR(255) NOT NULL,

    file_path VARCHAR(255) NULL,

    html_content LONGTEXT NULL,

    status VARCHAR(255) NULL,

    error_message VARCHAR(255) NULL,

    document_type VARCHAR(30)
    NOT NULL
    DEFAULT 'DOCUMENT',

    category_id BIGINT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_documents_title
    UNIQUE (title),

    CONSTRAINT fk_document_category
    FOREIGN KEY (category_id)
    REFERENCES categories (id)

    ) ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- ==========================================
-- DOCUMENT FORMATS
-- ==========================================

CREATE TABLE IF NOT EXISTS document_formats
(
    id BIGINT NOT NULL AUTO_INCREMENT,

    document_id BIGINT NOT NULL,

    paragraph_index INT NOT NULL,

    format_type VARCHAR(30) NOT NULL,

    code_language VARCHAR(30) NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_document_format_paragraph
    UNIQUE (
               document_id,
               paragraph_index
           ),

    CONSTRAINT fk_document_format_document
    FOREIGN KEY (document_id)
    REFERENCES documents (id)

    ) ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- ==========================================
-- SAVED SQL QUERIES
-- ==========================================

CREATE TABLE IF NOT EXISTS saved_sql_queries
(
    id BIGINT NOT NULL AUTO_INCREMENT,

    query_uuid VARCHAR(36) NOT NULL,

    title VARCHAR(200) NOT NULL,

    sql_text LONGTEXT NOT NULL,

    owner VARCHAR(255) NOT NULL,

    created_at DATETIME(6) NOT NULL,

    updated_at DATETIME(6) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_saved_sql_query_uuid
    UNIQUE (query_uuid),

    INDEX idx_saved_sql_owner (owner)

    ) ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- ==========================================
-- REQUIRED SYSTEM DATA
-- ==========================================

INSERT INTO categories (name)
SELECT 'Διάφορα'
    WHERE NOT EXISTS (
    SELECT 1
    FROM categories
    WHERE name = 'Διάφορα'
);
