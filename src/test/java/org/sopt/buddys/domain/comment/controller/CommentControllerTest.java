package org.sopt.buddys.domain.comment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

  @DisplayName("댓글 목록을 작성 시간 오름차순으로 조회할 수 있다")
  @Test
  void getComments_returnsCommentsOrderByCreatedAtAsc() throws Exception {
    // given
    User postAuthor = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User commentAuthor = userRepository.save(createUser(
        "commenter@test.com",
        "provider-commenter",
        "댓글작성자",
        "https://example.com/commenter.png"
    ));
    Post post = createPost(postAuthor);

    Comment recentComment = saveComment(post, commentAuthor, "최신 댓글", LocalDateTime.now().minusMinutes(3));
    Comment oldComment = saveComment(post, commentAuthor, "오래된 댓글", LocalDateTime.now().minusHours(2));

    // when, then
    mockMvc.perform(get("/api/v1/posts/{postId}/comments", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(commentAuthor.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("COMMENT-S002"))
        .andExpect(jsonPath("$.message").value("댓글 목록 조회에 성공했습니다."))
        .andExpect(jsonPath("$.data.comments").isArray())
        .andExpect(jsonPath("$.data.comments.length()").value(2))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.hasNext").value(false))
        .andExpect(jsonPath("$.data.comments[0].commentId").value(oldComment.getId()))
        .andExpect(jsonPath("$.data.comments[0].writerId").value(commentAuthor.getId()))
        .andExpect(jsonPath("$.data.comments[0].writerName").value("댓글작성자"))
        .andExpect(jsonPath("$.data.comments[0].writerProfileImageUrl")
            .value("https://example.com/commenter.png"))
        .andExpect(jsonPath("$.data.comments[0].content").value("오래된 댓글"))
        .andExpect(jsonPath("$.data.comments[0].createdAt").exists())
        .andExpect(jsonPath("$.data.comments[0].timeAgo").value("2시간 전"))
        .andExpect(jsonPath("$.data.comments[1].commentId").value(recentComment.getId()))
        .andExpect(jsonPath("$.data.comments[1].writerId").value(commentAuthor.getId()))
        .andExpect(jsonPath("$.data.comments[1].writerName").value("댓글작성자"))
        .andExpect(jsonPath("$.data.comments[1].writerProfileImageUrl")
            .value("https://example.com/commenter.png"))
        .andExpect(jsonPath("$.data.comments[1].content").value("최신 댓글"))
        .andExpect(jsonPath("$.data.comments[1].createdAt").exists())
        .andExpect(jsonPath("$.data.comments[1].timeAgo").value("3분 전"));
  }

  @DisplayName("댓글 목록은 페이지와 크기에 맞게 무한스크롤용 페이지네이션 정보를 반환한다")
  @Test
  void getComments_returnsPaginationForInfiniteScroll() throws Exception {
    // given
    User postAuthor = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User commentAuthor = userRepository.save(createUser("commenter@test.com", "provider-commenter", "댓글작성자"));
    Post post = createPost(postAuthor);

    saveComment(post, commentAuthor, "첫 번째 댓글", LocalDateTime.now().minusHours(3));
    saveComment(post, commentAuthor, "두 번째 댓글", LocalDateTime.now().minusHours(2));
    saveComment(post, commentAuthor, "세 번째 댓글", LocalDateTime.now().minusHours(1));

    // when, then
    mockMvc.perform(get("/api/v1/posts/{postId}/comments", post.getId())
            .queryParam("page", "0")
            .queryParam("size", "2")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(commentAuthor.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.comments.length()").value(2))
        .andExpect(jsonPath("$.data.comments[0].content").value("첫 번째 댓글"))
        .andExpect(jsonPath("$.data.comments[1].content").value("두 번째 댓글"))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(2))
        .andExpect(jsonPath("$.data.hasNext").value(true));

    mockMvc.perform(get("/api/v1/posts/{postId}/comments", post.getId())
            .queryParam("page", "1")
            .queryParam("size", "2")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(commentAuthor.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.comments.length()").value(1))
        .andExpect(jsonPath("$.data.comments[0].content").value("세 번째 댓글"))
        .andExpect(jsonPath("$.data.page").value(1))
        .andExpect(jsonPath("$.data.size").value(2))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @DisplayName("작성 시간이 같은 댓글이 여러 개여도 페이지 경계에서 중복이나 누락 없이 안정적으로 정렬된다")
  @Test
  void getComments_sameCreatedAt_stablePaginationById() throws Exception {
    // given
    User postAuthor = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User commentAuthor = userRepository.save(createUser("commenter@test.com", "provider-commenter", "댓글작성자"));
    Post post = createPost(postAuthor);

    LocalDateTime sameCreatedAt = LocalDateTime.now().minusHours(1);
    Comment first = saveComment(post, commentAuthor, "댓글1", sameCreatedAt);
    Comment second = saveComment(post, commentAuthor, "댓글2", sameCreatedAt);
    Comment third = saveComment(post, commentAuthor, "댓글3", sameCreatedAt);

    // when, then
    mockMvc.perform(get("/api/v1/posts/{postId}/comments", post.getId())
            .queryParam("page", "0")
            .queryParam("size", "2")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(commentAuthor.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.comments[0].commentId").value(first.getId()))
        .andExpect(jsonPath("$.data.comments[1].commentId").value(second.getId()))
        .andExpect(jsonPath("$.data.hasNext").value(true));

    mockMvc.perform(get("/api/v1/posts/{postId}/comments", post.getId())
            .queryParam("page", "1")
            .queryParam("size", "2")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(commentAuthor.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.comments.length()").value(1))
        .andExpect(jsonPath("$.data.comments[0].commentId").value(third.getId()))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @DisplayName("댓글 목록 조회 시 30일 이상은 월 단위, 1년 이상은 년 단위로 상대 시간을 반환한다")
  @Test
  void getComments_returnsMonthAndYearTimeAgo() throws Exception {
    // given
    User postAuthor = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User commentAuthor = userRepository.save(createUser("commenter@test.com", "provider-commenter", "댓글작성자"));
    Post post = createPost(postAuthor);

    saveComment(post, commentAuthor, "월 단위 댓글", LocalDateTime.now().minusDays(60));
    saveComment(post, commentAuthor, "364일 경계 댓글", LocalDateTime.now().minusDays(364));
    saveComment(post, commentAuthor, "년 단위 댓글", LocalDateTime.now().minusDays(365));

    // when, then
    mockMvc.perform(get("/api/v1/posts/{postId}/comments", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(commentAuthor.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.comments[0].content").value("년 단위 댓글"))
        .andExpect(jsonPath("$.data.comments[0].timeAgo").value("1년 전"))
        .andExpect(jsonPath("$.data.comments[1].content").value("364일 경계 댓글"))
        .andExpect(jsonPath("$.data.comments[1].timeAgo").value("11개월 전"))
        .andExpect(jsonPath("$.data.comments[2].content").value("월 단위 댓글"))
        .andExpect(jsonPath("$.data.comments[2].timeAgo").value("2개월 전"));
  }

  @DisplayName("탈퇴한 유저가 작성한 댓글은 목록 조회 시 닉네임과 프로필 이미지가 가려진다")
  @Test
  void getComments_withdrawnCommentAuthor_masksNicknameAndProfileImage() throws Exception {
    // given
    User postAuthor = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User commentAuthor = userRepository.save(createUser(
        "commenter@test.com", "provider-commenter", "댓글작성자", "https://example.com/commenter.png"));
    Post post = createPost(postAuthor);
    Comment comment = saveComment(post, commentAuthor, "댓글", LocalDateTime.now().minusMinutes(1));
    jdbcTemplate.update("UPDATE `user` SET deleted_at = ? WHERE id = ?", LocalDateTime.now(), commentAuthor.getId());

    // when, then
    mockMvc.perform(get("/api/v1/posts/{postId}/comments", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(postAuthor.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.comments[0].commentId").value(comment.getId()))
        .andExpect(jsonPath("$.data.comments[0].writerName").value("탈퇴한 사용자"))
        .andExpect(jsonPath("$.data.comments[0].writerProfileImageUrl").doesNotExist());
  }

  @DisplayName("댓글이 없는 게시글은 빈 댓글 배열을 반환한다")
  @Test
  void getComments_emptyPost_returnsEmptyArray() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Post post = createPost(user);

    // when, then
    mockMvc.perform(get("/api/v1/posts/{postId}/comments", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.comments").isArray())
        .andExpect(jsonPath("$.data.comments.length()").value(0))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @DisplayName("존재하지 않는 게시글의 댓글 목록을 조회하면 예외가 발생한다")
  @Test
  void getComments_postNotFound_returnsNotFound() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));

    // when, then
    mockMvc.perform(get("/api/v1/posts/{postId}/comments", 999L)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("POST-E006"));
  }

  @DisplayName("댓글 목록 조회 페이지 요청값이 잘못되면 예외가 발생한다")
  @Test
  void getComments_invalidPageRequest_returnsBadRequest() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Post post = createPost(user);

    // when, then
    mockMvc.perform(get("/api/v1/posts/{postId}/comments", post.getId())
            .queryParam("page", "-1")
            .queryParam("size", "20")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E001"));
  }

  @DisplayName("로그인하지 않은 사용자는 댓글 목록을 조회할 수 없다")
  @Test
  void getComments_unauthenticatedUser_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/posts/{postId}/comments", 1L))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E002"));
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
        .andExpect(jsonPath("$.code").value("POST-E006"));

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

  @DisplayName("댓글 내용이 100자를 초과하면 예외가 발생한다")
  @Test
  void createComment_contentLongerThan100_returnsBadRequest() throws Exception {
    assertInvalidContent("""
        {
          "content": "%s"
        }
        """.formatted("a".repeat(101)));
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
        LocalDate.now().plusDays(10),
        LocalDate.now().plusDays(13),
        CompanionType.FULL_TRIP,
        RecruitmentCountType.TWO
    ));
  }

  private User createUser(String email, String providerId, String nickname) {
    return createUser(email, providerId, nickname, null);
  }

  private User createUser(
      String email,
      String providerId,
      String nickname,
      String profileImageUrl
  ) {
    return User.builder()
        .email(email)
        .provider(AuthProvider.KAKAO)
        .providerId(providerId)
        .nickname(nickname)
        .profileImageUrl(profileImageUrl)
        .build();
  }

  private String bearerToken(Long userId) {
    return "Bearer " + jwtProvider.generateToken(userId);
  }

  private Comment saveComment(
      Post post,
      User author,
      String content,
      LocalDateTime createdAt
  ) {
    Comment comment = commentRepository.saveAndFlush(new Comment(post, author, content));
    jdbcTemplate.update(
        "UPDATE post_comment SET created_at = ?, updated_at = ? WHERE id = ?",
        createdAt,
        createdAt,
        comment.getId()
    );
    return comment;
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
    jdbcTemplate.update("DELETE FROM university");
    jdbcTemplate.update("DELETE FROM city");
    jdbcTemplate.update("DELETE FROM country");
  }
}
