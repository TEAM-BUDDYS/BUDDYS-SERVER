CREATE TABLE university_verification
(
    user_id       BIGINT       PRIMARY KEY,
    university_id BIGINT       NOT NULL,
    email         VARCHAR(255) NOT NULL,
    token         VARCHAR(255) NOT NULL,
    expires_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_university_verification_token UNIQUE (token),
    CONSTRAINT fk_university_verification_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_university_verification_university FOREIGN KEY (university_id) REFERENCES university (id)
);
