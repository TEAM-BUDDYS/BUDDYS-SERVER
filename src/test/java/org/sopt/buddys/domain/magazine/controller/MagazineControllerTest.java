package org.sopt.buddys.domain.magazine.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.magazine.entity.Magazine;
import org.sopt.buddys.domain.magazine.entity.MagazineBookmark;
import org.sopt.buddys.domain.magazine.repository.MagazineBookmarkRepository;
import org.sopt.buddys.domain.magazine.repository.MagazineRepository;
import org.sopt.buddys.domain.magazine.service.MagazineService;
import org.sopt.buddys.domain.magazine.service.result.MagazineBookmarkResult;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.security.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
class MagazineControllerTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private MagazineRepository magazineRepository;

  @Autowired
  private MagazineBookmarkRepository magazineBookmarkRepository;

  @Autowired
  private MagazineService magazineService;

  @Autowired
  private UserRepository userRepository;

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

  @DisplayName("지정한 연월의 매거진 목록을 발행일과 ID 내림차순으로 조회한다")
  @Test
  void getMagazines_specifiedYearMonth_returnsSortedList() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Magazine older = magazineRepository.save(createMagazine("먼저 발행", LocalDate.of(2026, 8, 10)));
    Magazine newer = magazineRepository.save(createMagazine("나중에 발행", LocalDate.of(2026, 8, 20)));
    Magazine otherMonth = magazineRepository.save(createMagazine("다른 달", LocalDate.of(2026, 7, 20)));

    // when, then
    mockMvc.perform(get("/api/v1/magazines")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId()))
            .param("year", "2026")
            .param("month", "8"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("MAGAZINE-S001"))
        .andExpect(jsonPath("$.message").value("매거진 목록 조회에 성공했습니다."))
        .andExpect(jsonPath("$.data.year").value(2026))
        .andExpect(jsonPath("$.data.month").value(8))
        .andExpect(jsonPath("$.data.totalCount").value(2))
        .andExpect(jsonPath("$.data.magazines.length()").value(2))
        .andExpect(jsonPath("$.data.magazines[0].magazineId").value(newer.getId()))
        .andExpect(jsonPath("$.data.magazines[1].magazineId").value(older.getId()));

    assertThat(otherMonth.getId()).isNotNull();
  }

  @DisplayName("동일 발행일이면 매거진 ID 내림차순으로 정렬한다")
  @Test
  void getMagazines_samePublishedDate_sortsByIdDescending() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    LocalDate sameDate = LocalDate.of(2026, 8, 15);
    Magazine first = magazineRepository.save(createMagazine("첫번째", sameDate));
    Magazine second = magazineRepository.save(createMagazine("두번째", sameDate));

    // when, then
    mockMvc.perform(get("/api/v1/magazines")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId()))
            .param("year", "2026")
            .param("month", "8"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.magazines[0].magazineId").value(second.getId()))
        .andExpect(jsonPath("$.data.magazines[1].magazineId").value(first.getId()));
  }

  @DisplayName("연도와 월을 생략하면 Asia/Seoul 기준 현재 연월을 적용한다")
  @Test
  void getMagazines_omittedYearMonth_appliesCurrentSeoulYearMonth() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    Magazine magazine = magazineRepository.save(createMagazine("이번달 매거진", today));

    // when, then
    mockMvc.perform(get("/api/v1/magazines")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.year").value(today.getYear()))
        .andExpect(jsonPath("$.data.month").value(today.getMonthValue()))
        .andExpect(jsonPath("$.data.magazines[0].magazineId").value(magazine.getId()));
  }

  @DisplayName("연도와 월 중 하나만 전달하면 실패한다")
  @Test
  void getMagazines_onlyYearOrMonth_returnsBadRequest() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));

    // when, then
    mockMvc.perform(get("/api/v1/magazines")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId()))
            .param("year", "2026"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("GLB-E001"));

    mockMvc.perform(get("/api/v1/magazines")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId()))
            .param("month", "8"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("GLB-E001"));
  }

  @DisplayName("유효하지 않은 연도 또는 월이면 실패한다")
  @Test
  void getMagazines_invalidYearOrMonth_returnsBadRequest() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));

    for (String[] params : List.of(
        new String[]{"year", "0"},
        new String[]{"year", "-1"},
        new String[]{"year", "999"},
        new String[]{"year", "10000"},
        new String[]{"month", "0"},
        new String[]{"month", "13"}
    )) {
      String otherKey = params[0].equals("year") ? "month" : "year";
      String otherValue = params[0].equals("year") ? "8" : "2026";

      mockMvc.perform(get("/api/v1/magazines")
              .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId()))
              .param(params[0], params[1])
              .param(otherKey, otherValue))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("GLB-E001"));
    }
  }

  @DisplayName("페이지가 음수이거나 크기가 0 이하 또는 100 초과이면 실패한다")
  @Test
  void getMagazines_invalidPageOrSize_returnsBadRequest() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));

    mockMvc.perform(get("/api/v1/magazines")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId()))
            .param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("GLB-E001"));

    mockMvc.perform(get("/api/v1/magazines")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId()))
            .param("size", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("GLB-E001"));

    mockMvc.perform(get("/api/v1/magazines")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId()))
            .param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("GLB-E001"));
  }

  @DisplayName("페이지네이션 정보와 hasNext, totalCount를 반환한다")
  @Test
  void getMagazines_pagination_returnsPageInfo() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    for (int day = 1; day <= 3; day++) {
      magazineRepository.save(createMagazine("매거진 " + day, LocalDate.of(2026, 8, day)));
    }

    // when, then
    mockMvc.perform(get("/api/v1/magazines")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId()))
            .param("year", "2026")
            .param("month", "8")
            .param("page", "0")
            .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalCount").value(3))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(2))
        .andExpect(jsonPath("$.data.hasNext").value(true))
        .andExpect(jsonPath("$.data.magazines.length()").value(2));

    mockMvc.perform(get("/api/v1/magazines")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId()))
            .param("year", "2026")
            .param("month", "8")
            .param("page", "1")
            .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.hasNext").value(false))
        .andExpect(jsonPath("$.data.magazines.length()").value(1));
  }

  @DisplayName("조회 결과가 없으면 빈 목록과 200을 반환한다")
  @Test
  void getMagazines_noResults_returnsEmptyListWithOk() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));

    // when, then
    mockMvc.perform(get("/api/v1/magazines")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId()))
            .param("year", "2026")
            .param("month", "8"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.totalCount").value(0))
        .andExpect(jsonPath("$.data.magazines").isArray())
        .andExpect(jsonPath("$.data.magazines.length()").value(0));
  }

  @DisplayName("로그인 사용자별로 매거진 저장 여부를 반환한다")
  @Test
  void getMagazines_returnsIsBookmarkedPerUser() throws Exception {
    // given
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));
    User otherUser = userRepository.save(createUser("other@test.com", "provider-other", "다른사용자"));
    Magazine bookmarkedMagazine = magazineRepository.save(createMagazine("저장한 매거진", LocalDate.of(2026, 8, 10)));
    Magazine notBookmarkedMagazine = magazineRepository.save(createMagazine("저장 안한 매거진", LocalDate.of(2026, 8, 11)));
    magazineBookmarkRepository.saveAndFlush(new MagazineBookmark(viewer, bookmarkedMagazine));
    magazineBookmarkRepository.saveAndFlush(new MagazineBookmark(otherUser, notBookmarkedMagazine));

    // when, then
    mockMvc.perform(get("/api/v1/magazines")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("year", "2026")
            .param("month", "8"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.magazines[0].magazineId").value(notBookmarkedMagazine.getId()))
        .andExpect(jsonPath("$.data.magazines[0].isBookmarked").value(false))
        .andExpect(jsonPath("$.data.magazines[1].magazineId").value(bookmarkedMagazine.getId()))
        .andExpect(jsonPath("$.data.magazines[1].isBookmarked").value(true));
  }

  @DisplayName("로그인하지 않은 사용자는 매거진 목록을 조회할 수 없다")
  @Test
  void getMagazines_unauthenticatedUser_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/magazines"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("GLB-E002"));
  }

  @DisplayName("매거진을 반복 저장해도 북마크 한 건만 생성된다")
  @Test
  void bookmarkMagazine_repeatedRequests_areIdempotent() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Magazine magazine = magazineRepository.save(createMagazine("매거진", LocalDate.of(2026, 8, 10)));
    LocalDateTime firstCreatedAt = null;

    for (int requestCount = 0; requestCount < 2; requestCount++) {
      mockMvc.perform(post("/api/v1/magazines/{magazineId}/bookmarks", magazine.getId())
              .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.code").value("MAGAZINE-S002"))
          .andExpect(jsonPath("$.message").value("매거진 저장에 성공했습니다."))
          .andExpect(jsonPath("$.data.magazineId").value(magazine.getId()))
          .andExpect(jsonPath("$.data.isBookmarked").value(true));

      LocalDateTime createdAt = jdbcTemplate.queryForObject(
          "SELECT created_at FROM magazine_bookmark WHERE user_id = ? AND magazine_id = ?",
          LocalDateTime.class,
          user.getId(),
          magazine.getId()
      );
      if (firstCreatedAt == null) {
        firstCreatedAt = createdAt;
      } else {
        assertThat(createdAt).isEqualTo(firstCreatedAt);
      }
    }

    assertThat(magazineBookmarkRepository.count()).isOne();
  }

  @DisplayName("동시에 같은 매거진을 최초 저장해도 두 요청이 성공하고 북마크는 한 건만 생성된다")
  @Test
  void bookmarkMagazine_concurrentFirstRequests_areIdempotent() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Magazine magazine = magazineRepository.save(createMagazine("매거진", LocalDate.of(2026, 8, 10)));
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    CountDownLatch readyLatch = new CountDownLatch(2);
    CountDownLatch startLatch = new CountDownLatch(1);

    try {
      Future<MagazineBookmarkResult> first = executorService.submit(
          () -> bookmarkMagazineAfterSignal(user.getId(), magazine.getId(), readyLatch, startLatch));
      Future<MagazineBookmarkResult> second = executorService.submit(
          () -> bookmarkMagazineAfterSignal(user.getId(), magazine.getId(), readyLatch, startLatch));

      assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
      startLatch.countDown();

      assertThat(first.get(10, TimeUnit.SECONDS).isBookmarked()).isTrue();
      assertThat(second.get(10, TimeUnit.SECONDS).isBookmarked()).isTrue();
      assertThat(magazineBookmarkRepository.count()).isOne();
    } finally {
      executorService.shutdownNow();
      assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  @DisplayName("매거진 저장을 반복 취소해도 성공하고 북마크가 남지 않는다")
  @Test
  void removeMagazineBookmark_repeatedRequests_areIdempotent() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Magazine magazine = magazineRepository.save(createMagazine("매거진", LocalDate.of(2026, 8, 10)));
    magazineBookmarkRepository.saveAndFlush(new MagazineBookmark(user, magazine));

    for (int requestCount = 0; requestCount < 2; requestCount++) {
      mockMvc.perform(delete("/api/v1/magazines/{magazineId}/bookmarks", magazine.getId())
              .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.code").value("MAGAZINE-S003"))
          .andExpect(jsonPath("$.message").value("매거진 저장 취소에 성공했습니다."))
          .andExpect(jsonPath("$.data.magazineId").value(magazine.getId()))
          .andExpect(jsonPath("$.data.isBookmarked").value(false));
    }

    assertThat(magazineBookmarkRepository.count()).isZero();
  }

  @DisplayName("저장하지 않은 매거진의 저장 취소도 성공한다")
  @Test
  void removeMagazineBookmark_notBookmarked_succeeds() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Magazine magazine = magazineRepository.save(createMagazine("매거진", LocalDate.of(2026, 8, 10)));

    // when, then
    mockMvc.perform(delete("/api/v1/magazines/{magazineId}/bookmarks", magazine.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("MAGAZINE-S003"))
        .andExpect(jsonPath("$.data.isBookmarked").value(false));
  }

  @DisplayName("매거진 저장과 저장 취소는 매거진 존재 여부, magazineId와 인증을 검증한다")
  @Test
  void magazineBookmark_invalidRequests_returnExpectedErrors() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Magazine magazine = magazineRepository.save(createMagazine("매거진", LocalDate.of(2026, 8, 10)));

    for (String method : List.of("POST", "DELETE")) {
      var missingMagazineRequest = method.equals("POST")
          ? post("/api/v1/magazines/{magazineId}/bookmarks", 99999L)
          : delete("/api/v1/magazines/{magazineId}/bookmarks", 99999L);
      mockMvc.perform(missingMagazineRequest.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("MAGAZINE-E001"));

      for (long invalidMagazineId : List.of(0L, -1L)) {
        var invalidIdRequest = method.equals("POST")
            ? post("/api/v1/magazines/{magazineId}/bookmarks", invalidMagazineId)
            : delete("/api/v1/magazines/{magazineId}/bookmarks", invalidMagazineId);
        mockMvc.perform(invalidIdRequest.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("GLB-E001"));
      }

      var unauthenticatedRequest = method.equals("POST")
          ? post("/api/v1/magazines/{magazineId}/bookmarks", magazine.getId())
          : delete("/api/v1/magazines/{magazineId}/bookmarks", magazine.getId());
      mockMvc.perform(unauthenticatedRequest)
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.code").value("GLB-E002"));
    }
  }

  @DisplayName("동일한 사용자와 매거진 조합은 DB 유니크 제약조건으로 중복 저장될 수 없다")
  @Test
  void magazineBookmark_duplicateUserAndMagazine_violatesUniqueConstraint() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Magazine magazine = magazineRepository.save(createMagazine("매거진", LocalDate.of(2026, 8, 10)));
    magazineBookmarkRepository.saveAndFlush(new MagazineBookmark(user, magazine));

    // when, then
    assertThatThrownBy(() ->
        magazineBookmarkRepository.saveAndFlush(new MagazineBookmark(user, magazine)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @DisplayName("매거진 API OpenAPI 스키마는 필수 응답 필드를 optional로 노출하지 않는다")
  @Test
  void magazine_openApiSchema_matchesContract() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/magazines/{magazineId}/bookmarks'].post.parameters[0].required")
            .value(true))
        .andExpect(jsonPath("$.paths['/api/v1/magazines/{magazineId}/bookmarks'].post.requestBody").doesNotExist())
        .andExpect(jsonPath("$.paths['/api/v1/magazines/{magazineId}/bookmarks'].delete.parameters[0].required")
            .value(true))
        .andExpect(jsonPath("$.paths['/api/v1/magazines/{magazineId}/bookmarks'].delete.requestBody").doesNotExist())
        .andExpect(jsonPath("$.components.schemas.MagazineListSuccessResponse.required")
            .value(org.hamcrest.Matchers.containsInAnyOrder("success", "code", "message", "data")))
        .andExpect(jsonPath("$.components.schemas.MagazineListResponse.required")
            .value(org.hamcrest.Matchers.containsInAnyOrder(
                "year", "month", "totalCount", "page", "size", "hasNext", "magazines")))
        .andExpect(jsonPath("$.components.schemas.MagazineSummaryResponse.required")
            .value(org.hamcrest.Matchers.containsInAnyOrder(
                "magazineId", "title", "summary", "thumbnailImageUrl", "publishedAt", "externalUrl", "isBookmarked")))
        .andExpect(jsonPath("$.components.schemas.MagazineBookmarkSuccessResponse.required")
            .value(org.hamcrest.Matchers.containsInAnyOrder("success", "code", "message", "data")))
        .andExpect(jsonPath("$.components.schemas.MagazineBookmarkSuccessResponse.properties.code.example")
            .value("MAGAZINE-S002"))
        .andExpect(jsonPath("$.components.schemas.DeleteMagazineBookmarkSuccessResponse.required")
            .value(org.hamcrest.Matchers.containsInAnyOrder("success", "code", "message", "data")))
        .andExpect(jsonPath("$.components.schemas.DeleteMagazineBookmarkSuccessResponse.properties.code.example")
            .value("MAGAZINE-S003"))
        .andExpect(jsonPath("$.components.schemas.MagazineBookmarkResponse.required")
            .value(org.hamcrest.Matchers.containsInAnyOrder("magazineId", "isBookmarked")));
  }

  private MagazineBookmarkResult bookmarkMagazineAfterSignal(
      Long userId,
      Long magazineId,
      CountDownLatch readyLatch,
      CountDownLatch startLatch
  ) throws InterruptedException {
    readyLatch.countDown();
    if (!startLatch.await(5, TimeUnit.SECONDS)) {
      throw new IllegalStateException("동시 저장 시작 신호를 기다리는 중 시간 초과");
    }
    return magazineService.bookmarkMagazine(userId, magazineId);
  }

  private Magazine createMagazine(String title, LocalDate publishedAt) {
    return new Magazine(
        title,
        "요약 문구",
        "https://example.com/magazines/thumbnail.png",
        "https://www.instagram.com/p/ABC123/",
        publishedAt
    );
  }

  private User createUser(String email, String providerId, String nickname) {
    return User.builder()
        .email(email)
        .provider(AuthProvider.KAKAO)
        .providerId(providerId)
        .nickname(nickname)
        .build();
  }

  private String bearerToken(Long userId) {
    return "Bearer " + jwtProvider.generateToken(userId);
  }

  private void cleanUp() {
    jdbcTemplate.update("DELETE FROM magazine_bookmark");
    jdbcTemplate.update("DELETE FROM magazine");
    jdbcTemplate.update("DELETE FROM `user`");
  }
}
