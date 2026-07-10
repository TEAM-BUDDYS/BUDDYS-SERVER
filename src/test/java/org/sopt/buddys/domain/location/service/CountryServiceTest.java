package org.sopt.buddys.domain.location.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.location.repository.CityRepository;
import org.sopt.buddys.domain.location.repository.CountryRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Slice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class CountryServiceTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private CountryService countryService;

  @Autowired
  private CountryRepository countryRepository;

  @Autowired
  private CityRepository cityRepository;

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

  @DisplayName("키워드와 부분 일치하는 국가만 대소문자 구분 없이 검색된다")
  @Test
  void searchCountries_partialMatchIgnoreCase() {
    // given
    insertCountry("대한민국", "KR");
    insertCountry("미국", "US");
    insertCountry("Korea Test", "KT");

    // when
    Slice<Country> result = countryService.searchCountries("한", 0, 20);
    Slice<Country> caseInsensitiveResult = countryService.searchCountries("KOREA", 0, 20);

    // then
    assertThat(result.getContent())
        .extracting(Country::getName)
        .containsExactly("대한민국");
    assertThat(caseInsensitiveResult.getContent())
        .extracting(Country::getName)
        .containsExactly("Korea Test");
  }

  @DisplayName("일치하는 국가가 없으면 빈 결과를 반환한다")
  @Test
  void searchCountries_noMatch_returnsEmptySlice() {
    // given
    insertCountry("대한민국", "KR");

    // when
    Slice<Country> result = countryService.searchCountries("존재하지않는국가", 0, 20);

    // then
    assertThat(result.getContent()).isEmpty();
    assertThat(result.hasNext()).isFalse();
  }

  @DisplayName("키워드 앞뒤 공백은 트리밍되어 정상적으로 매칭된다")
  @Test
  void searchCountries_trimsWhitespaceInKeyword() {
    // given
    insertCountry("대한민국", "KR");

    // when
    Slice<Country> result = countryService.searchCountries("  대한민국  ", 0, 20);

    // then
    assertThat(result.getContent())
        .extracting(Country::getName)
        .containsExactly("대한민국");
  }

  @DisplayName("page가 음수이면 예외가 발생한다")
  @Test
  void searchCountries_negativePage_throwsException() {
    // when, then
    assertThatThrownBy(() -> countryService.searchCountries("대한민국", -1, 20))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(GlobalErrorCode.INVALID_REQUEST);
  }

  @DisplayName("size가 0 이하이면 예외가 발생한다")
  @Test
  void searchCountries_zeroSize_throwsException() {
    // when, then
    assertThatThrownBy(() -> countryService.searchCountries("대한민국", 0, 0))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(GlobalErrorCode.INVALID_REQUEST);
  }

  @DisplayName("size가 최대 허용치를 초과하면 예외가 발생한다")
  @Test
  void searchCountries_sizeExceedsMax_throwsException() {
    // when, then
    assertThatThrownBy(() -> countryService.searchCountries("대한민국", 0, 101))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(GlobalErrorCode.INVALID_REQUEST);
  }

  @DisplayName("size만큼 페이지가 채워지면 hasNext는 true, 마지막 페이지는 false다")
  @Test
  void searchCountries_hasNextReflectsRemainingPages() {
    // given
    insertCountry("가나", "GH");
    insertCountry("가봉", "GA");
    insertCountry("가이아나", "GY");

    // when
    Slice<Country> firstPage = countryService.searchCountries("가", 0, 2);
    Slice<Country> secondPage = countryService.searchCountries("가", 1, 2);

    // then
    assertThat(firstPage.getContent()).hasSize(2);
    assertThat(firstPage.hasNext()).isTrue();
    assertThat(secondPage.getContent()).hasSize(1);
    assertThat(secondPage.hasNext()).isFalse();
  }

  private void insertCountry(String name, String isoCode) {
    jdbcTemplate.update("INSERT INTO country (name, iso_code) VALUES (?, ?)", name, isoCode);
  }

  private void cleanUp() {
    cityRepository.deleteAllInBatch();
    countryRepository.deleteAllInBatch();
  }
}
