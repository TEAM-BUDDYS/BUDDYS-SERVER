package org.sopt.buddys.domain.airline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.airline.entity.Airline;
import org.sopt.buddys.domain.airline.repository.AirlineRepository;
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
class AirlineServiceTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private AirlineService airlineService;

  @Autowired
  private AirlineRepository airlineRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    airlineRepository.deleteAllInBatch();
  }

  @AfterEach
  void tearDown() {
    airlineRepository.deleteAllInBatch();
  }

  @DisplayName("항공사명에 키워드가 부분 일치(대소문자 무시)하면 검색된다")
  @Test
  void searchAirlines_matchesByNamePartially() {
    // given
    insertAirline("대한항공", "KE");
    insertAirline("아시아나항공", "OZ");

    // when
    Slice<Airline> result = airlineService.searchAirlines("대한", 0, 20);

    // then
    assertThat(result.getContent())
        .extracting(Airline::getName)
        .containsExactly("대한항공");
  }

  @DisplayName("항공사 코드에 키워드가 부분 일치(대소문자 무시)하면 검색된다")
  @Test
  void searchAirlines_matchesByCodeIgnoreCase() {
    // given
    insertAirline("대한항공", "KE");
    insertAirline("아시아나항공", "OZ");

    // when
    Slice<Airline> result = airlineService.searchAirlines("ke", 0, 20);

    // then
    assertThat(result.getContent())
        .extracting(Airline::getCode)
        .containsExactly("KE");
  }

  @DisplayName("키워드가 없으면 저장소를 조회하지 않고 빈 결과를 반환한다")
  @Test
  void searchAirlines_nullKeyword_returnsEmpty() {
    // given
    insertAirline("대한항공", "KE");

    // when
    Slice<Airline> result = airlineService.searchAirlines(null, 0, 20);

    // then
    assertThat(result.getContent()).isEmpty();
    assertThat(result.hasNext()).isFalse();
  }

  @DisplayName("키워드가 공백 문자만으로 이루어지면 빈 결과를 반환한다")
  @Test
  void searchAirlines_blankKeyword_returnsEmpty() {
    // given
    insertAirline("대한항공", "KE");

    // when
    Slice<Airline> result = airlineService.searchAirlines("   ", 0, 20);

    // then
    assertThat(result.getContent()).isEmpty();
  }

  @DisplayName("검색 결과는 항공사명 오름차순으로 정렬된다")
  @Test
  void searchAirlines_ordersByNameAsc() {
    // given
    insertAirline("제주항공", "7C");
    insertAirline("대한항공", "KE");
    insertAirline("아시아나항공", "OZ");

    // when
    Slice<Airline> result = airlineService.searchAirlines("항공", 0, 20);

    // then
    assertThat(result.getContent())
        .extracting(Airline::getName)
        .containsExactly("대한항공", "아시아나항공", "제주항공");
  }

  @DisplayName("page가 음수이면 예외가 발생한다")
  @Test
  void searchAirlines_negativePage_throwsException() {
    // when, then
    assertThatThrownBy(() -> airlineService.searchAirlines("대한", -1, 20))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(GlobalErrorCode.INVALID_REQUEST);
  }

  @DisplayName("size가 0 이하이면 예외가 발생한다")
  @Test
  void searchAirlines_zeroSize_throwsException() {
    // when, then
    assertThatThrownBy(() -> airlineService.searchAirlines("대한", 0, 0))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(GlobalErrorCode.INVALID_REQUEST);
  }

  @DisplayName("size가 최대 허용치를 초과하면 예외가 발생한다")
  @Test
  void searchAirlines_sizeExceedsMax_throwsException() {
    // when, then
    assertThatThrownBy(() -> airlineService.searchAirlines("대한", 0, 101))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(GlobalErrorCode.INVALID_REQUEST);
  }

  @DisplayName("size만큼 페이지가 채워지면 hasNext는 true, 마지막 페이지는 false다")
  @Test
  void searchAirlines_hasNextReflectsRemainingPages() {
    // given
    insertAirline("대한항공", "KE");
    insertAirline("아시아나항공", "OZ");
    insertAirline("제주항공", "7C");

    // when
    Slice<Airline> firstPage = airlineService.searchAirlines("항공", 0, 2);
    Slice<Airline> secondPage = airlineService.searchAirlines("항공", 1, 2);

    // then
    assertThat(firstPage.getContent()).hasSize(2);
    assertThat(firstPage.hasNext()).isTrue();
    assertThat(secondPage.getContent()).hasSize(1);
    assertThat(secondPage.hasNext()).isFalse();
  }

  @DisplayName("항공사 국문명에 키워드가 부분 일치(대소문자 무시)하면 검색된다")
  @Test
  void searchAirlines_matchesByKoreanNamePartially() {
    // given
    insertAirline("Korean Air", "대한항공", "KE");
    insertAirline("Asiana Airlines", "아시아나항공", "OZ");

    // when
    Slice<Airline> result = airlineService.searchAirlines("대한", 0, 20);

    // then
    assertThat(result.getContent())
        .extracting(Airline::getName)
        .containsExactly("Korean Air");
  }

  @DisplayName("키워드의 언더스코어(_)는 단일 문자 와일드카드가 아닌 일반 문자로 매칭된다")
  @Test
  void searchAirlines_escapesUnderscore_matchesLiterally() {
    // given
    insertAirline("에어_1", "A1");
    insertAirline("에어21", "A2");

    // when
    Slice<Airline> result = airlineService.searchAirlines("에어_1", 0, 20);

    // then
    assertThat(result.getContent())
        .extracting(Airline::getName)
        .containsExactly("에어_1");
  }

  @DisplayName("키워드의 퍼센트(%)는 임의 문자열 와일드카드가 아닌 일반 문자로 매칭된다")
  @Test
  void searchAirlines_escapesPercent_matchesLiterally() {
    // given
    insertAirline("에어%1", "A1");
    insertAirline("에어아무거나1", "A2");

    // when
    Slice<Airline> result = airlineService.searchAirlines("에어%1", 0, 20);

    // then
    assertThat(result.getContent())
        .extracting(Airline::getName)
        .containsExactly("에어%1");
  }

  @DisplayName("키워드의 백슬래시(\\)는 이스케이프 문자가 아닌 일반 문자로 매칭된다")
  @Test
  void searchAirlines_escapesBackslash_matchesLiterally() {
    // given
    insertAirline("에어\\1", "A1");
    insertAirline("에어1", "A2");

    // when
    Slice<Airline> result = airlineService.searchAirlines("에어\\1", 0, 20);

    // then
    assertThat(result.getContent())
        .extracting(Airline::getName)
        .containsExactly("에어\\1");
  }

  private void insertAirline(String name, String code) {
    insertAirline(name, null, code);
  }

  private void insertAirline(String name, String koreanName, String code) {
    jdbcTemplate.update(connection -> {
          var preparedStatement = connection.prepareStatement(
              "INSERT INTO airline (name, korean_name, code) VALUES (?, ?, ?)",
              Statement.RETURN_GENERATED_KEYS
          );
          preparedStatement.setString(1, name);
          preparedStatement.setString(2, koreanName);
          preparedStatement.setString(3, code);
          return preparedStatement;
        }
    );
  }
}
