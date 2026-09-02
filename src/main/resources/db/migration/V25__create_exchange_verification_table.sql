CREATE TABLE exchange_verification
(
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                 BIGINT       NOT NULL,
    document_key            VARCHAR(512) NOT NULL,
    original_file_name      VARCHAR(255) NOT NULL,
    content_type            VARCHAR(100) NOT NULL,
    file_size               BIGINT       NOT NULL,
    status                  VARCHAR(20)  NOT NULL,
    rejection_reason        VARCHAR(500),
    reviewed_by             BIGINT,
    reviewed_at             DATETIME(6),
    version                 BIGINT       NOT NULL DEFAULT 0,
    pending_user_id         BIGINT GENERATED ALWAYS AS (
                                CASE WHEN status = 'PENDING' THEN user_id ELSE NULL END
                            ) STORED,
    created_at              DATETIME(6)  NOT NULL,
    updated_at              DATETIME(6)  NOT NULL,
    CONSTRAINT uk_exchange_verification_document_key UNIQUE (document_key),
    CONSTRAINT uk_exchange_verification_pending_user UNIQUE (pending_user_id),
    CONSTRAINT fk_exchange_verification_user
        FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_exchange_verification_reviewer
        FOREIGN KEY (reviewed_by) REFERENCES `user` (id),
    CONSTRAINT chk_exchange_verification_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_exchange_verification_file_size
        CHECK (file_size > 0)
);

CREATE INDEX idx_exchange_verification_status_created_at
    ON exchange_verification (status, created_at);

CREATE INDEX idx_exchange_verification_user_created_at
    ON exchange_verification (user_id, created_at);
