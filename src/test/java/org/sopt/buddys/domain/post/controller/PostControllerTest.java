package org.sopt.buddys.domain.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Statement;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.location.entity.City;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.location.repository.CityRepository;
import org.sopt.buddys.domain.location.repository.CountryRepository;
import org.sopt.buddys.domain.post.entity.CompanionType;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.sopt.buddys.domain.post.entity.RecruitmentCountType;
import org.sopt.buddys.domain.post.repository.PostRepository;
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
class PostControllerTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private PostRepository postRepository;

  @Autowired
  private UserRepository userRepository;

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

  @DisplayName("동행 게시글 목록을 조회한다")
  @Test
  void getPosts_returnsPostList() throws Exception {
    // given
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);

    // when, then
    mockMvc.perform(get("/api/v1/posts")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("page", "0")
            .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("POST-S003"))
        .andExpect(jsonPath("$.message").value("동행 게시글 목록 조회에 성공했습니다."))
        .andExpect(jsonPath("$.data.content[0].postId").value(post.getId()))
        .andExpect(jsonPath("$.data.content[0].title").value("동행 구해요"))
        .andExpect(jsonPath("$.data.content[0].durationDays").value(4))
        .andExpect(jsonPath("$.data.content[0].recruitmentStatus").value("RECRUITING"))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @DisplayName("로그인하지 않은 사용자는 동행 게시글 목록을 조회할 수 없다")
  @Test
  void getPosts_unauthenticatedUser_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/posts"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E002"));
  }

  @DisplayName("동행 게시글 목록 조회 시 page가 음수이면 실패한다")
  @Test
  void getPosts_negativePage_returnsBadRequest() throws Exception {
    // given
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));

    // when, then
    mockMvc.perform(get("/api/v1/posts")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E001"));
  }

  @DisplayName("동행 게시글 목록 조회 시 size가 0이면 실패한다")
  @Test
  void getPosts_zeroSize_returnsBadRequest() throws Exception {
    // given
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));

    // when, then
    mockMvc.perform(get("/api/v1/posts")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("size", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E001"));
  }

  @DisplayName("동행 게시글 목록 조회 시 쉼표로 구분된 enum 목록 파라미터를 바인딩한다")
  @Test
  void getPosts_commaSeparatedEnumParams_returnsOk() throws Exception {
    // given
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));

    // when, then
    mockMvc.perform(get("/api/v1/posts")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("ageConditions", "EARLY_20S,MID_20S")
            .param("genderConditions", "FEMALE")
            .param("companionTypes", "FULL_TRIP,MEAL")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content").isArray());
  }

  @DisplayName("동행 게시글 목록 조회 시 잘못된 enum 값이면 실패한다")
  @Test
  void getPosts_invalidEnum_returnsBadRequest() throws Exception {
    // given
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));

    // when, then
    mockMvc.perform(get("/api/v1/posts")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("genderConditions", "UNKNOWN"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E001"));
  }

  @DisplayName("동행 게시글 목록 조회 시 잘못된 날짜 형식이면 실패한다")
  @Test
  void getPosts_invalidDateFormat_returnsBadRequest() throws Exception {
    // given
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));

    // when, then
    mockMvc.perform(get("/api/v1/posts")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("startDate", "2026/07/23"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E001"));
  }

  @DisplayName("작성자는 모집 상태를 모집 완료로 변경할 수 있다")
  @Test
  void updatePostStatus_authorUpdatesToCompleted() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);

    // when, then
    mockMvc.perform(patch("/api/v1/posts/{postId}/status", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": "COMPLETED"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("POST-S002"))
        .andExpect(jsonPath("$.message").value("모집 상태 변경에 성공했습니다."))
        .andExpect(jsonPath("$.data.postId").value(post.getId()))
        .andExpect(jsonPath("$.data.status").value("COMPLETED"));

    assertThat(postRepository.findById(post.getId()).orElseThrow().getStatus())
        .isEqualTo(PostStatus.COMPLETED);
  }

  @DisplayName("작성자는 모집 상태를 모집 중으로 변경할 수 있다")
  @Test
  void updatePostStatus_authorUpdatesToRecruiting() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);
    post.updateStatus(PostStatus.COMPLETED);
    postRepository.saveAndFlush(post);

    // when, then
    mockMvc.perform(patch("/api/v1/posts/{postId}/status", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": "RECRUITING"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.postId").value(post.getId()))
        .andExpect(jsonPath("$.data.status").value("RECRUITING"));

    assertThat(postRepository.findById(post.getId()).orElseThrow().getStatus())
        .isEqualTo(PostStatus.RECRUITING);
  }

  @DisplayName("존재하지 않는 게시글 모집 상태 변경 시 실패한다")
  @Test
  void updatePostStatus_postNotFound_returnsNotFound() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));

    // when, then
    mockMvc.perform(patch("/api/v1/posts/{postId}/status", 999L)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": "COMPLETED"
                }
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("POST-E006"));
  }

  @DisplayName("작성자가 아닌 사용자는 모집 상태를 변경할 수 없다")
  @Test
  void updatePostStatus_notAuthor_returnsForbidden() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User otherUser = userRepository.save(createUser("other@test.com", "provider-other", "다른사용자"));
    Post post = createPost(author);

    // when, then
    mockMvc.perform(patch("/api/v1/posts/{postId}/status", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(otherUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": "COMPLETED"
                }
                """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E003"));

    assertThat(postRepository.findById(post.getId()).orElseThrow().getStatus())
        .isEqualTo(PostStatus.RECRUITING);
  }

  @DisplayName("로그인하지 않은 사용자는 모집 상태를 변경할 수 없다")
  @Test
  void updatePostStatus_unauthenticatedUser_returnsUnauthorized() throws Exception {
    mockMvc.perform(patch("/api/v1/posts/{postId}/status", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": "COMPLETED"
                }
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E002"));
  }

  @DisplayName("모집 상태가 null이면 실패한다")
  @Test
  void updatePostStatus_nullStatus_returnsBadRequest() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);

    // when, then
    mockMvc.perform(patch("/api/v1/posts/{postId}/status", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": null
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E001"));
  }

  @DisplayName("모집 상태가 enum에 없는 값이면 실패한다")
  @Test
  void updatePostStatus_invalidStatus_returnsBadRequest() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);

    // when, then
    mockMvc.perform(patch("/api/v1/posts/{postId}/status", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": "CLOSED"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E001"));
  }

  private Post createPost(User author) {
    Long countryId = insertCountry("대한민국", "KR");
    Long cityId = insertCity(countryId, "Seoul", "서울특별시", 10_000_000L);
    Country country = countryRepository.findById(countryId).orElseThrow();
    City city = cityRepository.findById(cityId).orElseThrow();

    return postRepository.save(new Post(
        author,
        country,
        city,
        "동행 구해요",
        "함께 여행하실 분을 구합니다.",
        LocalDate.now().plusDays(10),
        LocalDate.now().plusDays(13),
        CompanionType.FULL_TRIP,
        RecruitmentCountType.TWO
    ));
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
    jdbcTemplate.update("DELETE FROM post_comment");
    jdbcTemplate.update("DELETE FROM post_image");
    jdbcTemplate.update("DELETE FROM post_age_condition");
    jdbcTemplate.update("DELETE FROM post_gender_condition");
    jdbcTemplate.update("DELETE FROM post_tag");
    jdbcTemplate.update("DELETE FROM post");
    jdbcTemplate.update("DELETE FROM tag");
    jdbcTemplate.update("DELETE FROM `user`");
    jdbcTemplate.update("DELETE FROM university");
    jdbcTemplate.update("DELETE FROM city");
    jdbcTemplate.update("DELETE FROM country");
  }
}
