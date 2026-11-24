package org.sopt.buddys.domain.place.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.place.entity.Place;
import org.sopt.buddys.domain.place.entity.PlaceCategory;
import org.sopt.buddys.support.IntegrationTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

class PlaceRepositoryPhotoKeywordTest extends IntegrationTestSupport {

  @Autowired
  private PlaceRepository placeRepository;

  @DisplayName("도시가 연결된 장소는 도시 영문명을 검색어로 쓴다")
  @Test
  void findPhotoSearchKeyword_cityLinked_returnsCityEnglishName() {
    Long countryId = insert("INSERT INTO country (name, iso_code, english_name) VALUES (?, ?, ?)", "프랑스", "FR", "France");
    Long cityId = insert("INSERT INTO city (country_id, name, korean_name, population) VALUES (?, ?, ?, ?)",
        countryId, "Paris", "파리", 1_000_000L);
    savePlace("with-city", "파리", "프랑스");
    jdbcTemplate.update("UPDATE place SET city_id = ?, country_id = ? WHERE google_place_id = ?",
        cityId, countryId, "with-city");

    assertThat(placeRepository.findPhotoSearchKeyword("with-city")).contains("Paris");
  }

  @DisplayName("도시가 없으면 국가 영문명, 그것도 없으면 구글 표기 도시명을 쓴다")
  @Test
  void findPhotoSearchKeyword_fallbackOrder() {
    Long countryId = insert("INSERT INTO country (name, iso_code, english_name) VALUES (?, ?, ?)", "스페인", "ES", "Spain");
    savePlace("country-only", "바르셀로나", "스페인");
    jdbcTemplate.update("UPDATE place SET country_id = ? WHERE google_place_id = ?", countryId, "country-only");
    savePlace("names-only", "바르셀로나", "스페인");
    savePlace("empty-city-name", "", "스페인");

    assertThat(placeRepository.findPhotoSearchKeyword("country-only")).contains("Spain");
    assertThat(placeRepository.findPhotoSearchKeyword("names-only")).contains("바르셀로나");
    assertThat(placeRepository.findPhotoSearchKeyword("empty-city-name")).contains("스페인");
  }

  @DisplayName("저장되지 않은 장소나 위치 정보가 전혀 없는 장소는 검색어가 없다")
  @Test
  void findPhotoSearchKeyword_noLocation_returnsEmpty() {
    savePlace("no-location", null, null);

    assertThat(placeRepository.findPhotoSearchKeyword("no-location")).isEmpty();
    assertThat(placeRepository.findPhotoSearchKeyword("unknown")).isEmpty();
  }

  private void savePlace(String googlePlaceId, String cityName, String countryName) {
    placeRepository.save(Place.builder()
        .googlePlaceId(googlePlaceId)
        .name("장소")
        .category(PlaceCategory.TOURISM)
        .cityName(cityName)
        .countryName(countryName)
        .build());
  }

  private Long insert(String sql, Object... params) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      var preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      for (int i = 0; i < params.length; i++) {
        preparedStatement.setObject(i + 1, params[i]);
      }
      return preparedStatement;
    }, keyHolder);
    return keyHolder.getKey().longValue();
  }
}
