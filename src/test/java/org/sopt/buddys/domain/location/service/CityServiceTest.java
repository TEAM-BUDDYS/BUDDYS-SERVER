package org.sopt.buddys.domain.location.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.location.code.LocationErrorCode;
import org.sopt.buddys.domain.location.entity.City;
import org.sopt.buddys.domain.location.repository.CityRepository;
import org.sopt.buddys.domain.location.repository.CountryRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Slice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class CityServiceTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private CityService cityService;

  @Autowired
  private CityRepository cityRepository;

  @Autowired
  private CountryRepository countryRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    cleanUp();
  }

  @AfterEach
  void tearDown() {
    cleanUp();
  }

  @DisplayName("영문 키워드와 부분 일치하는 도시만 검색된다")
  @Test
  void searchCities_partialMatchByName() {
    // given
    Long koreaId = insertCountry("대한민국", "KR");
    insertCity(koreaId, "Seoul", "서울특별시", 10_000_000L);
    insertCity(koreaId, "Suwon-si", "수원시", 1_200_000L);

    // when
    Slice<City> result = cityService.searchCities(koreaId, "Seo", 0, 20);

    // then
    assertThat(result.getContent())
        .extracting(City::getName)
        .containsExactly("Seoul");
  }

  @DisplayName("한글 키워드는 koreanName 기준으로 검색된다")
  @Test
  void searchCities_partialMatchByKoreanName() {
    // given
    Long koreaId = insertCountry("대한민국", "KR");
    insertCity(koreaId, "Seoul", "서울특별시", 10_000_000L);
    insertCity(koreaId, "Busan", "부산광역시", 3_000_000L);

    // when
    Slice<City> result = cityService.searchCities(koreaId, "서울", 0, 20);

    // then
    assertThat(result.getContent())
        .extracting(City::getName)
        .containsExactly("Seoul");
  }

  @DisplayName("koreanName이 없는 도시는 한글 키워드 검색에서 제외된다")
  @Test
  void searchCities_koreanNameNull_excludedFromKoreanSearch() {
    // given
    Long koreaId = insertCountry("대한민국", "KR");
    insertCity(koreaId, "Sejong-si", null, 300_000L);

    // when
    Slice<City> result = cityService.searchCities(koreaId, "세종", 0, 20);

    // then
    assertThat(result.getContent()).isEmpty();
  }

  @DisplayName("다른 국가에 있는 동명 도시는 결과에 섞이지 않는다")
  @Test
  void searchCities_scopedByCountryId() {
    // given
    Long koreaId = insertCountry("대한민국", "KR");
    Long usId = insertCountry("미국", "US");
    insertCity(koreaId, "Seoul", "서울특별시", 10_000_000L);
    insertCity(usId, "Seoul", null, 1_000L);

    // when
    Slice<City> result = cityService.searchCities(koreaId, "Seoul", 0, 20);

    // then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getCountry().getId()).isEqualTo(koreaId);
  }

  @DisplayName("검색 결과는 인구순으로 내림차순 정렬된다")
  @Test
  void searchCities_ordersByPopulationDesc() {
    // given
    Long koreaId = insertCountry("대한민국", "KR");
    insertCity(koreaId, "Suwon-si", "수원시", 1_200_000L);
    insertCity(koreaId, "Seoul", "서울특별시", 10_000_000L);
    insertCity(koreaId, "Sejong-si", "세종특별자치시", 300_000L);

    // when
    Slice<City> result = cityService.searchCities(koreaId, "s", 0, 20);

    // then
    assertThat(result.getContent())
        .extracting(City::getName)
        .containsExactly("Seoul", "Suwon-si", "Sejong-si");
  }

  @DisplayName("존재하지 않는 국가로 조회하면 예외가 발생한다")
  @Test
  void searchCities_countryNotFound_throwsException() {
    // when, then
    assertThatThrownBy(() -> cityService.searchCities(999_999L, "Seoul", 0, 20))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(LocationErrorCode.COUNTRY_NOT_FOUND);
  }

  @DisplayName("page가 음수이면 예외가 발생한다")
  @Test
  void searchCities_negativePage_throwsException() {
    // given
    Long koreaId = insertCountry("대한민국", "KR");

    // when, then
    assertThatThrownBy(() -> cityService.searchCities(koreaId, "Seoul", -1, 20))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(GlobalErrorCode.INVALID_REQUEST);
  }

  @DisplayName("size가 0 이하이면 예외가 발생한다")
  @Test
  void searchCities_zeroSize_throwsException() {
    // given
    Long koreaId = insertCountry("대한민국", "KR");

    // when, then
    assertThatThrownBy(() -> cityService.searchCities(koreaId, "Seoul", 0, 0))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(GlobalErrorCode.INVALID_REQUEST);
  }

  @DisplayName("size가 최대 허용치를 초과하면 예외가 발생한다")
  @Test
  void searchCities_sizeExceedsMax_throwsException() {
    // given
    Long koreaId = insertCountry("대한민국", "KR");

    // when, then
    assertThatThrownBy(() -> cityService.searchCities(koreaId, "Seoul", 0, 101))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(GlobalErrorCode.INVALID_REQUEST);
  }

  @DisplayName("size만큼 페이지가 채워지면 hasNext는 true, 마지막 페이지는 false다")
  @Test
  void searchCities_hasNextReflectsRemainingPages() {
    // given
    Long koreaId = insertCountry("대한민국", "KR");
    insertCity(koreaId, "Seoul", "서울특별시", 10_000_000L);
    insertCity(koreaId, "Suwon-si", "수원시", 1_200_000L);
    insertCity(koreaId, "Sejong-si", "세종특별자치시", 300_000L);

    // when
    Slice<City> firstPage = cityService.searchCities(koreaId, "s", 0, 2);
    Slice<City> secondPage = cityService.searchCities(koreaId, "s", 1, 2);

    // then
    assertThat(firstPage.getContent()).hasSize(2);
    assertThat(firstPage.hasNext()).isTrue();
    assertThat(secondPage.getContent()).hasSize(1);
    assertThat(secondPage.hasNext()).isFalse();
  }

  private Long insertCountry(String name, String isoCode) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
          var preparedStatement = connection.prepareStatement(
              "INSERT INTO country (name, iso_code) VALUES (?, ?)",
              Statement.RETURN_GENERATED_KEYS
          );
          preparedStatement.setString(1, name);
          preparedStatement.setString(2, isoCode);
          return preparedStatement;
        },
        keyHolder
    );
    return keyHolder.getKey().longValue();
  }

  private void insertCity(Long countryId, String name, String koreanName, Long population) {
    jdbcTemplate.update(
        "INSERT INTO city (country_id, name, korean_name, population) VALUES (?, ?, ?, ?)",
        countryId, name, koreanName, population
    );
  }

  private void cleanUp() {
    cityRepository.deleteAllInBatch();
    countryRepository.deleteAllInBatch();
  }
}