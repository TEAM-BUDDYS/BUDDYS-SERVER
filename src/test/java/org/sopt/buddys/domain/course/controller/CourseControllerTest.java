package org.sopt.buddys.domain.course.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.course.repository.CourseRepository;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.support.IntegrationTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

class CourseControllerTest extends IntegrationTestSupport {

  @Autowired
  private CourseRepository courseRepository;

  @Autowired
  private UserRepository userRepository;

  @DisplayName("내가 작성한 코스 목록을 프로필에서 조회한다")
  @Test
  void getMyCourses_returnsOk() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");
    createCourseViaApi(author, countryId, cityId, tagId, "파리 코스");

    Long courseId = courseRepository.findAll().get(0).getId();

    // when, then
    mockMvc.perform(get("/api/v1/users/me/courses")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.courses.length()").value(1))
        .andExpect(jsonPath("$.data.courses[0].courseId").value(courseId))
        .andExpect(jsonPath("$.data.courses[0].thumbnailImageUrl")
            .value("https://example.com/day1.jpg"))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(18))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @DisplayName("타 유저가 작성한 코스 목록을 프로필에서 조회한다")
  @Test
  void getUserCourses_returnsOk() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");
    createCourseViaApi(author, countryId, cityId, tagId, "파리 코스");

    Long courseId = courseRepository.findAll().get(0).getId();

    // when, then
    mockMvc.perform(get("/api/v1/users/{userId}/courses", author.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.courses.length()").value(1))
        .andExpect(jsonPath("$.data.courses[0].courseId").value(courseId))
        .andExpect(jsonPath("$.data.courses[0].thumbnailImageUrl")
            .value("https://example.com/day1.jpg"))
        .andExpect(jsonPath("$.data.size").value(18));
  }

  @DisplayName("일차가 역순으로 등록되어도 가장 이른 일차의 첫 사진을 프로필 썸네일로 반환한다")
  @Test
  void getMyCourses_returnsFirstImageOfEarliestDay() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    mockMvc.perform(post("/api/v1/courses")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryIds": [%d],
                  "cityIds": [%d],
                  "title": "파리 코스",
                  "startDate": "2026-09-01",
                  "endDate": "2026-09-05",
                  "tagIds": [%d],
                  "days": [
                    {
                      "dayNumber": 2,
                      "imageUrls": ["https://example.com/day2-first.jpg"]
                    },
                    {
                      "dayNumber": 1,
                      "imageUrls": [
                        "https://example.com/day1-first.jpg",
                        "https://example.com/day1-second.jpg"
                      ]
                    }
                  ]
                }
                """.formatted(countryId, cityId, tagId)))
        .andExpect(status().isCreated());

    // when, then
    mockMvc.perform(get("/api/v1/users/me/courses")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.courses[0].thumbnailImageUrl")
            .value("https://example.com/day1-first.jpg"));
  }

  @DisplayName("코스 목록을 국가로 필터링하여 조회한다")
  @Test
  void getCourses_filteredByCountry_returnsOk() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long franceId = insertCountry("프랑스", "FR");
    Long japanId = insertCountry("일본", "JP");
    Long parisId = insertCity(franceId, "Paris", "파리", 2_000_000L);
    Long tokyoId = insertCity(japanId, "Tokyo", "도쿄", 14_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    createCourseViaApi(author, franceId, parisId, tagId, "파리 코스");
    createCourseViaApi(author, japanId, tokyoId, tagId, "도쿄 코스");

    // when, then
    mockMvc.perform(get("/api/v1/courses")
            .queryParam("countryId", String.valueOf(franceId))
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("COURSE-S005"))
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].title").value("파리 코스"))
        .andExpect(jsonPath("$.data.content[0].countries").value("프랑스"))
        .andExpect(jsonPath("$.data.content[0].cities").value("파리"))
        .andExpect(jsonPath("$.data.content[0].images[0]").value("https://example.com/thumbnail.jpg"))
        .andExpect(jsonPath("$.data.content[0].isBookmarked").value(false))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @DisplayName("코스 목록 조회 시 countryId가 0 이하이면 실패한다")
  @Test
  void getCourses_nonPositiveCountryId_returnsBadRequest() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));

    // when, then
    mockMvc.perform(get("/api/v1/courses")
            .queryParam("countryId", "-1")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E001"));
  }

  @DisplayName("저장한 코스 목록을 조회한다")
  @Test
  void getBookmarkedCourses_returnsOk() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    createCourseViaApi(author, countryId, cityId, tagId, "저장할 코스");
    createCourseViaApi(author, countryId, cityId, tagId, "저장 안 할 코스");
    Long bookmarkedCourseId = courseRepository.findAll().stream()
        .filter(course -> course.getTitle().equals("저장할 코스"))
        .findFirst().orElseThrow().getId();

    mockMvc.perform(post("/api/v1/courses/{courseId}/bookmark", bookmarkedCourseId)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId())))
        .andExpect(status().isOk());

    // when, then
    mockMvc.perform(get("/api/v1/courses/bookmarks")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("COURSE-S006"))
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].courseId").value(bookmarkedCourseId))
        .andExpect(jsonPath("$.data.content[0].isBookmarked").value(true));
  }

  @DisplayName("코스 게시글을 작성한다")
  @Test
  void createCourse_returnsCreated() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    // when, then
    mockMvc.perform(post("/api/v1/courses")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryIds": [%d],
                  "cityIds": [%d],
                  "title": "파리 5일 코스",
                  "content": "루브르부터...",
                  "startDate": "2026-09-01",
                  "endDate": "2026-09-05",
                  "tagIds": [%d],
                  "days": [
                    {
                      "dayNumber": 1,
                      "date": "2026-09-01",
                      "imageUrls": ["https://example.com/a.jpg"],
                      "places": [
                        {
                          "googlePlaceId": "ChIJ-test-place",
                          "name": "루브르 박물관",
                          "category": "TOURISM",
                          "latitude": 48.8606,
                          "longitude": 2.3376,
                          "orderNo": 0,
                          "memo": "예약 필수",
                          "cost": 22000
                        }
                      ]
                    }
                  ],
                  "flights": [
                    {
                      "airline": "대한항공",
                      "flightNumber": "KE901",
                      "departureAirport": "ICN",
                      "departureAt": "2026-09-01T13:00:00",
                      "arrivalAirport": "CDG",
                      "arrivalAt": "2026-09-01T18:30:00"
                    }
                  ]
                }
                """.formatted(countryId, cityId, tagId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("GLB-S002"))
        .andExpect(jsonPath("$.data.courseId").isNumber());

    assertThat(courseRepository.findAll()).hasSize(1);
  }

  @DisplayName("로그인하지 않은 사용자는 코스 게시글을 작성할 수 없다")
  @Test
  void createCourse_unauthenticatedUser_returnsUnauthorized() throws Exception {
    mockMvc.perform(post("/api/v1/courses")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E002"));
  }

  @DisplayName("필수값이 누락되면 실패한다")
  @Test
  void createCourse_missingRequiredField_returnsBadRequest() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));

    // when, then
    mockMvc.perform(post("/api/v1/courses")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryIds": [1],
                  "cityIds": [1],
                  "startDate": "2026-09-01",
                  "endDate": "2026-09-05",
                  "days": []
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E001"));
  }

  @DisplayName("dayNumber가 중복되면 실패한다")
  @Test
  void createCourse_duplicateDayNumber_returnsBadRequest() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    // when, then
    mockMvc.perform(post("/api/v1/courses")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryIds": [%d],
                  "cityIds": [%d],
                  "title": "파리 코스",
                  "startDate": "2026-09-01",
                  "endDate": "2026-09-05",
                  "tagIds": [%d],
                  "days": [
                    { "dayNumber": 1, "imageUrls": ["https://example.com/day1.jpg"] },
                    { "dayNumber": 1, "imageUrls": ["https://example.com/day1.jpg"] }
                  ]
                }
                """.formatted(countryId, cityId, tagId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("COURSE-E004"));
  }

  @DisplayName("일자에 사진이 없으면 실패한다")
  @Test
  void createCourse_dayWithoutImage_returnsBadRequest() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    // when, then
    mockMvc.perform(post("/api/v1/courses")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryIds": [%d],
                  "cityIds": [%d],
                  "title": "파리 코스",
                  "startDate": "2026-09-01",
                  "endDate": "2026-09-05",
                  "tagIds": [%d],
                  "days": [ { "dayNumber": 1 } ]
                }
                """.formatted(countryId, cityId, tagId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E001"));
  }

  @DisplayName("days 목록에 null 요소가 있으면 실패한다")
  @Test
  void createCourse_nullElementInDays_returnsBadRequest() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    // when, then
    mockMvc.perform(post("/api/v1/courses")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryIds": [%d],
                  "cityIds": [%d],
                  "title": "파리 코스",
                  "startDate": "2026-09-01",
                  "endDate": "2026-09-05",
                  "tagIds": [%d],
                  "days": [ null ]
                }
                """.formatted(countryId, cityId, tagId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E001"));
  }

  @DisplayName("flights 목록에 null 요소가 있으면 실패한다")
  @Test
  void createCourse_nullElementInFlights_returnsBadRequest() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    // when, then
    mockMvc.perform(post("/api/v1/courses")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryIds": [%d],
                  "cityIds": [%d],
                  "title": "파리 코스",
                  "startDate": "2026-09-01",
                  "endDate": "2026-09-05",
                  "tagIds": [%d],
                  "days": [ { "dayNumber": 1, "imageUrls": ["https://example.com/day1.jpg"] } ],
                  "flights": [ null ]
                }
                """.formatted(countryId, cityId, tagId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E001"));
  }

  @DisplayName("places 목록에 null 요소가 있으면 실패한다")
  @Test
  void createCourse_nullElementInPlaces_returnsBadRequest() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    // when, then
    mockMvc.perform(post("/api/v1/courses")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryIds": [%d],
                  "cityIds": [%d],
                  "title": "파리 코스",
                  "startDate": "2026-09-01",
                  "endDate": "2026-09-05",
                  "tagIds": [%d],
                  "days": [ { "dayNumber": 1, "imageUrls": ["https://example.com/day1.jpg"], "places": [ null ] } ]
                }
                """.formatted(countryId, cityId, tagId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E001"));
  }

  @DisplayName("작성자가 코스를 수정한다")
  @Test
  void updateCourse_byAuthor_returnsOk() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    mockMvc.perform(post("/api/v1/courses")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryIds": [%d],
                  "cityIds": [%d],
                  "title": "파리 5일 코스",
                  "startDate": "2026-09-01",
                  "endDate": "2026-09-05",
                  "tagIds": [%d],
                  "days": [ { "dayNumber": 1, "imageUrls": ["https://example.com/day1.jpg"] } ]
                }
                """.formatted(countryId, cityId, tagId)))
        .andExpect(status().isCreated());

    Long courseId = courseRepository.findAll().get(0).getId();

    // when, then
    mockMvc.perform(put("/api/v1/courses/{courseId}", courseId)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryIds": [%d],
                  "cityIds": [%d],
                  "title": "수정된 파리 코스",
                  "content": "수정된 소개",
                  "startDate": "2026-10-01",
                  "endDate": "2026-10-03",
                  "tagIds": [%d],
                  "days": [ { "dayNumber": 1, "imageUrls": ["https://example.com/updated-day1.jpg"] } ]
                }
                """.formatted(countryId, cityId, tagId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("COURSE-S004"))
        .andExpect(jsonPath("$.data.courseId").value(courseId));

    mockMvc.perform(get("/api/v1/courses/{courseId}", courseId)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("수정된 파리 코스"))
        .andExpect(jsonPath("$.data.content").value("수정된 소개"));
  }

  @DisplayName("작성자가 아닌 유저가 수정하면 실패한다")
  @Test
  void updateCourse_notAuthor_returnsForbidden() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User other = userRepository.save(createUser("other@test.com", "provider-other", "다른유저"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    mockMvc.perform(post("/api/v1/courses")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryIds": [%d],
                  "cityIds": [%d],
                  "title": "파리 5일 코스",
                  "startDate": "2026-09-01",
                  "endDate": "2026-09-05",
                  "tagIds": [%d],
                  "days": [ { "dayNumber": 1, "imageUrls": ["https://example.com/day1.jpg"] } ]
                }
                """.formatted(countryId, cityId, tagId)))
        .andExpect(status().isCreated());

    Long courseId = courseRepository.findAll().get(0).getId();

    // when, then
    mockMvc.perform(put("/api/v1/courses/{courseId}", courseId)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(other.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryIds": [%d],
                  "cityIds": [%d],
                  "title": "수정된 파리 코스",
                  "startDate": "2026-10-01",
                  "endDate": "2026-10-03",
                  "tagIds": [%d],
                  "days": [ { "dayNumber": 1, "imageUrls": ["https://example.com/updated-day1.jpg"] } ]
                }
                """.formatted(countryId, cityId, tagId)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("GLB-E003"));
  }

  @DisplayName("존재하지 않는 코스를 수정하면 실패한다")
  @Test
  void updateCourse_courseNotFound_returnsNotFound() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    // when, then
    mockMvc.perform(put("/api/v1/courses/{courseId}", 999_999L)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryIds": [%d],
                  "cityIds": [%d],
                  "title": "수정된 파리 코스",
                  "startDate": "2026-10-01",
                  "endDate": "2026-10-03",
                  "tagIds": [%d],
                  "days": [ { "dayNumber": 1, "imageUrls": ["https://example.com/updated-day1.jpg"] } ]
                }
                """.formatted(countryId, cityId, tagId)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COURSE-E005"));
  }

  @DisplayName("코스 상세를 조회한다")
  @Test
  void getCourseDetail_returnsOk() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    mockMvc.perform(post("/api/v1/courses")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryIds": [%d],
                  "cityIds": [%d],
                  "title": "파리 5일 코스",
                  "content": "루브르부터...",
                  "thumbnailImageUrl": "https://example.com/thumbnail.jpg",
                  "startDate": "2026-09-01",
                  "endDate": "2026-09-05",
                  "tagIds": [%d],
                  "days": [
                    { "dayNumber": 1, "date": "2026-09-01", "imageUrls": ["https://example.com/day1.jpg"] }
                  ]
                }
                """.formatted(countryId, cityId, tagId)))
        .andExpect(status().isCreated());

    Long courseId = courseRepository.findAll().get(0).getId();

    // when, then
    mockMvc.perform(get("/api/v1/courses/{courseId}", courseId)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("COURSE-S001"))
        .andExpect(jsonPath("$.data.courseId").value(courseId))
        .andExpect(jsonPath("$.data.title").value("파리 5일 코스"))
        .andExpect(jsonPath("$.data.thumbnailImageUrl").value("https://example.com/thumbnail.jpg"))
        .andExpect(jsonPath("$.data.isMine").value(true))
        .andExpect(jsonPath("$.data.isBookmarked").value(false))
        .andExpect(jsonPath("$.data.commentCount").value(0))
        .andExpect(jsonPath("$.data.bookmarkCount").value(0))
        .andExpect(jsonPath("$.data.countries[0].countryId").value(countryId))
        .andExpect(jsonPath("$.data.cities[0].cityId").value(cityId))
        .andExpect(jsonPath("$.data.days[0].dayNumber").value(1))
        .andExpect(jsonPath("$.data.days[0].imageUrls[0]").value("https://example.com/day1.jpg"));
  }

  @DisplayName("존재하지 않는 코스를 조회하면 실패한다")
  @Test
  void getCourseDetail_courseNotFound_returnsNotFound() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));

    // when, then
    mockMvc.perform(get("/api/v1/courses/{courseId}", 999_999L)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("COURSE-E005"));
  }

  @DisplayName("작성자가 코스를 삭제한다")
  @Test
  void deleteCourse_byAuthor_returnsNoContent() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    mockMvc.perform(post("/api/v1/courses")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryIds": [%d],
                  "cityIds": [%d],
                  "title": "파리 5일 코스",
                  "startDate": "2026-09-01",
                  "endDate": "2026-09-05",
                  "tagIds": [%d],
                  "days": [ { "dayNumber": 1, "imageUrls": ["https://example.com/day1.jpg"] } ]
                }
                """.formatted(countryId, cityId, tagId)))
        .andExpect(status().isCreated());

    Long courseId = courseRepository.findAll().get(0).getId();

    // when, then
    mockMvc.perform(delete("/api/v1/courses/{courseId}", courseId)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("GLB-S001"));

    mockMvc.perform(get("/api/v1/courses/{courseId}", courseId)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COURSE-E005"));
  }

  @DisplayName("작성자가 아닌 유저가 삭제하면 실패한다")
  @Test
  void deleteCourse_notAuthor_returnsForbidden() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User other = userRepository.save(createUser("other@test.com", "provider-other", "다른유저"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    mockMvc.perform(post("/api/v1/courses")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryIds": [%d],
                  "cityIds": [%d],
                  "title": "파리 5일 코스",
                  "startDate": "2026-09-01",
                  "endDate": "2026-09-05",
                  "tagIds": [%d],
                  "days": [ { "dayNumber": 1, "imageUrls": ["https://example.com/day1.jpg"] } ]
                }
                """.formatted(countryId, cityId, tagId)))
        .andExpect(status().isCreated());

    Long courseId = courseRepository.findAll().get(0).getId();

    // when, then
    mockMvc.perform(delete("/api/v1/courses/{courseId}", courseId)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(other.getId())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("GLB-E003"));
  }

  @DisplayName("존재하지 않는 코스를 삭제하면 실패한다")
  @Test
  void deleteCourse_courseNotFound_returnsNotFound() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));

    // when, then
    mockMvc.perform(delete("/api/v1/courses/{courseId}", 999_999L)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COURSE-E005"));
  }

  @DisplayName("코스를 저장하고, DELETE 요청으로 저장을 취소한다")
  @Test
  void bookmarkAndUnbookmarkCourse_returnsOk() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    mockMvc.perform(post("/api/v1/courses")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryIds": [%d],
                  "cityIds": [%d],
                  "title": "파리 5일 코스",
                  "startDate": "2026-09-01",
                  "endDate": "2026-09-05",
                  "tagIds": [%d],
                  "days": [ { "dayNumber": 1, "imageUrls": ["https://example.com/day1.jpg"] } ]
                }
                """.formatted(countryId, cityId, tagId)))
        .andExpect(status().isCreated());

    Long courseId = courseRepository.findAll().get(0).getId();

    // when, then
    mockMvc.perform(post("/api/v1/courses/{courseId}/bookmark", courseId)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("COURSE-S002"))
        .andExpect(jsonPath("$.data.courseId").value(courseId))
        .andExpect(jsonPath("$.data.bookmarked").value(true));

    mockMvc.perform(delete("/api/v1/courses/{courseId}/bookmark", courseId)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("COURSE-S003"))
        .andExpect(jsonPath("$.data.bookmarked").value(false));
  }

  @DisplayName("존재하지 않는 코스를 저장하면 실패한다")
  @Test
  void bookmarkCourse_courseNotFound_returnsNotFound() throws Exception {
    // given
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));

    // when, then
    mockMvc.perform(post("/api/v1/courses/{courseId}/bookmark", 999_999L)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COURSE-E005"));
  }

  @DisplayName("존재하지 않는 코스를 저장 취소하면 실패한다")
  @Test
  void unbookmarkCourse_courseNotFound_returnsNotFound() throws Exception {
    // given
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));

    // when, then
    mockMvc.perform(delete("/api/v1/courses/{courseId}/bookmark", 999_999L)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COURSE-E005"));
  }

  private void createCourseViaApi(User author, Long countryId, Long cityId, Long tagId, String title) throws Exception {
    mockMvc.perform(post("/api/v1/courses")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryIds": [%d],
                  "cityIds": [%d],
                  "title": "%s",
                  "thumbnailImageUrl": "https://example.com/thumbnail.jpg",
                  "startDate": "2026-09-01",
                  "endDate": "2026-09-05",
                  "tagIds": [%d],
                  "days": [ { "dayNumber": 1, "imageUrls": ["https://example.com/day1.jpg"] } ]
                }
                """.formatted(countryId, cityId, title, tagId)))
        .andExpect(status().isCreated());
  }

  private User createUser(String email, String providerId, String nickname) {
    return User.builder()
        .email(email)
        .provider(AuthProvider.KAKAO)
        .providerId(providerId)
        .nickname(nickname)
        .build();
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

  private Long insertCity(Long countryId, String name, String koreanName, Long population) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
          var preparedStatement = connection.prepareStatement(
              "INSERT INTO city (country_id, name, korean_name, population) VALUES (?, ?, ?, ?)",
              Statement.RETURN_GENERATED_KEYS
          );
          preparedStatement.setLong(1, countryId);
          preparedStatement.setString(2, name);
          preparedStatement.setString(3, koreanName);
          preparedStatement.setLong(4, population);
          return preparedStatement;
        },
        keyHolder
    );
    return keyHolder.getKey().longValue();
  }

  private Long insertTag(String name, String tagType) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
          var preparedStatement = connection.prepareStatement(
              "INSERT INTO tag (name, tag_type) VALUES (?, ?)",
              Statement.RETURN_GENERATED_KEYS
          );
          preparedStatement.setString(1, name);
          preparedStatement.setString(2, tagType);
          return preparedStatement;
        },
        keyHolder
    );
    return keyHolder.getKey().longValue();
  }
}
