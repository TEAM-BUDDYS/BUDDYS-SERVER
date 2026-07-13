CREATE TABLE university
(
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    country_id BIGINT       NOT NULL,
    name       VARCHAR(255) NOT NULL,
    domain     VARCHAR(255),
    FOREIGN KEY (country_id) REFERENCES country (id)
);