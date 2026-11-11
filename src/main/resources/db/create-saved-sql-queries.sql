
-- ==========================================
-- CODING FACTORY AUTOMATION
-- SAVED SQL QUERIES TABLE
-- ==========================================

-- This script runs separately against each
-- configured physical DataSource.
--
-- LOCAL:
-- springboot-production
-- springboot-frontendtesting
-- springboot-apitesting
--
-- DOCKER:
-- production
-- frontendtesting
-- apitesting

CREATE TABLE IF NOT EXISTS saved_sql_queries (
                                                 id BIGINT NOT NULL AUTO_INCREMENT,
                                                 query_uuid VARCHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    sql_text LONGTEXT NOT NULL,
    owner VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_saved_sql_query_uuid (query_uuid),
    INDEX idx_saved_sql_owner (owner)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
