ALTER TABLE `user`
ADD COLUMN interest_country_id BIGINT,
ADD COLUMN interest_city_id BIGINT,
ADD CONSTRAINT fk_user_interest_country FOREIGN KEY (interest_country_id) REFERENCES country(id),
ADD CONSTRAINT fk_user_interest_city FOREIGN KEY (interest_city_id) REFERENCES city(id);