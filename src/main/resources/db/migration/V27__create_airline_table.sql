CREATE TABLE airline
(
    id   BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(10)  NOT NULL,
    CONSTRAINT uk_airline_code UNIQUE (code)
);
