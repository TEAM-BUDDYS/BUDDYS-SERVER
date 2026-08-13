-- 장소: 구글맵 연동. 검색은 구글 Places API를 실시간 호출한다.
-- 이 테이블은 북마크 또는 코스에서 참조된 장소를 google_place_id 기준으로 1행씩 보관하는 공유 캐시다(재사용). 참조가 모두 해제돼도 행은 삭제하지 않으며, 표시용 최소 스냅샷만 저장한다.
-- 이름/주소/사진 등 상세·최신 정보는 상세 조회 시 구글에서 실시간 보정한다.

CREATE TABLE place
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    google_place_id VARCHAR(255)   NOT NULL,             -- 구글맵 place_id
    name            VARCHAR(255)   NOT NULL,             -- 표시용 스냅샷
    category        VARCHAR(20)    NOT NULL,             -- PlaceCategory: RESTAURANT, CAFE, TOURISM, ACCOMMODATION, ETC
    country_id      BIGINT,
    city_id         BIGINT,
    address         VARCHAR(512),                        -- 표시용 스냅샷
    latitude        DECIMAL(10, 7),
    longitude       DECIMAL(10, 7),
    created_at      DATETIME(6)    NOT NULL,
    CONSTRAINT uk_place_google_place_id UNIQUE (google_place_id),
    CONSTRAINT fk_place_country FOREIGN KEY (country_id) REFERENCES country (id),
    CONSTRAINT fk_place_city    FOREIGN KEY (city_id)    REFERENCES city (id)
);

CREATE INDEX idx_place_city_category ON place (city_id, category);

CREATE TABLE place_bookmark
(
    user_id    BIGINT      NOT NULL,
    place_id   BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id, place_id),
    CONSTRAINT fk_place_bookmark_user  FOREIGN KEY (user_id)  REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_place_bookmark_place FOREIGN KEY (place_id) REFERENCES place (id)  ON DELETE CASCADE
);
CREATE INDEX idx_place_bookmark_user_created ON place_bookmark (user_id, created_at);