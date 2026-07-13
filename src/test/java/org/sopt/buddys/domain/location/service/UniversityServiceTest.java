package org.sopt.buddys.domain.location.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.location.code.LocationErrorCode;
import org.sopt.buddys.domain.location.entity.University;
import org.sopt.buddys.domain.location.repository.CityRepository;
import org.sopt.buddys.domain.location.repository.CountryRepository;
import org.sopt.buddys.domain.location.repository.UniversityRepository;
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
class UniversityServiceTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private UniversityService universityService;

  @Autowired
  private UniversityRepository universityRepository;

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

  @DisplayName("키워드와 부분 일치하는 대학교만 대소문자 구분 없이 검색된다")
  @Test
  void searchUniversities_partialMatchIgnoreCase() {
    // given
    Long koreaId = insertCountry("대한민국", "KR");
    insertUniversity(koreaId, "Yonsei University", "yonsei.ac.kr");
    insertUniversity(koreaId, "Korea University", "korea.ac.kr");

    // when
    Slice<University> result = universityService.searchUniversities(koreaId, "yonsei", 0, 20);

    // then
    assertThat(result.getContent())
        .extracting(University::getName)
        .containsExactly("Yonsei University");
  }

  @DisplayName("키워드가 없으면 빈 결과를 반환한다")
  @Test
  void searchUniversities_nullKeyword_returnsEmpty() {
    // given
    Long koreaId = insertCountry("대한민국", "KR");
    insertUniversity(koreaId, "Yonsei University", "yonsei.ac.kr");
    insertUniversity(koreaId, "Korea University", "korea.ac.kr");

    // when
    Slice<University> result = universityService.searchUniversities(koreaId, null, 0, 20);

    // then
    assertThat(result.getContent()).isEmpty();
    assertThat(result.hasNext()).isFalse();
  }

  @DisplayName("키워드가 공백 문자만으로 이루어지면 빈 결과를 반환한다")
  @Test
  void searchUniversities_blankKeyword_returnsEmpty() {
    // given
    Long koreaId = insertCountry("대한민국", "KR");
    insertUniversity(koreaId, "Yonsei University", "yonsei.ac.kr");

    // when
    Slice<University> result = universityService.searchUniversities(koreaId, "   ", 0, 20);

    // then
    assertThat(result.getContent()).isEmpty();
  }

  @DisplayName("다른 국가에 있는 동명 대학교는 결과에 섞이지 않는다")
  @Test
  void searchUniversities_scopedByCountryId() {
    // given
    Long koreaId = insertCountry("대한민국", "KR");
    Long usId = insertCountry("미국", "US");
    insertUniversity(koreaId, "Some University", "some.ac.kr");
    insertUniversity(usId, "Some University", "some.edu");

    // when
    Slice<University> result = universityService.searchUniversities(koreaId, "Some", 0, 20);

    // then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getCountry().getId()).isEqualTo(koreaId);
  }

  @DisplayName("검색 결과는 이름순으로 오름차순 정렬된다")
  @Test
  void searchUniversities_ordersByNameAsc() {
    // given
    Long koreaId = insertCountry("대한민국", "KR");
    insertUniversity(koreaId, "Yonsei University", "yonsei.ac.kr");
    insertUniversity(koreaId, "Ajou University", "ajou.ac.kr");
    insertUniversity(koreaId, "Korea University", "korea.ac.kr");

    // when
    Slice<University> result = universityService.searchUniversities(koreaId, "University", 0, 20);

    // then
    assertThat(result.getContent())
        .extracting(University::getName)
        .containsExactly("Ajou University", "Korea University", "Yonsei University");
  }

  @DisplayName("존재하지 않는 국가로 조회하면 예외가 발생한다")
  @Test
  void searchUniversities_countryNotFound_throwsException() {
    // when, then
    assertThatThrownBy(() -> universityService.searchUniversities(999_999L, "Yonsei", 0, 20))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(LocationErrorCode.COUNTRY_NOT_FOUND);
  }

  @DisplayName("page가 음수이면 예외가 발생한다")
  @Test
  void searchUniversities_negativePage_throwsException() {
    // given
    Long koreaId = insertCountry("대한민국", "KR");

    // when, then
    assertThatThrownBy(() -> universityService.searchUniversities(koreaId, "Yonsei", -1, 20))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(GlobalErrorCode.INVALID_REQUEST);
  }

  @DisplayName("size가 0 이하이면 예외가 발생한다")
  @Test
  void searchUniversities_zeroSize_throwsException() {
    // given
    Long koreaId = insertCountry("대한민국", "KR");

    // when, then
    assertThatThrownBy(() -> universityService.searchUniversities(koreaId, "Yonsei", 0, 0))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(GlobalErrorCode.INVALID_REQUEST);
  }

  @DisplayName("size가 최대 허용치를 초과하면 예외가 발생한다")
  @Test
  void searchUniversities_sizeExceedsMax_throwsException() {
    // given
    Long koreaId = insertCountry("대한민국", "KR");

    // when, then
    assertThatThrownBy(() -> universityService.searchUniversities(koreaId, "Yonsei", 0, 101))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(GlobalErrorCode.INVALID_REQUEST);
  }

  @DisplayName("size만큼 페이지가 채워지면 hasNext는 true, 마지막 페이지는 false다")
  @Test
  void searchUniversities_hasNextReflectsRemainingPages() {
    // given
    Long koreaId = insertCountry("대한민국", "KR");
    insertUniversity(koreaId, "Ajou University", "ajou.ac.kr");
    insertUniversity(koreaId, "Korea University", "korea.ac.kr");
    insertUniversity(koreaId, "Yonsei University", "yonsei.ac.kr");

    // when
    Slice<University> firstPage = universityService.searchUniversities(koreaId, "University", 0, 2);
    Slice<University> secondPage = universityService.searchUniversities(koreaId, "University", 1, 2);

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

  private void insertUniversity(Long countryId, String name, String domain) {
    jdbcTemplate.update(
        "INSERT INTO university (country_id, name, domain) VALUES (?, ?, ?)",
        countryId, name, domain
    );
  }

  private void cleanUp() {
    universityRepository.deleteAllInBatch();
    cityRepository.deleteAllInBatch();
    countryRepository.deleteAllInBatch();
  }
}