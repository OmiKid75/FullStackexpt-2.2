-- =====================================================================
-- Campus Library Resource Explorer API - MySQL 8 setup script
-- Run in MySQL Workbench or:  mysql -u root -p < database/schema.sql
-- =====================================================================

-- 1) Create the database (the app can also do this itself because the default
--    JDBC URL contains createDatabaseIfNotExist=true).
CREATE DATABASE IF NOT EXISTS campus_library
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2) OPTIONAL: a dedicated user instead of root.
--    CHANGE THE PASSWORD before running. Do not commit real passwords.
-- CREATE USER IF NOT EXISTS 'campus_user'@'localhost' IDENTIFIED BY 'CHANGE_ME';
-- GRANT ALL PRIVILEGES ON campus_library.* TO 'campus_user'@'localhost';
-- FLUSH PRIVILEGES;

-- 3) Tables are created automatically by Hibernate (spring.jpa.hibernate.ddl-auto=update).
--    The reference DDL below shows what Hibernate generates. You do NOT need to run it.
--
-- CREATE TABLE categories (
--     id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
--     name        VARCHAR(100) NOT NULL UNIQUE,
--     description VARCHAR(500)
-- );
--
-- CREATE TABLE resources (
--     id            BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
--     title         VARCHAR(200)  NOT NULL,
--     description   VARCHAR(2000),
--     author        VARCHAR(150),
--     category_id   BIGINT        NOT NULL,
--     resource_type VARCHAR(30)   NOT NULL,
--     rating        DOUBLE        NOT NULL,
--     created_at    DATETIME(6)   NOT NULL,
--     updated_at    DATETIME(6)   NOT NULL,
--     CONSTRAINT fk_resources_category FOREIGN KEY (category_id) REFERENCES categories (id)
-- );
-- CREATE INDEX idx_resources_category   ON resources (category_id);
-- CREATE INDEX idx_resources_created_at ON resources (created_at);
-- CREATE INDEX idx_resources_rating     ON resources (rating);
-- CREATE INDEX idx_resources_title      ON resources (title);

-- 4) To start again with fresh sample data (DESTROYS all data in this database):
-- DROP DATABASE campus_library;
-- then re-run step 1 and restart the application.
