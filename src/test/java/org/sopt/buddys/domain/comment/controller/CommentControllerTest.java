package org.sopt.buddys.domain.comment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.comment.entity.Comment;
import org.sopt.buddys.domain.comment.repository.CommentRepository;
import org.sopt.buddys.domain.location.entity.City;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.location.repository.CityRepository;
import org.sopt.buddys.domain.location.repository.CountryRepository;
import org.sopt.buddys.domain.post.entity.CompanionType;
import org.sopt.buddys.domain.post.entity.GenderCondition;
import org.sopt.buddys.domain.post.entity.Post;
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
class CommentControllerTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private CommentRepository commentRepository;

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

  @DisplayName("로그인한 사용자는 게시글에 댓글을 작성할 수 있다")
  @Test
  void createComment_authenticatedUser_savesComment() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Post post = createPost(user);

    // when, then
    mockMvc.perform(post("/api/v1/posts/{postId}/comments", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "content": "저도 같이 가고 싶어요!"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("COMMENT-S001"))
        .andExpect(jsonPath("$.message").value("댓글 작성에 성공했습니다."))
        .andExpect(jsonPath("$.data.commentId").isNumber());

    List<Comment> comments = commentRepository.findAll();
    assertThat(comments).hasSize(1);

    Comment comment = comments.get(0);
    assertThat(comment.getPost().getId()).isEqualTo(post.getId());
    assertThat(comment.getAuthor().getId()).isEqualTo(user.getId());
    assertThat(comment.getContent()).isEqualTo("저도 같이 가고 싶어요!");
    assertThat(comment.getCreatedAt()).isNotNull();
    assertThat(postRepository.findById(post.getId()).orElseThrow().getCommentCount()).isEqualTo(1L);
  }

  @DisplayName("존재하지 않는 게시글에 댓글을 작성하면 예외가 발생한다")
  @Test
  void createComment_postNotFound_returnsNotFound() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));

    // when, then
    mockMvc.perform(post("/api/v1/posts/{postId}/comments", 999L)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "content": "저도 같이 가고 싶어요!"
                }
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("POST-E005"));

    assertThat(commentRepository.findAll()).isEmpty();
  }

  @DisplayName("댓글 내용이 null이면 예외가 발생한다")
  @Test
  void createComment_nullContent_returnsBadRequest() throws Exception {
    assertInvalidContent("""
        {
          "content": null
        }
        """);
  }

  @DisplayName("댓글 내용이 빈 문자열이면 예외가 발생한다")
  @Test
  void createComment_emptyContent_returnsBadRequest() throws Exception {
    assertInvalidContent("""
        {
          "content": ""
        }
        """);
  }

  @DisplayName("댓글 내용이 공백 문자열이면 예외가 발생한다")
  @Test
  void createComment_blankContent_returnsBadRequest() throws Exception {
    assertInvalidContent("""
        {
          "content": "   "
        }
        """);
  }

  @DisplayName("로그인하지 않은 사용자는 댓글을 작성할 수 없다")
  @Test
  void createComment_unauthenticatedUser_returnsUnauthorized() throws Exception {
    mockMvc.perform(post("/api/v1/posts/{postId}/comments", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "content": "저도 같이 가고 싶어요!"
                }
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E002"));
  }

  private void assertInvalidContent(String requestBody) throws Exception {
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Post post = createPost(user);

    mockMvc.perform(post("/api/v1/posts/{postId}/comments", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E001"));

    assertThat(commentRepository.findAll()).isEmpty();
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
        LocalDate.of(2026, 9, 6),
        LocalDate.of(2026, 9, 19),
        GenderCondition.ANY,
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
    jdbcTemplate.update("DELETE FROM post_tag");
    jdbcTemplate.update("DELETE FROM post");
    jdbcTemplate.update("DELETE FROM tag");
    jdbcTemplate.update("DELETE FROM `user`");
    jdbcTemplate.update("DELETE FROM city");
    jdbcTemplate.update("DELETE FROM country");
  }
}
