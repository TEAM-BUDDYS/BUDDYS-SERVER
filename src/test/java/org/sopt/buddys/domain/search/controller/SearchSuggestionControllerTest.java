package org.sopt.buddys.domain.search.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.repository.CourseRepository;
import org.sopt.buddys.domain.location.entity.City;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.location.repository.CityRepository;
import org.sopt.buddys.domain.location.repository.CountryRepository;
import org.sopt.buddys.domain.place.entity.Place;
import org.sopt.buddys.domain.place.entity.PlaceCategory;
import org.sopt.buddys.domain.place.repository.PlaceRepository;
import org.sopt.buddys.domain.post.entity.CompanionType;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.sopt.buddys.domain.post.entity.RecruitmentCountType;
import org.sopt.buddys.domain.post.repository.PostRepository;
import org.sopt.buddys.domain.user.entity.AccountStatus;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.support.IntegrationTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

class SearchSuggestionControllerTest extends IntegrationTestSupport {

  @Autowired
  private CountryRepository countryRepository;

  @Autowired
  private CityRepository cityRepository;

  @Autowired
  private PlaceRepository placeRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CourseRepository courseRepository;

  @Autowired
  private PostRepository postRepository;

  @DisplayName("국가, 장소, 활성 사용자, 활성 코스와 모집 중 게시글의 이름과 제목만 후보로 반환한다")
  @Test
  void getSuggestions_returnsEligibleCandidatesFromAllSources() throws Exception {
    User viewer = saveUser("viewer@test.com", "viewer", "MatchSelf", AccountStatus.ACTIVE);
    User author = saveUser("author@test.com", "author", "MatchActive", AccountStatus.ACTIVE);
    saveUser("withdrawn@test.com", "withdrawn", "MatchWithdrawn", AccountStatus.WITHDRAWN);
    saveUser("suspended@test.com", "suspended", "MatchSuspended", AccountStatus.SUSPENDED);
    User deletedUser = saveUser("deleted@test.com", "deleted", "MatchDeleted", AccountStatus.ACTIVE);
    jdbcTemplate.update(
        "UPDATE `user` SET deleted_at = ? WHERE id = ?", LocalDateTime.now(), deletedUser.getId());
    Location location = saveLocation("Matchland", "ML", "MatchCity", "매치도시");
    placeRepository.saveAndFlush(Place.builder()
        .googlePlaceId("match-place")
        .name("MatchPlace")
        .category(PlaceCategory.TOURISM)
        .build());
    saveCourse(author, "MatchCourse", "본문");
    saveCourse(author, "본문 전용 코스", "MatchContentOnly");
    Course deletedCourse = saveCourse(author, "MatchDeletedCourse", "본문");
    deletedCourse.delete();
    courseRepository.saveAndFlush(deletedCourse);
    savePost(author, location, "MatchPost", "본문");
    savePost(author, location, "본문 전용 게시글", "MatchPostContentOnly");
    Post completedPost = savePost(author, location, "MatchCompletedPost", "본문");
    completedPost.updateStatus(PostStatus.COMPLETED);
    postRepository.saveAndFlush(completedPost);
    Post deletedPost = savePost(author, location, "MatchDeletedPost", "본문");
    deletedPost.softDelete(LocalDateTime.now());
    postRepository.saveAndFlush(deletedPost);

    mockMvc.perform(suggestionRequest(viewer, "  MATCH  ").param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SEARCH-S002"))
        .andExpect(jsonPath("$.message").value("자동완성 검색어 조회에 성공했습니다."))
        .andExpect(jsonPath("$.data.suggestions[*].keyword")
            .value(org.hamcrest.Matchers.containsInAnyOrder(
                "Matchland", "MatchCity", "MatchPlace", "MatchActive", "MatchCourse", "MatchPost")))
        .andExpect(jsonPath("$.data.suggestions[?(@.keyword == 'Matchland')].type").value("COUNTRY"))
        .andExpect(jsonPath("$.data.suggestions[?(@.keyword == 'MatchCity')].type").value("CITY"))
        .andExpect(jsonPath("$.data.suggestions[?(@.keyword == 'MatchPlace')].type").value("PLACE"))
        .andExpect(jsonPath("$.data.suggestions[?(@.keyword == 'MatchActive')].type").value("USER"))
        .andExpect(jsonPath("$.data.suggestions[?(@.keyword == 'MatchCourse')].type").value("COURSE"))
        .andExpect(jsonPath("$.data.suggestions[?(@.keyword == 'MatchPost')].type").value("POST"));
  }

  @DisplayName("도시는 영문 검색 시 영문명, 한글 검색 시 한글명을 한 번만 반환한다")
  @Test
  void getSuggestions_cityReturnsMatchedDisplayNameWithoutDuplicate() throws Exception {
    User viewer = saveUser("viewer@test.com", "viewer", "조회자", AccountStatus.ACTIVE);
    saveLocation("France", "FR", "Paris", "파리");
    saveLocation("SameLand", "SL", "SameCity", "SameCity");

    mockMvc.perform(suggestionRequest(viewer, "PAR"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.suggestions.length()").value(1))
        .andExpect(jsonPath("$.data.suggestions[0].type").value("CITY"))
        .andExpect(jsonPath("$.data.suggestions[0].keyword").value("Paris"));

    mockMvc.perform(suggestionRequest(viewer, "파"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.suggestions.length()").value(1))
        .andExpect(jsonPath("$.data.suggestions[0].type").value("CITY"))
        .andExpect(jsonPath("$.data.suggestions[0].keyword").value("파리"));

    mockMvc.perform(suggestionRequest(viewer, "samecity"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.suggestions.length()").value(1))
        .andExpect(jsonPath("$.data.suggestions[0].type").value("CITY"))
        .andExpect(jsonPath("$.data.suggestions[0].keyword").value("SameCity"));
  }

  @DisplayName("자동완성은 완전 일치, 접두 일치, 중간 포함 순으로 정렬한다")
  @Test
  void getSuggestions_ordersExactPrefixAndContains() throws Exception {
    User viewer = saveUser("viewer@test.com", "viewer", "조회자", AccountStatus.ACTIVE);
    insertCountry("파리", "P1");
    saveUser("prefix@test.com", "prefix", "파리여행", AccountStatus.ACTIVE);
    saveCourse(viewer, "여름 파리", "본문");

    mockMvc.perform(suggestionRequest(viewer, "파리"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.suggestions[*].keyword")
            .value(org.hamcrest.Matchers.contains("파리", "파리여행", "여름 파리")));
  }

  @DisplayName("같은 type과 keyword는 대소문자와 무관하게 중복 제거하고 다른 type은 유지한다")
  @Test
  void getSuggestions_deduplicatesByTypeAndKeywordIgnoringCase() throws Exception {
    User viewer = saveUser("viewer@test.com", "viewer", "조회자", AccountStatus.ACTIVE);
    placeRepository.saveAndFlush(savePlace("duplicate-place-1", "Shared"));
    placeRepository.saveAndFlush(savePlace("duplicate-place-2", "shared"));
    saveCourse(viewer, "Shared", "본문");

    mockMvc.perform(suggestionRequest(viewer, "shared"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.suggestions.length()").value(2))
        .andExpect(jsonPath("$.data.suggestions[*].type")
            .value(org.hamcrest.Matchers.containsInAnyOrder("PLACE", "COURSE")));
  }

  @DisplayName("퍼센트와 언더스코어는 LIKE wildcard가 아니라 literal로 검색한다")
  @Test
  void getSuggestions_treatsWildcardCharactersAsLiterals() throws Exception {
    User viewer = saveUser("viewer@test.com", "viewer", "조회자", AccountStatus.ACTIVE);
    insertCountry("France", "FR");
    insertCountry("100% Pure", "PP");
    insertCountry("under_score", "US");

    mockMvc.perform(suggestionRequest(viewer, "%"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.suggestions.length()").value(1))
        .andExpect(jsonPath("$.data.suggestions[0].keyword").value("100% Pure"));

    mockMvc.perform(suggestionRequest(viewer, "_"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.suggestions.length()").value(1))
        .andExpect(jsonPath("$.data.suggestions[0].keyword").value("under_score"));
  }

  @DisplayName("기본 size는 8이고 지정 size와 최대 20을 적용한다")
  @Test
  void getSuggestions_appliesDefaultAndRequestedLimits() throws Exception {
    User viewer = saveUser("viewer@test.com", "viewer", "조회자", AccountStatus.ACTIVE);
    for (int index = 0; index < 12; index++) {
      insertCountry("LimitCountry" + index, "%02d".formatted(index));
    }

    mockMvc.perform(suggestionRequest(viewer, "limit"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.suggestions.length()").value(8));

    mockMvc.perform(suggestionRequest(viewer, "limit").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.suggestions.length()").value(1));

    mockMvc.perform(suggestionRequest(viewer, "limit").param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.suggestions.length()").value(12));
  }

  @DisplayName("검색어와 size가 유효하지 않으면 400을 반환한다")
  @Test
  void getSuggestions_invalidRequest_returnsBadRequest() throws Exception {
    User viewer = saveUser("viewer@test.com", "viewer", "조회자", AccountStatus.ACTIVE);

    assertBadRequest(viewer, null, null);
    assertBadRequest(viewer, "", null);
    assertBadRequest(viewer, "   ", null);
    assertBadRequest(viewer, "Paris", "0");
    assertBadRequest(viewer, "Paris", "21");
  }

  @DisplayName("결과가 없으면 빈 목록을 반환하고 미인증 요청은 401이다")
  @Test
  void getSuggestions_emptyAndUnauthenticatedResponses() throws Exception {
    User viewer = saveUser("viewer@test.com", "viewer", "조회자", AccountStatus.ACTIVE);

    mockMvc.perform(suggestionRequest(viewer, "no-result"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.suggestions").isEmpty());

    mockMvc.perform(get("/api/v1/search/suggestions").param("keyword", "Paris"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("GLB-E002"));
  }

  @DisplayName("자동완성 OpenAPI는 필수 검색어, size 범위와 공통 응답을 문서화한다")
  @Test
  void getSuggestions_openApiContract_matchesRequestAndResponse() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath(
            "$.paths['/api/v1/search/suggestions'].get.parameters[?(@.name == 'keyword')].required")
            .value(true))
        .andExpect(jsonPath(
            "$.paths['/api/v1/search/suggestions'].get.parameters[?(@.name == 'size')].schema.default")
            .value(8))
        .andExpect(jsonPath(
            "$.paths['/api/v1/search/suggestions'].get.parameters[?(@.name == 'size')].schema.minimum")
            .value(1))
        .andExpect(jsonPath(
            "$.paths['/api/v1/search/suggestions'].get.parameters[?(@.name == 'size')].schema.maximum")
            .value(20))
        .andExpect(jsonPath("$.paths['/api/v1/search/suggestions'].get.responses['200']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/search/suggestions'].get.responses['400']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/search/suggestions'].get.responses['401']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/search/suggestions'].get.responses['500']").exists());
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder suggestionRequest(
      User viewer,
      String keyword
  ) {
    return get("/api/v1/search/suggestions")
        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
        .param("keyword", keyword);
  }

  private void assertBadRequest(User viewer, String keyword, String size) throws Exception {
    var request = get("/api/v1/search/suggestions")
        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()));
    if (keyword != null) {
      request.param("keyword", keyword);
    }
    if (size != null) {
      request.param("size", size);
    }
    mockMvc.perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("GLB-E001"));
  }

  private User saveUser(
      String email,
      String providerId,
      String nickname,
      AccountStatus accountStatus
  ) {
    return userRepository.saveAndFlush(User.builder()
        .email(email)
        .provider(AuthProvider.KAKAO)
        .providerId(providerId)
        .nickname(nickname)
        .accountStatus(accountStatus)
        .build());
  }

  private Location saveLocation(String countryName, String isoCode, String cityName, String koreanName) {
    Long countryId = insertCountry(countryName, isoCode);
    Long cityId = insertCity(countryId, cityName, koreanName);
    return new Location(
        countryRepository.findById(countryId).orElseThrow(),
        cityRepository.findById(cityId).orElseThrow()
    );
  }

  private Place savePlace(String googlePlaceId, String name) {
    return Place.builder()
        .googlePlaceId(googlePlaceId)
        .name(name)
        .category(PlaceCategory.TOURISM)
        .build();
  }

  private Course saveCourse(User author, String title, String content) {
    return courseRepository.saveAndFlush(new Course(
        author,
        title,
        content,
        null,
        LocalDate.now(),
        LocalDate.now().plusDays(1)
    ));
  }

  private Post savePost(User author, Location location, String title, String content) {
    return postRepository.saveAndFlush(new Post(
        author,
        location.country(),
        location.city(),
        title,
        content,
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(2),
        CompanionType.FULL_TRIP,
        RecruitmentCountType.TWO
    ));
  }

  private Long insertCountry(String name, String isoCode) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      var statement = connection.prepareStatement(
          "INSERT INTO country (name, iso_code) VALUES (?, ?)",
          Statement.RETURN_GENERATED_KEYS
      );
      statement.setString(1, name);
      statement.setString(2, isoCode);
      return statement;
    }, keyHolder);
    return keyHolder.getKey().longValue();
  }

  private Long insertCity(Long countryId, String name, String koreanName) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      var statement = connection.prepareStatement(
          "INSERT INTO city (country_id, name, korean_name, population) VALUES (?, ?, ?, ?)",
          Statement.RETURN_GENERATED_KEYS
      );
      statement.setLong(1, countryId);
      statement.setString(2, name);
      statement.setString(3, koreanName);
      statement.setLong(4, 1_000_000L);
      return statement;
    }, keyHolder);
    return keyHolder.getKey().longValue();
  }

  @Override
  protected void cleanUp() {
    jdbcTemplate.update("DELETE FROM post_comment");
    jdbcTemplate.update("DELETE FROM post_bookmark");
    jdbcTemplate.update("DELETE FROM post_image");
    jdbcTemplate.update("DELETE FROM post_age_condition");
    jdbcTemplate.update("DELETE FROM post_gender_condition");
    jdbcTemplate.update("DELETE FROM post_tag");
    jdbcTemplate.update("DELETE FROM post");
    super.cleanUp();
  }

  private record Location(Country country, City city) {
  }
}
