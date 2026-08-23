package org.sopt.buddys.domain.course.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.course.repository.CourseRepository;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.security.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
class CourseControllerTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private CourseRepository courseRepository;

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

  @DisplayName("코스 게시글을 작성한다")
  @Test
  void createCourse_returnsCreated() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);

    // when, then
    mockMvc.perform(post("/api/v1/courses")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryId": %d,
                  "cityId": %d,
                  "title": "파리 5일 코스",
                  "content": "루브르부터...",
                  "startDate": "2026-09-01",
                  "endDate": "2026-09-05",
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
                """.formatted(countryId, cityId)))
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
                  "countryId": 1,
                  "cityId": 1,
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

    // when, then
    mockMvc.perform(post("/api/v1/courses")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "countryId": %d,
                  "cityId": %d,
                  "title": "파리 코스",
                  "startDate": "2026-09-01",
                  "endDate": "2026-09-05",
                  "days": [
                    { "dayNumber": 1 },
                    { "dayNumber": 1 }
                  ]
                }
                """.formatted(countryId, cityId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("COURSE-E004"));
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

  private void cleanUp() {
    jdbcTemplate.update("DELETE FROM course_flight");
    jdbcTemplate.update("DELETE FROM course_companion");
    jdbcTemplate.update("DELETE FROM course_place");
    jdbcTemplate.update("DELETE FROM course_image");
    jdbcTemplate.update("DELETE FROM course_day");
    jdbcTemplate.update("DELETE FROM course_tag");
    jdbcTemplate.update("DELETE FROM course");
    jdbcTemplate.update("DELETE FROM place_bookmark");
    jdbcTemplate.update("DELETE FROM place");
    jdbcTemplate.update("DELETE FROM tag");
    jdbcTemplate.update("DELETE FROM `user`");
    jdbcTemplate.update("DELETE FROM city");
    jdbcTemplate.update("DELETE FROM country");
  }
}
