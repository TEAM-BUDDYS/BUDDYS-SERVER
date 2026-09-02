package org.sopt.buddys.domain.search.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.entity.CourseBookmark;
import org.sopt.buddys.domain.course.entity.CourseCity;
import org.sopt.buddys.domain.course.entity.CourseCountry;
import org.sopt.buddys.domain.course.entity.CourseDay;
import org.sopt.buddys.domain.course.entity.CourseImage;
import org.sopt.buddys.domain.course.entity.CoursePlace;
import org.sopt.buddys.domain.course.repository.CourseBookmarkRepository;
import org.sopt.buddys.domain.course.repository.CourseCityRepository;
import org.sopt.buddys.domain.course.repository.CourseCountryRepository;
import org.sopt.buddys.domain.course.repository.CourseDayRepository;
import org.sopt.buddys.domain.course.repository.CourseImageRepository;
import org.sopt.buddys.domain.course.repository.CoursePlaceRepository;
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
import org.sopt.buddys.domain.post.entity.PostImage;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.sopt.buddys.domain.post.entity.RecruitmentCountType;
import org.sopt.buddys.domain.post.repository.PostImageRepository;
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

class SearchControllerTest extends IntegrationTestSupport {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CountryRepository countryRepository;

  @Autowired
  private CityRepository cityRepository;

  @Autowired
  private CourseRepository courseRepository;

  @Autowired
  private CourseCountryRepository courseCountryRepository;

  @Autowired
  private CourseCityRepository courseCityRepository;

  @Autowired
  private CourseDayRepository courseDayRepository;

  @Autowired
  private CourseImageRepository courseImageRepository;

  @Autowired
  private CoursePlaceRepository coursePlaceRepository;

  @Autowired
  private CourseBookmarkRepository courseBookmarkRepository;

  @Autowired
  private PlaceRepository placeRepository;

  @Autowired
  private PostRepository postRepository;

  @Autowired
  private PostImageRepository postImageRepository;

  @DisplayName("코스는 제목, 본문, 장소, 국가, 도시 영문명과 한글명으로 검색되고 중복과 삭제 코스는 제외된다")
  @Test
  void searchCourse_matchesAllFieldsWithoutDuplicateAndDeletedCourse() throws Exception {
    User viewer = saveUser("viewer@test.com", "viewer", "조회자", AccountStatus.ACTIVE);
    User author = saveUser("author@test.com", "author", "작성자", AccountStatus.ACTIVE);
    Location textLocation = saveLocation("France", "FR", "Paris", "파리");
    Course textAndPlaceCourse = saveCourse(
        author, textLocation, "Shared PARIS Art Journey", "A Shared special Story", null);
    savePlace(textAndPlaceCourse, "Shared Match Museum", "place-match");
    Location countryLocation = saveLocation("Matchland", "ML", "CountryCity", "국가도시");
    Course countryCourse = saveCourse(author, countryLocation, "국가 검색 코스", "내용", null);
    Location cityLocation = saveLocation("Cityland", "CL", "MatchCity", "매치시");
    Course cityCourse = saveCourse(author, cityLocation, "도시 검색 코스", "내용", null);
    Course deletedCourse = saveCourse(author, textLocation, "Another Art", "Story", null);
    savePlace(deletedCourse, "Match Museum Annex", "place-deleted");
    deletedCourse.delete();
    courseRepository.saveAndFlush(deletedCourse);

    assertSingleCourse(viewer, "  aRt  ", textAndPlaceCourse.getId());
    assertSingleCourse(viewer, "story", textAndPlaceCourse.getId());
    assertSingleCourse(viewer, "museum", textAndPlaceCourse.getId());
    assertSingleCourse(viewer, "shared", textAndPlaceCourse.getId());
    assertSingleCourse(viewer, "MATCHLAND", countryCourse.getId());
    assertSingleCourse(viewer, "matchcity", cityCourse.getId());
    assertSingleCourse(viewer, "매치시", cityCourse.getId());
  }

  @DisplayName("코스 검색은 같은 page와 size를 적용하고 다음 페이지 여부를 반환한다")
  @Test
  void searchCourse_appliesPagination() throws Exception {
    User viewer = saveUser("viewer@test.com", "viewer", "조회자", AccountStatus.ACTIVE);
    User author = saveUser("author@test.com", "author", "작성자", AccountStatus.ACTIVE);
    Location location = saveLocation("France", "FR", "Paris", "파리");
    for (int index = 0; index < 6; index++) {
      saveCourse(author, location, "Page course " + index, "content", null);
    }

    mockMvc.perform(get("/api/v1/search")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("keyword", "page")
            .param("page", "0")
            .param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.courses.content.length()").value(5))
        .andExpect(jsonPath("$.data.courses.page").value(0))
        .andExpect(jsonPath("$.data.courses.size").value(5))
        .andExpect(jsonPath("$.data.courses.hasNext").value(true));
  }

  @DisplayName("사용자 검색은 닉네임 부분 일치 시 본인과 비활성 및 삭제 사용자를 제외한다")
  @Test
  void searchUser_returnsOnlyActiveOtherUsers() throws Exception {
    User viewer = saveUser("viewer@test.com", "viewer", "TravelSelf", AccountStatus.ACTIVE);
    User first = saveUser("first@test.com", "first", "TravelBuddy", AccountStatus.ACTIVE);
    User second = saveUser("second@test.com", "second", "TRAVELMate", AccountStatus.ACTIVE);
    saveUser("withdrawn@test.com", "withdrawn", "TravelWithdrawn", AccountStatus.WITHDRAWN);
    saveUser("suspended@test.com", "suspended", "TravelSuspended", AccountStatus.SUSPENDED);
    User deleted = saveUser("deleted@test.com", "deleted", "TravelDeleted", AccountStatus.ACTIVE);
    jdbcTemplate.update("UPDATE `user` SET deleted_at = ? WHERE id = ?", LocalDateTime.now(), deleted.getId());

    mockMvc.perform(get("/api/v1/search")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("keyword", "  travel  "))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.users.content.length()").value(2))
        .andExpect(jsonPath("$.data.users.content[*].userId")
            .value(org.hamcrest.Matchers.containsInAnyOrder(first.getId().intValue(), second.getId().intValue())))
        .andExpect(jsonPath("$.data.users.hasNext").value(false));

    mockMvc.perform(get("/api/v1/search")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("keyword", "travel")
            .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.users.content.length()").value(1))
        .andExpect(jsonPath("$.data.users.hasNext").value(true));
  }

  @DisplayName("게시글은 기존 제목과 본문 검색을 재사용하고 모집 완료 및 삭제 게시글을 제외한다")
  @Test
  void searchPost_reusesExistingRecruitingPostSearch() throws Exception {
    User viewer = saveUser("viewer@test.com", "viewer", "조회자", AccountStatus.ACTIVE);
    User author = saveUser("author@test.com", "author", "작성자", AccountStatus.ACTIVE);
    Location location = saveLocation("France", "FR", "Paris", "파리");
    Post titleMatch = savePost(author, location, "PARIS companion", "본문");
    Post contentMatch = savePost(author, location, "동행 구해요", "Paris 여행 본문");
    Post completed = savePost(author, location, "Paris 완료", "본문");
    completed.updateStatus(PostStatus.COMPLETED);
    postRepository.saveAndFlush(completed);
    Post deleted = savePost(author, location, "Paris 삭제", "본문");
    deleted.softDelete(LocalDateTime.now());
    postRepository.saveAndFlush(deleted);

    assertPostIds("/api/v1/search", viewer, titleMatch.getId(), contentMatch.getId());

    mockMvc.perform(get("/api/v1/posts")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("keyword", "  paris  "))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(2))
        .andExpect(jsonPath("$.data.content[*].postId")
            .value(org.hamcrest.Matchers.containsInAnyOrder(
                titleMatch.getId().intValue(), contentMatch.getId().intValue())));
  }

  @DisplayName("통합 검색은 코스, 사용자, 게시글 카드와 기본 pagination을 한 응답에 반환한다")
  @Test
  void search_returnsIntegratedResponseWithDefaultPagination() throws Exception {
    User viewer = saveUser("viewer@test.com", "viewer", "조회자", AccountStatus.ACTIVE);
    User author = saveUser("author@test.com", "author", "ParisUser", AccountStatus.ACTIVE);
    Location location = saveLocation("France", "FR", "Paris", "파리");
    Course course = saveCourse(
        author, location, "Paris course", "course content", "https://example.com/thumb.jpg");
    CourseDay day = courseDayRepository.saveAndFlush(new CourseDay(course, (short) 1, LocalDate.now()));
    courseImageRepository.saveAndFlush(new CourseImage(day, "https://example.com/day.jpg", (short) 0));
    courseBookmarkRepository.saveAndFlush(new CourseBookmark(viewer, course));
    Post post = savePost(author, location, "Paris post", "post content");
    postImageRepository.saveAndFlush(new PostImage(post, "https://example.com/post.jpg", (short) 0));

    mockMvc.perform(get("/api/v1/search")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("keyword", "Paris"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("SEARCH-S001"))
        .andExpect(jsonPath("$.message").value("검색에 성공했습니다."))
        .andExpect(jsonPath("$.data.courses.content[0].courseId").value(course.getId()))
        .andExpect(jsonPath("$.data.courses.content[0].isBookmarked").value(true))
        .andExpect(jsonPath("$.data.courses.content[0].images.length()").value(2))
        .andExpect(jsonPath("$.data.courses.content[0].countries").value("France"))
        .andExpect(jsonPath("$.data.courses.content[0].cities").value("파리"))
        .andExpect(jsonPath("$.data.users.content[0].userId").value(author.getId()))
        .andExpect(jsonPath("$.data.users.content[0].profileImageUrl").value((Object) null))
        .andExpect(jsonPath("$.data.posts.content[0].postId").value(post.getId()))
        .andExpect(jsonPath("$.data.posts.content[0].thumbnailImageUrl")
            .value("https://example.com/post.jpg"))
        .andExpect(jsonPath("$.data.courses.page").value(0))
        .andExpect(jsonPath("$.data.users.size").value(5))
        .andExpect(jsonPath("$.data.posts.size").value(5));
  }

  @DisplayName("검색 결과가 없으면 모든 영역에 빈 배열을 반환한다")
  @Test
  void search_noResults_returnsEmptyContentForAllSections() throws Exception {
    User viewer = saveUser("viewer@test.com", "viewer", "조회자", AccountStatus.ACTIVE);

    mockMvc.perform(get("/api/v1/search")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("keyword", "no-result"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.courses.content").isEmpty())
        .andExpect(jsonPath("$.data.users.content").isEmpty())
        .andExpect(jsonPath("$.data.posts.content").isEmpty())
        .andExpect(jsonPath("$.data.courses.hasNext").value(false))
        .andExpect(jsonPath("$.data.users.hasNext").value(false))
        .andExpect(jsonPath("$.data.posts.hasNext").value(false));
  }

  @DisplayName("검색 요청값이 유효하지 않으면 400을 반환한다")
  @Test
  void search_invalidRequest_returnsBadRequest() throws Exception {
    User viewer = saveUser("viewer@test.com", "viewer", "조회자", AccountStatus.ACTIVE);

    assertBadRequest(viewer, null, null, null);
    assertBadRequest(viewer, "", null, null);
    assertBadRequest(viewer, "   ", null, null);
    assertBadRequest(viewer, "Paris", "-1", null);
    assertBadRequest(viewer, "Paris", null, "0");
  }

  @DisplayName("인증되지 않은 사용자는 통합 검색을 사용할 수 없다")
  @Test
  void search_unauthenticated_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/search").param("keyword", "Paris"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("GLB-E002"));
  }

  @DisplayName("통합 검색 OpenAPI는 필수 검색어와 기본 pagination 및 공통 응답을 문서화한다")
  @Test
  void search_openApiContract_matchesRequestAndResponse() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/search'].get.parameters[?(@.name == 'keyword')].required")
            .value(true))
        .andExpect(jsonPath("$.paths['/api/v1/search'].get.parameters[?(@.name == 'page')].schema.default")
            .value(0))
        .andExpect(jsonPath("$.paths['/api/v1/search'].get.parameters[?(@.name == 'size')].schema.default")
            .value(5))
        .andExpect(jsonPath("$.paths['/api/v1/search'].get.responses['200']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/search'].get.responses['400']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/search'].get.responses['401']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/search'].get.responses['500']").exists());
  }

  private void assertSingleCourse(User viewer, String keyword, Long courseId) throws Exception {
    mockMvc.perform(get("/api/v1/search")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("keyword", keyword))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.courses.content.length()").value(1))
        .andExpect(jsonPath("$.data.courses.content[0].courseId").value(courseId));
  }

  private void assertPostIds(String path, User viewer, Long firstId, Long secondId) throws Exception {
    mockMvc.perform(get(path)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("keyword", "  paris  "))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.posts.content.length()").value(2))
        .andExpect(jsonPath("$.data.posts.content[*].postId")
            .value(org.hamcrest.Matchers.containsInAnyOrder(firstId.intValue(), secondId.intValue())));
  }

  private void assertBadRequest(User viewer, String keyword, String page, String size) throws Exception {
    var request = get("/api/v1/search")
        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()));
    if (keyword != null) {
      request.param("keyword", keyword);
    }
    if (page != null) {
      request.param("page", page);
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

  private Course saveCourse(
      User author,
      Location location,
      String title,
      String content,
      String thumbnailImageUrl
  ) {
    Course course = courseRepository.saveAndFlush(new Course(
        author,
        title,
        content,
        thumbnailImageUrl,
        LocalDate.now(),
        LocalDate.now().plusDays(1)
    ));
    courseCountryRepository.saveAndFlush(new CourseCountry(course, location.country()));
    courseCityRepository.saveAndFlush(new CourseCity(course, location.city()));
    return course;
  }

  private void savePlace(Course course, String name, String googlePlaceId) {
    CourseDay day = courseDayRepository.saveAndFlush(new CourseDay(course, (short) 1, LocalDate.now()));
    Place place = placeRepository.saveAndFlush(Place.builder()
        .googlePlaceId(googlePlaceId)
        .name(name)
        .category(PlaceCategory.TOURISM)
        .build());
    coursePlaceRepository.saveAndFlush(new CoursePlace(day, place, (short) 0, null, null));
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
