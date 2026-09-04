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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.comment.entity.CourseComment;
import org.sopt.buddys.domain.comment.repository.CourseCommentRepository;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.repository.CourseRepository;
import org.sopt.buddys.domain.course.service.CourseService;
import org.sopt.buddys.domain.course.service.command.CourseDayCommand;
import org.sopt.buddys.domain.course.service.command.CreateCourseCommand;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.support.IntegrationTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

class CourseCommentControllerTest extends IntegrationTestSupport {

  @Autowired
  private CourseCommentRepository courseCommentRepository;

  @Autowired
  private CourseRepository courseRepository;

  @Autowired
  private CourseService courseService;

  @Autowired
  private UserRepository userRepository;

  @DisplayName("로그인한 사용자는 코스에 댓글을 작성할 수 있다")
  @Test
  void createComment_authenticatedUser_savesCommentAndIncreasesCommentCount() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User commenter = userRepository.save(createUser("commenter@test.com", "provider-commenter", "댓글작성자"));
    Course course = createCourse(author);

    // when, then
    mockMvc.perform(post("/api/v1/courses/{courseId}/comments", course.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(commenter.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "content": "저도 같이 가고 싶어요!"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("COMMENT-S001"))
        .andExpect(jsonPath("$.data.commentId").isNumber());

    List<CourseComment> comments = courseCommentRepository.findAll();
    assertThat(comments).hasSize(1);
    assertThat(comments.get(0).getCourse().getId()).isEqualTo(course.getId());
    assertThat(comments.get(0).getAuthor().getId()).isEqualTo(commenter.getId());
    assertThat(comments.get(0).getContent()).isEqualTo("저도 같이 가고 싶어요!");
    assertThat(courseRepository.findById(course.getId()).orElseThrow().getCommentCount()).isEqualTo(1L);
  }

  @DisplayName("존재하지 않는 코스에 댓글을 작성하면 예외가 발생한다")
  @Test
  void createComment_courseNotFound_returnsNotFound() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));

    // when, then
    mockMvc.perform(post("/api/v1/courses/{courseId}/comments", 999_999L)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "content": "저도 같이 가고 싶어요!"
                }
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COURSE-E005"));

    assertThat(courseCommentRepository.findAll()).isEmpty();
  }

  @DisplayName("댓글 내용이 빈 문자열이면 예외가 발생한다")
  @Test
  void createComment_blankContent_returnsBadRequest() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Course course = createCourse(author);

    // when, then
    mockMvc.perform(post("/api/v1/courses/{courseId}/comments", course.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "content": "   "
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("GLB-E001"));

    assertThat(courseCommentRepository.findAll()).isEmpty();
  }

  @DisplayName("로그인하지 않은 사용자는 코스에 댓글을 작성할 수 없다")
  @Test
  void createComment_unauthenticatedUser_returnsUnauthorized() throws Exception {
    mockMvc.perform(post("/api/v1/courses/{courseId}/comments", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "content": "저도 같이 가고 싶어요!"
                }
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("GLB-E002"));
  }

  @DisplayName("코스 댓글 목록을 작성 시간 오름차순으로 조회할 수 있다")
  @Test
  void getComments_returnsCommentsOrderByCreatedAtAsc() throws Exception {
    // given
    User courseAuthor = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User commentAuthor = userRepository.save(createUser("commenter@test.com", "provider-commenter", "댓글작성자"));
    Course course = createCourse(courseAuthor);

    CourseComment recentComment = saveComment(course, commentAuthor, "최신 댓글", LocalDateTime.now().minusMinutes(3));
    CourseComment oldComment = saveComment(course, commentAuthor, "오래된 댓글", LocalDateTime.now().minusHours(2));

    // when, then
    mockMvc.perform(get("/api/v1/courses/{courseId}/comments", course.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(commentAuthor.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("COMMENT-S002"))
        .andExpect(jsonPath("$.data.comments.length()").value(2))
        .andExpect(jsonPath("$.data.hasNext").value(false))
        .andExpect(jsonPath("$.data.comments[0].commentId").value(oldComment.getId()))
        .andExpect(jsonPath("$.data.comments[0].content").value("오래된 댓글"))
        .andExpect(jsonPath("$.data.comments[0].timeAgo").value("2시간 전"))
        .andExpect(jsonPath("$.data.comments[1].commentId").value(recentComment.getId()))
        .andExpect(jsonPath("$.data.comments[1].content").value("최신 댓글"))
        .andExpect(jsonPath("$.data.comments[1].timeAgo").value("3분 전"));
  }

  @DisplayName("작성 시간이 같은 댓글이 여러 개여도 페이지 경계에서 중복이나 누락 없이 안정적으로 정렬된다")
  @Test
  void getComments_sameCreatedAt_stablePaginationById() throws Exception {
    // given
    User courseAuthor = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User commentAuthor = userRepository.save(createUser("commenter@test.com", "provider-commenter", "댓글작성자"));
    Course course = createCourse(courseAuthor);

    LocalDateTime sameCreatedAt = LocalDateTime.now().minusHours(1);
    CourseComment first = saveComment(course, commentAuthor, "댓글1", sameCreatedAt);
    CourseComment second = saveComment(course, commentAuthor, "댓글2", sameCreatedAt);
    CourseComment third = saveComment(course, commentAuthor, "댓글3", sameCreatedAt);

    // when, then
    mockMvc.perform(get("/api/v1/courses/{courseId}/comments", course.getId())
            .queryParam("page", "0")
            .queryParam("size", "2")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(commentAuthor.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.comments[0].commentId").value(first.getId()))
        .andExpect(jsonPath("$.data.comments[1].commentId").value(second.getId()))
        .andExpect(jsonPath("$.data.hasNext").value(true));

    mockMvc.perform(get("/api/v1/courses/{courseId}/comments", course.getId())
            .queryParam("page", "1")
            .queryParam("size", "2")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(commentAuthor.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.comments.length()").value(1))
        .andExpect(jsonPath("$.data.comments[0].commentId").value(third.getId()))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @DisplayName("탈퇴한 유저가 작성한 댓글은 목록 조회 시 닉네임과 프로필 이미지가 가려진다")
  @Test
  void getComments_withdrawnCommentAuthor_masksNicknameAndProfileImage() throws Exception {
    // given
    User courseAuthor = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User commentAuthor = userRepository.save(createUser("commenter@test.com", "provider-commenter", "댓글작성자"));
    Course course = createCourse(courseAuthor);
    CourseComment comment = saveComment(course, commentAuthor, "댓글", LocalDateTime.now().minusMinutes(1));
    jdbcTemplate.update("UPDATE `user` SET deleted_at = ? WHERE id = ?", LocalDateTime.now(), commentAuthor.getId());

    // when, then
    mockMvc.perform(get("/api/v1/courses/{courseId}/comments", course.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(courseAuthor.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.comments[0].commentId").value(comment.getId()))
        .andExpect(jsonPath("$.data.comments[0].writerName").value("탈퇴한 사용자"))
        .andExpect(jsonPath("$.data.comments[0].writerProfileImageUrl").doesNotExist());
  }

  @DisplayName("댓글이 없는 코스는 빈 댓글 배열을 반환한다")
  @Test
  void getComments_emptyCourse_returnsEmptyArray() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Course course = createCourse(author);

    // when, then
    mockMvc.perform(get("/api/v1/courses/{courseId}/comments", course.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.comments").isArray())
        .andExpect(jsonPath("$.data.comments.length()").value(0));
  }

  @DisplayName("존재하지 않는 코스의 댓글 목록을 조회하면 예외가 발생한다")
  @Test
  void getComments_courseNotFound_returnsNotFound() throws Exception {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));

    // when, then
    mockMvc.perform(get("/api/v1/courses/{courseId}/comments", 999_999L)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COURSE-E005"));
  }

  @DisplayName("로그인하지 않은 사용자는 코스 댓글 목록을 조회할 수 없다")
  @Test
  void getComments_unauthenticatedUser_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/courses/{courseId}/comments", 1L))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("GLB-E002"));
  }

  private Course createCourse(User author) {
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    CreateCourseCommand command = new CreateCourseCommand(
        List.of(countryId), List.of(cityId), "파리 코스", null, null,
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
        List.of(tagId), null,
        List.of(new CourseDayCommand((short) 1, null, List.of("https://example.com/day1.jpg"), null)),
        null
    );
    return courseService.createCourse(author.getId(), command);
  }

  private CourseComment saveComment(
      Course course,
      User author,
      String content,
      LocalDateTime createdAt
  ) {
    CourseComment comment = courseCommentRepository.saveAndFlush(new CourseComment(course, author, content));
    jdbcTemplate.update(
        "UPDATE course_comment SET created_at = ?, updated_at = ? WHERE id = ?",
        createdAt,
        createdAt,
        comment.getId()
    );
    return comment;
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
