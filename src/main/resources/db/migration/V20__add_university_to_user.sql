ALTER TABLE `user`
    ADD COLUMN university_id BIGINT,
    ADD CONSTRAINT fk_user_university FOREIGN KEY (university_id) REFERENCES university (id);

CREATE INDEX idx_university_domain
    ON university (domain);