CREATE TABLE country
(
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    CONSTRAINT uk_country_name UNIQUE (name)
);

CREATE TABLE city
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    country_id BIGINT      NOT NULL,
    name       VARCHAR(80) NOT NULL,
    CONSTRAINT uk_city_country_name UNIQUE (country_id, name),
    FOREIGN KEY (country_id) REFERENCES country (id)
);

CREATE TABLE `user`
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    email                VARCHAR(255) NOT NULL,
    provider             VARCHAR(10)  NOT NULL,
    provider_id          VARCHAR(255) NOT NULL,
    nickname             VARCHAR(50)  NOT NULL,
    profile_image_url    VARCHAR(512),
    introduction         VARCHAR(150),
    birth_date           DATE,
    gender               VARCHAR(10),
    notification_enabled BOOLEAN      NOT NULL DEFAULT TRUE,
    account_status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    exchange_country_id  BIGINT,
    exchange_university  VARCHAR(100),
    exchange_start_date  DATE,
    exchange_end_date    DATE,
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    deleted_at           DATETIME(6),
    CONSTRAINT uk_user_provider UNIQUE (provider, provider_id),
    CONSTRAINT uk_user_nickname UNIQUE (nickname),
    FOREIGN KEY (exchange_country_id) REFERENCES country (id)
);

CREATE TABLE refresh_token
(
    user_id    BIGINT       PRIMARY KEY,
    token      VARCHAR(512) NOT NULL,
    expires_at DATETIME(6)  NOT NULL,
    INDEX idx_refresh_token_token (token),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE
);

CREATE TABLE tag
(
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(30) NOT NULL,
    tag_type VARCHAR(20) NOT NULL,
    CONSTRAINT uk_tag_type_name UNIQUE (tag_type, name)
);

CREATE TABLE user_tag
(
    user_id    BIGINT      NOT NULL,
    tag_id     BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id, tag_id),
    FOREIGN KEY (user_id) REFERENCES `user` (id),
    FOREIGN KEY (tag_id) REFERENCES tag (id)
);

CREATE TABLE post
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    author_id        BIGINT       NOT NULL,
    country_id       BIGINT       NOT NULL,
    city_id          BIGINT,
    title            VARCHAR(120) NOT NULL,
    content          TEXT         NOT NULL,
    start_date       DATE,
    end_date         DATE,
    recruit_count    SMALLINT     NOT NULL DEFAULT 1,
    min_age          SMALLINT,
    max_age          SMALLINT,
    gender_condition VARCHAR(10)  NOT NULL DEFAULT 'ANY',
    status           VARCHAR(20)  NOT NULL DEFAULT 'RECRUITING',
    view_count       BIGINT       NOT NULL DEFAULT 0,
    comment_count    BIGINT       NOT NULL DEFAULT 0,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    FOREIGN KEY (author_id) REFERENCES `user` (id),
    FOREIGN KEY (country_id) REFERENCES country (id),
    FOREIGN KEY (city_id) REFERENCES city (id)
);

CREATE TABLE post_image
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT       NOT NULL,
    image_url  VARCHAR(512) NOT NULL,
    order_no   SMALLINT     NOT NULL DEFAULT 0,
    created_at DATETIME(6)  NOT NULL,
    FOREIGN KEY (post_id) REFERENCES post (id)
);

CREATE TABLE post_tag
(
    post_id BIGINT NOT NULL,
    tag_id  BIGINT NOT NULL,
    PRIMARY KEY (post_id, tag_id),
    FOREIGN KEY (post_id) REFERENCES post (id),
    FOREIGN KEY (tag_id) REFERENCES tag (id)
);

CREATE TABLE chat_room
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL
);

CREATE TABLE chat_message
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_room_id BIGINT        NOT NULL,
    sender_id    BIGINT        NOT NULL,
    message      VARCHAR(2000) NOT NULL,
    created_at   DATETIME(6)   NOT NULL,
    FOREIGN KEY (chat_room_id) REFERENCES chat_room (id),
    FOREIGN KEY (sender_id) REFERENCES `user` (id)
);

CREATE TABLE chat_room_member
(
    chat_room_id BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,
    joined_at    DATETIME(6) NOT NULL,
    last_read_at DATETIME(6),
    PRIMARY KEY (chat_room_id, user_id),
    FOREIGN KEY (chat_room_id) REFERENCES chat_room (id),
    FOREIGN KEY (user_id) REFERENCES `user` (id)
);