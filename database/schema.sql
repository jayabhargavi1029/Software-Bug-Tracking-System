-- Software Bug & Ticket Tracking System - MySQL 8 schema
-- OPTIONAL: the application creates/updates these tables automatically (spring.jpa.hibernate.ddl-auto=update).
-- Use this file only if you want to create the database manually or show the schema in your report.
-- Demo users and tickets are inserted by the application on first start (DemoDataLoader), not by this file.

CREATE DATABASE IF NOT EXISTS bugtracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bugtracker;

CREATE TABLE IF NOT EXISTS app_users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(120) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,              -- BCrypt hash
    role        VARCHAR(20)  NOT NULL,              -- ADMIN / DEVELOPER
    active      BIT          NOT NULL DEFAULT 1,
    access_revoked BIT       NOT NULL DEFAULT 0,
    -- Retained for legacy rows anonymized by older versions.
    anonymized  BIT          NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL
);

CREATE TABLE IF NOT EXISTS bug_tickets (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id      VARCHAR(20) UNIQUE,              -- BUG-1001, BUG-1002 ...
    title          VARCHAR(150)  NOT NULL,
    description    VARCHAR(4000) NOT NULL,
    category       VARCHAR(30)   NOT NULL,
    priority       VARCHAR(20)   NOT NULL,          -- LOW / MEDIUM / HIGH / CRITICAL
    severity       VARCHAR(20)   NOT NULL,          -- MINOR / MODERATE / MAJOR / BLOCKER
    status         VARCHAR(20)   NOT NULL,          -- OPEN / IN_PROGRESS / RESOLVED / CLOSED / REOPENED
    reported_by_id BIGINT        NOT NULL,
    assigned_to_id BIGINT        NULL,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    CONSTRAINT fk_ticket_reporter FOREIGN KEY (reported_by_id) REFERENCES app_users(id),
    CONSTRAINT fk_ticket_assignee FOREIGN KEY (assigned_to_id) REFERENCES app_users(id)
);

CREATE TABLE IF NOT EXISTS comments (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id   BIGINT        NOT NULL,
    author_id   BIGINT        NOT NULL,
    message     VARCHAR(2000) NOT NULL,
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    CONSTRAINT fk_comment_ticket FOREIGN KEY (ticket_id) REFERENCES bug_tickets(id),
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES app_users(id)
);

CREATE TABLE IF NOT EXISTS ticket_history (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id     BIGINT       NOT NULL,
    changed_by_id BIGINT       NOT NULL,
    action        VARCHAR(60)  NOT NULL,
    details       VARCHAR(500) NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    CONSTRAINT fk_history_ticket FOREIGN KEY (ticket_id) REFERENCES bug_tickets(id),
    CONSTRAINT fk_history_user   FOREIGN KEY (changed_by_id) REFERENCES app_users(id)
);
