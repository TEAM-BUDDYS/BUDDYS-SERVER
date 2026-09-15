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
import org.sopt.buddys.domain.magazine.entity.MagazineCategory;
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

  @DisplayName("필수 카테고리만 조회하고 sort 미전달 시 최신순과 ID 내림차순을 적용한다")
  @Test
  void getMagazines_filtersCategoryAndAppliesDefaultLatestSort() throws Exception {
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    LocalDate sameDate = LocalDate.of(2026, 8, 20);
    Magazine older = magazineRepository.save(createMagazine(
        "먼저 발행", "지원 요약", LocalDate.of(2026, 8, 10), MagazineCategory.SUPPORT));
    Magazine sameDateFirst = magazineRepository.save(createMagazine(
        "같은 날 첫 번째", "지원 요약", sameDate, MagazineCategory.SUPPORT));
    Magazine sameDateSecond = magazineRepository.save(createMagazine(
        "같은 날 두 번째", "지원 요약", sameDate, MagazineCategory.SUPPORT));
    magazineRepository.save(createMagazine(
        "여행 매거진", "여행 요약", LocalDate.of(2026, 8, 30), MagazineCategory.TRAVEL));

    mockMvc.perform(magazineListRequest(user).param("category", "SUPPORT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("MAGAZINE-S001"))
        .andExpect(jsonPath("$.message").value("매거진 목록 조회에 성공했습니다."))
        .andExpect(jsonPath("$.data.year").doesNotExist())
        .andExpect(jsonPath("$.data.month").doesNotExist())
        .andExpect(jsonPath("$.data.totalCount").value(3))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(10))
        .andExpect(jsonPath("$.data.hasNext").value(false))
        .andExpect(jsonPath("$.data.magazines[*].magazineId")
            .value(org.hamcrest.Matchers.contains(
                sameDateSecond.getId().intValue(), sameDateFirst.getId().intValue(), older.getId().intValue())));
  }

  @DisplayName("제목과 요약을 부분 검색하고 keyword 공백을 정규화한다")
  @Test
  void getMagazines_searchesTitleAndSummaryWithNormalizedKeyword() throws Exception {
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Magazine titleMatch = magazineRepository.save(createMagazine(
        "EXCHANGE 교환학생 지원 안내", "제목 일치", LocalDate.of(2026, 8, 10), MagazineCategory.SUPPORT));
    Magazine summaryMatch = magazineRepository.save(createMagazine(
        "지원 준비", "교환학생 필수 정보", LocalDate.of(2026, 8, 20), MagazineCategory.SUPPORT));
    magazineRepository.save(createMagazine(
        "불일치", "검색 결과 제외", LocalDate.of(2026, 8, 30), MagazineCategory.SUPPORT));
    magazineRepository.save(createMagazine(
        "교환학생 여행", "다른 카테고리", LocalDate.of(2026, 8, 30), MagazineCategory.TRAVEL));

    mockMvc.perform(magazineListRequest(user)
            .param("category", "SUPPORT")
            .param("keyword", "  교환학생  ")
            .param("sort", "LATEST"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalCount").value(2))
        .andExpect(jsonPath("$.data.magazines[*].magazineId")
            .value(org.hamcrest.Matchers.contains(
                summaryMatch.getId().intValue(), titleMatch.getId().intValue())));

    mockMvc.perform(magazineListRequest(user)
            .param("category", "SUPPORT")
            .param("keyword", "exchange"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalCount").value(1))
        .andExpect(jsonPath("$.data.magazines[0].magazineId").value(titleMatch.getId()));

    for (String keyword : List.of("", "   ")) {
      mockMvc.perform(magazineListRequest(user)
              .param("category", "SUPPORT")
              .param("keyword", keyword))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalCount").value(3));
    }

    mockMvc.perform(magazineListRequest(user)
            .param("category", "SUPPORT")
            .param("keyword", "없는 검색어"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalCount").value(0))
        .andExpect(jsonPath("$.data.magazines").isEmpty());
  }

  @DisplayName("퍼센트, 언더스코어와 느낌표는 LIKE wildcard가 아닌 문자 그대로 검색한다")
  @Test
  void getMagazines_treatsLikeWildcardsAsLiterals() throws Exception {
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    magazineRepository.save(createMagazine(
        "100% 지원", "일반 요약", LocalDate.of(2026, 8, 10), MagazineCategory.SUPPORT));
    magazineRepository.save(createMagazine(
        "under_score", "일반 요약", LocalDate.of(2026, 8, 11), MagazineCategory.SUPPORT));
    magazineRepository.save(createMagazine(
        "주의! 지원", "일반 요약", LocalDate.of(2026, 8, 12), MagazineCategory.SUPPORT));
    magazineRepository.save(createMagazine(
        "일반 지원", "일반 요약", LocalDate.of(2026, 8, 13), MagazineCategory.SUPPORT));

    mockMvc.perform(magazineListRequest(user).param("category", "SUPPORT").param("keyword", "%"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalCount").value(1))
        .andExpect(jsonPath("$.data.magazines[0].title").value("100% 지원"));

    mockMvc.perform(magazineListRequest(user).param("category", "SUPPORT").param("keyword", "_"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalCount").value(1))
        .andExpect(jsonPath("$.data.magazines[0].title").value("under_score"));

    mockMvc.perform(magazineListRequest(user).param("category", "SUPPORT").param("keyword", "!"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalCount").value(1))
        .andExpect(jsonPath("$.data.magazines[0].title").value("주의! 지원"));
  }

  @DisplayName("전체 저장 수와 발행일 및 ID 순으로 정렬하며 저장 0건도 포함한다")
  @Test
  void getMagazines_bookmarkSortUsesGlobalCountAndStableTieBreakers() throws Exception {
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));
    User other1 = userRepository.save(createUser("other1@test.com", "provider-other1", "다른사용자1"));
    User other2 = userRepository.save(createUser("other2@test.com", "provider-other2", "다른사용자2"));
    Magazine mostBookmarked = magazineRepository.save(createMagazine(
        "교환학생 인기", "지원", LocalDate.of(2026, 8, 1), MagazineCategory.SUPPORT));
    Magazine tiedNewer = magazineRepository.save(createMagazine(
        "교환학생 최신", "지원", LocalDate.of(2026, 8, 20), MagazineCategory.SUPPORT));
    Magazine tiedOlder = magazineRepository.save(createMagazine(
        "교환학생 이전", "지원", LocalDate.of(2026, 8, 10), MagazineCategory.SUPPORT));
    LocalDate zeroBookmarkDate = LocalDate.of(2026, 8, 5);
    Magazine zeroFirst = magazineRepository.save(createMagazine(
        "교환학생 저장 없음 1", "지원", zeroBookmarkDate, MagazineCategory.SUPPORT));
    Magazine zeroSecond = magazineRepository.save(createMagazine(
        "교환학생 저장 없음 2", "지원", zeroBookmarkDate, MagazineCategory.SUPPORT));
    magazineRepository.save(createMagazine(
        "교환학생 여행", "여행", LocalDate.of(2026, 9, 1), MagazineCategory.TRAVEL));
    magazineBookmarkRepository.saveAllAndFlush(List.of(
        new MagazineBookmark(viewer, mostBookmarked),
        new MagazineBookmark(other1, mostBookmarked),
        new MagazineBookmark(other1, tiedNewer),
        new MagazineBookmark(other2, tiedOlder)
    ));

    mockMvc.perform(magazineListRequest(viewer)
            .param("category", "SUPPORT")
            .param("keyword", "교환학생")
            .param("sort", "BOOKMARK")
            .param("page", "0")
            .param("size", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalCount").value(5))
        .andExpect(jsonPath("$.data.hasNext").value(true))
        .andExpect(jsonPath("$.data.magazines[*].magazineId")
            .value(org.hamcrest.Matchers.contains(
                mostBookmarked.getId().intValue(), tiedNewer.getId().intValue(), tiedOlder.getId().intValue())))
        .andExpect(jsonPath("$.data.magazines[0].isBookmarked").value(true))
        .andExpect(jsonPath("$.data.magazines[1].isBookmarked").value(false));

    mockMvc.perform(magazineListRequest(viewer)
            .param("category", "SUPPORT")
            .param("keyword", "교환학생")
            .param("sort", "BOOKMARK")
            .param("page", "1")
            .param("size", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalCount").value(5))
        .andExpect(jsonPath("$.data.hasNext").value(false))
        .andExpect(jsonPath("$.data.magazines[*].magazineId")
            .value(org.hamcrest.Matchers.contains(
                zeroSecond.getId().intValue(), zeroFirst.getId().intValue())));
  }

  @DisplayName("필수 카테고리와 enum 및 페이지 요청값이 유효하지 않으면 실패한다")
  @Test
  void getMagazines_invalidRequest_returnsBadRequest() throws Exception {
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));

    for (String[] parameter : List.of(
        new String[]{"category", "INVALID"},
        new String[]{"sort", "INVALID"},
        new String[]{"page", "-1"},
        new String[]{"size", "0"},
        new String[]{"size", "101"}
    )) {
      var request = magazineListRequest(user).param("category", "SUPPORT");
      if (parameter[0].equals("category")) {
        request = magazineListRequest(user);
      }
      mockMvc.perform(request.param(parameter[0], parameter[1]))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("GLB-E001"));
    }

    mockMvc.perform(magazineListRequest(user))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("GLB-E001"));
  }

  @DisplayName("조회 결과가 없으면 빈 목록과 200을 반환한다")
  @Test
  void getMagazines_noResults_returnsEmptyListWithOk() throws Exception {
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));

    mockMvc.perform(magazineListRequest(user).param("category", "SUPPORT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.totalCount").value(0))
        .andExpect(jsonPath("$.data.magazines").isEmpty());
  }

  @DisplayName("로그인하지 않은 사용자는 매거진 목록을 조회할 수 없다")
  @Test
  void getMagazines_unauthenticatedUser_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/magazines"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("GLB-E002"));
  }

  @DisplayName("저장한 매거진만 최신 저장순으로 페이지 조회한다")
  @Test
  void getBookmarkedMagazines_returnsLatestBookmarksForUser() throws Exception {
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));
    User other = userRepository.save(createUser("other@test.com", "provider-other", "다른 사용자"));
    Magazine older = magazineRepository.save(createMagazine("먼저 저장한 매거진", LocalDate.of(2026, 7, 10)));
    Magazine newer = magazineRepository.save(createMagazine("최근 저장한 매거진", LocalDate.of(2026, 8, 10)));
    Magazine notMine = magazineRepository.save(createMagazine("다른 사용자의 매거진", LocalDate.of(2026, 9, 10)));

    magazineBookmarkRepository.saveAndFlush(new MagazineBookmark(viewer, older));
    magazineBookmarkRepository.saveAndFlush(new MagazineBookmark(viewer, newer));
    magazineBookmarkRepository.saveAndFlush(new MagazineBookmark(other, notMine));
    jdbcTemplate.update(
        "UPDATE magazine_bookmark SET created_at = ? WHERE user_id = ? AND magazine_id = ?",
        LocalDateTime.of(2026, 8, 1, 10, 0), viewer.getId(), older.getId()
    );
    jdbcTemplate.update(
        "UPDATE magazine_bookmark SET created_at = ? WHERE user_id = ? AND magazine_id = ?",
        LocalDateTime.of(2026, 8, 2, 10, 0), viewer.getId(), newer.getId()
    );

    mockMvc.perform(get("/api/v1/magazines/bookmarks")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("page", "0")
            .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("MAGAZINE-S004"))
        .andExpect(jsonPath("$.message").value("저장한 매거진 목록 조회에 성공했습니다."))
        .andExpect(jsonPath("$.data.magazines.length()").value(1))
        .andExpect(jsonPath("$.data.magazines[0].magazineId").value(newer.getId()))
        .andExpect(jsonPath("$.data.magazines[0].title").value("최근 저장한 매거진"))
        .andExpect(jsonPath("$.data.magazines[0].summary").value("요약 문구"))
        .andExpect(jsonPath("$.data.magazines[0].thumbnailImageUrl")
            .value("https://example.com/magazines/thumbnail.png"))
        .andExpect(jsonPath("$.data.magazines[0].publishedAt").value("2026-08-10"))
        .andExpect(jsonPath("$.data.magazines[0].externalUrl")
            .value("https://www.instagram.com/p/ABC123/"))
        .andExpect(jsonPath("$.data.magazines[0].isBookmarked").value(true))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(1))
        .andExpect(jsonPath("$.data.hasNext").value(true));

    mockMvc.perform(get("/api/v1/magazines/bookmarks")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("page", "1")
            .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.magazines.length()").value(1))
        .andExpect(jsonPath("$.data.magazines[0].magazineId").value(older.getId()))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @DisplayName("저장한 매거진 목록은 인증과 페이지 범위를 검증한다")
  @Test
  void getBookmarkedMagazines_validatesAuthenticationAndPagination() throws Exception {
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));

    mockMvc.perform(get("/api/v1/magazines/bookmarks"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("GLB-E002"));
    mockMvc.perform(get("/api/v1/magazines/bookmarks")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("GLB-E001"));
    mockMvc.perform(get("/api/v1/magazines/bookmarks")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("GLB-E001"));
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
        .andExpect(jsonPath(
            "$.paths['/api/v1/magazines'].get.parameters[?(@.name == 'category')].required")
            .value(true))
        .andExpect(jsonPath(
            "$.paths['/api/v1/magazines'].get.parameters[?(@.name == 'keyword')].required")
            .value(false))
        .andExpect(jsonPath(
            "$.paths['/api/v1/magazines'].get.parameters[?(@.name == 'sort')].schema.default")
            .value("LATEST"))
        .andExpect(jsonPath(
            "$.paths['/api/v1/magazines'].get.parameters[?(@.name == 'page')].schema.default")
            .value(0))
        .andExpect(jsonPath(
            "$.paths['/api/v1/magazines'].get.parameters[?(@.name == 'size')].schema.default")
            .value(10))
        .andExpect(jsonPath(
            "$.paths['/api/v1/magazines'].get.parameters[?(@.name == 'size')].schema.maximum")
            .value(100))
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
                "totalCount", "page", "size", "hasNext", "magazines")))
        .andExpect(jsonPath("$.components.schemas.MagazineListResponse.properties.year").doesNotExist())
        .andExpect(jsonPath("$.components.schemas.MagazineListResponse.properties.month").doesNotExist())
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
    return createMagazine(title, "요약 문구", publishedAt, MagazineCategory.SUPPORT);
  }

  private Magazine createMagazine(
      String title,
      String summary,
      LocalDate publishedAt,
      MagazineCategory category
  ) {
    return new Magazine(
        title,
        summary,
        "https://example.com/magazines/thumbnail.png",
        "https://www.instagram.com/p/ABC123/",
        publishedAt,
        category
    );
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder magazineListRequest(
      User user
  ) {
    return get("/api/v1/magazines")
        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId()));
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
