-- V18: Add notifications table
CREATE TABLE notifications (
    id           VARCHAR(36)   NOT NULL,
    target_role  VARCHAR(50),
    title        VARCHAR(255)  NOT NULL,
    message      VARCHAR(1000) NOT NULL,
    type         VARCHAR(50),
    is_read      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    CONSTRAINT pk_notifications PRIMARY KEY (id)
);
