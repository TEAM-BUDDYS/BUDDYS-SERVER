package org.sopt.buddys.domain.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.sql.Statement;
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
import org.sopt.buddys.domain.location.entity.City;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.location.repository.CityRepository;
import org.sopt.buddys.domain.location.repository.CountryRepository;
import org.sopt.buddys.domain.post.entity.CompanionType;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.entity.PostBookmark;
import org.sopt.buddys.domain.post.entity.PostImage;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.sopt.buddys.domain.post.entity.RecruitmentCountType;
import org.sopt.buddys.domain.post.repository.PostImageRepository;
import org.sopt.buddys.domain.post.repository.PostBookmarkRepository;
import org.sopt.buddys.domain.post.repository.PostRepository;
import org.sopt.buddys.domain.post.service.PostService;
import org.sopt.buddys.domain.post.service.result.PostBookmarkResult;
import org.sopt.buddys.domain.tag.entity.TagType;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
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
  private PostImageRepository postImageRepository;

  @Autowired
  private PostBookmarkRepository postBookmarkRepository;

  @Autowired
  private PostService postService;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private EntityManager entityManager;

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

  @DisplayName("제목만 수정하면 다른 게시글 필드는 유지되고 게시글 ID를 반환한다")
  @Test
  void updatePost_titleOnly_updatesTitleAndKeepsOthers() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);

    mockMvc.perform(patch("/api/v1/posts/{postId}", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"  변경된 제목  \"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("POST-S004"))
        .andExpect(jsonPath("$.message").value("게시글 수정에 성공했습니다."))
        .andExpect(jsonPath("$.data.postId").value(post.getId()));

    Post updated = postRepository.findById(post.getId()).orElseThrow();
    assertThat(updated.getTitle()).isEqualTo("변경된 제목");
    assertThat(updated.getContent()).isEqualTo("함께 여행하실 분을 구합니다.");
    assertThat(updated.getStartDate()).isEqualTo(post.getStartDate());
    assertThat(updated.getCountry().getId()).isEqualTo(post.getCountry().getId());
    assertThat(updated.getCity().getId()).isEqualTo(post.getCity().getId());
  }

  @DisplayName("모집 완료 게시글의 본문도 수정할 수 있다")
  @Test
  void updatePost_completedPost_updatesContent() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);
    post.updateStatus(PostStatus.COMPLETED);
    postRepository.saveAndFlush(post);

    mockMvc.perform(patch("/api/v1/posts/{postId}", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\" 새 본문 \"}"))
        .andExpect(status().isOk());

    assertThat(postRepository.findById(post.getId()).orElseThrow().getContent()).isEqualTo("새 본문");
  }

  @DisplayName("국가와 도시를 함께 수정하고 최종 조합을 저장한다")
  @Test
  void updatePost_countryAndCity_updatesPair() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);
    Long franceId = insertCountry("프랑스", "FR");
    Long parisId = insertCity(franceId, "Paris", "파리", 2_000_000L);

    mockMvc.perform(patch("/api/v1/posts/{postId}", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"countryId\":%d,\"cityId\":%d}".formatted(franceId, parisId)))
        .andExpect(status().isOk());

    Post updated = postRepository.findById(post.getId()).orElseThrow();
    assertThat(updated.getCountry().getId()).isEqualTo(franceId);
    assertThat(updated.getCity().getId()).isEqualTo(parisId);
  }

  @DisplayName("국가만 전달하면 잘못된 요청으로 거부한다")
  @Test
  void updatePost_countryOnly_returnsInvalidRequest() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);
    Long franceId = insertCountry("프랑스", "FR");

    mockMvc.perform(patch("/api/v1/posts/{postId}", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"countryId\":%d}".formatted(franceId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("GLB-E001"));
  }

  @DisplayName("도시만 전달하면 기존 국가에 속한 도시로 수정한다")
  @Test
  void updatePost_cityOnly_updatesCityWithinExistingCountry() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);
    Long busanId = insertCity(post.getCountry().getId(), "Busan", "부산광역시", 3_000_000L);

    mockMvc.perform(patch("/api/v1/posts/{postId}", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"cityId\":%d}".formatted(busanId)))
        .andExpect(status().isOk());

    Post updated = postRepository.findById(post.getId()).orElseThrow();
    assertThat(updated.getCountry().getId()).isEqualTo(post.getCountry().getId());
    assertThat(updated.getCity().getId()).isEqualTo(busanId);
  }

  @DisplayName("국가와 도시의 소속 관계가 맞지 않으면 기존 오류로 거부한다")
  @Test
  void updatePost_countryAndCityMismatch_returnsCityNotInCountry() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);
    Long franceId = insertCountry("프랑스", "FR");

    assertUpdateError(
        author,
        post,
        "{\"countryId\":%d,\"cityId\":%d}".formatted(franceId, post.getCity().getId()),
        "POST-E002",
        400
    );
  }

  @DisplayName("시작일 하나만 수정해 최종 날짜 조합을 검증한다")
  @Test
  void updatePost_startDateOnly_updatesDate() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);
    LocalDate newStart = LocalDate.now().plusDays(11);

    mockMvc.perform(patch("/api/v1/posts/{postId}", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"startDate\":\"%s\"}".formatted(newStart)))
        .andExpect(status().isOk());
    assertThat(postRepository.findById(post.getId()).orElseThrow().getStartDate()).isEqualTo(newStart);
  }

  @DisplayName("빈 객체와 명시적인 null 요청은 실패한다")
  @Test
  void updatePost_emptyOrExplicitNull_returnsBadRequest() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);

    for (String content : List.of(
        "{}", "{\"title\":null}", "{\"imageUrls\":null}",
        "{\"countryId\":null}", "{\"cityId\":null}")) {
      mockMvc.perform(patch("/api/v1/posts/{postId}", post.getId())
              .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
              .contentType(MediaType.APPLICATION_JSON)
              .content(content))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("GLB-E001"));
    }
  }

  @DisplayName("작성자가 아니거나 게시글이 없거나 postId가 0이면 수정할 수 없다")
  @Test
  void updatePost_invalidTarget_returnsExpectedErrors() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User other = userRepository.save(createUser("other@test.com", "provider-other", "다른 사용자"));
    Post post = createPost(author);

    mockMvc.perform(patch("/api/v1/posts/{postId}", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(other.getId()))
            .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"변경\"}"))
        .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("GLB-E003"));
    mockMvc.perform(patch("/api/v1/posts/{postId}", 999L)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"변경\"}"))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("POST-E006"));
    mockMvc.perform(patch("/api/v1/posts/{postId}", 0L)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"변경\"}"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("GLB-E001"));
  }

  @DisplayName("태그와 이미지는 전체 교체하며 빈 이미지 배열은 전체 삭제한다")
  @Test
  void updatePost_tagsAndImages_replaceAllAndDeleteImages() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);
    Long tagId = insertTag("산책", TagType.ACTIVITY);
    postImageRepository.saveAndFlush(new PostImage(post, "https://example.com/old.png", (short) 0));

    mockMvc.perform(patch("/api/v1/posts/{postId}", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tagIds\":[%d],\"imageUrls\":[\"https://example.com/a.png\",\"https://example.com/b.png\"]}".formatted(tagId)))
        .andExpect(status().isOk());
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM post_tag WHERE post_id = ?", Integer.class, post.getId())).isOne();
    assertThat(jdbcTemplate.queryForList("SELECT image_url FROM post_image WHERE post_id = ? ORDER BY order_no", String.class, post.getId()))
        .containsExactly("https://example.com/a.png", "https://example.com/b.png");

    mockMvc.perform(patch("/api/v1/posts/{postId}", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON).content("{\"imageUrls\":[]}"))
        .andExpect(status().isOk());
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM post_image WHERE post_id = ?", Integer.class, post.getId())).isZero();
  }

  @DisplayName("기존 태그를 유지하며 태그를 교체하고 동일 요청을 반복해도 DB 관계가 정확하다")
  @Test
  void updatePost_tags_replaceByDifferenceAndRemainIdempotent() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);
    Long tag1 = insertTag("산책", TagType.ACTIVITY);
    Long tag2 = insertTag("맛집", TagType.ACTIVITY);
    Long tag3 = insertTag("전시", TagType.ACTIVITY);
    jdbcTemplate.update("INSERT INTO post_tag (post_id, tag_id) VALUES (?, ?), (?, ?)",
        post.getId(), tag1, post.getId(), tag2);

    String replaceRequest = "{\"tagIds\":[%d,%d]}".formatted(tag2, tag3);
    updatePostTags(author, post, replaceRequest);
    entityManager.clear();
    assertThat(findPostTagIds(post.getId())).containsExactly(tag2, tag3);

    updatePostTags(author, post, replaceRequest);
    entityManager.clear();
    assertThat(findPostTagIds(post.getId())).containsExactly(tag2, tag3);

    updatePostTags(author, post, replaceRequest);
    entityManager.clear();
    assertThat(findPostTagIds(post.getId())).containsExactly(tag2, tag3);
  }

  @DisplayName("나이와 성별 조건을 전체 교체하고 미전달 이미지는 유지한다")
  @Test
  void updatePost_conditions_replaceAllAndKeepOmittedImages() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);
    jdbcTemplate.update("INSERT INTO post_age_condition (post_id, age_condition) VALUES (?, ?)", post.getId(), "EARLY_20S");
    jdbcTemplate.update("INSERT INTO post_gender_condition (post_id, gender_condition) VALUES (?, ?)", post.getId(), "MALE");
    postImageRepository.saveAndFlush(new PostImage(post, "https://example.com/keep.png", (short) 0));

    mockMvc.perform(patch("/api/v1/posts/{postId}", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"ageConditions\":[\"MID_20S\",\"LATE_20S\"],\"genderConditions\":[\"FEMALE\"]}"))
        .andExpect(status().isOk());

    assertThat(jdbcTemplate.queryForList(
        "SELECT age_condition FROM post_age_condition WHERE post_id = ? ORDER BY age_condition", String.class, post.getId()))
        .containsExactlyInAnyOrder("MID_20S", "LATE_20S");
    assertThat(jdbcTemplate.queryForList(
        "SELECT gender_condition FROM post_gender_condition WHERE post_id = ?", String.class, post.getId()))
        .containsExactly("FEMALE");
    assertThat(postImageRepository.findImageUrlsByPostId(post.getId()))
        .containsExactly("https://example.com/keep.png");
  }

  @DisplayName("존재하지 않는 국가, 도시, 태그는 각각 정해진 오류를 반환한다")
  @Test
  void updatePost_missingReferences_returnDomainErrors() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);

    assertUpdateError(
        author,
        post,
        "{\"countryId\":99999,\"cityId\":%d}".formatted(post.getCity().getId()),
        "LOC-E001",
        404
    );
    assertUpdateError(author, post, "{\"cityId\":99999}", "LOC-E002", 404);
    assertUpdateError(author, post, "{\"tagIds\":[99999]}", "POST-E003", 404);
  }

  @DisplayName("잘못된 날짜, 빈 조건 목록, 태그 정책 위반은 요청을 거부한다")
  @Test
  void updatePost_invalidDatesConditionsAndTags_returnBadRequest() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);
    Long interestTag = insertTag("자연", TagType.INTEREST);
    List<Long> activities = List.of(
        insertTag("활동1", TagType.ACTIVITY), insertTag("활동2", TagType.ACTIVITY),
        insertTag("활동3", TagType.ACTIVITY), insertTag("활동4", TagType.ACTIVITY));

    assertUpdateError(author, post, "{\"startDate\":\"%s\"}".formatted(LocalDate.now().minusDays(1)), "GLB-E001", 400);
    assertUpdateError(author, post, "{\"endDate\":\"%s\"}".formatted(LocalDate.now().plusDays(1)), "GLB-E001", 400);
    assertUpdateError(author, post, "{\"ageConditions\":[]}", "GLB-E001", 400);
    assertUpdateError(author, post, "{\"genderConditions\":[]}", "GLB-E001", 400);
    assertUpdateError(author, post, "{\"tagIds\":[]}", "GLB-E001", 400);
    assertUpdateError(author, post, "{\"tagIds\":[%d]}".formatted(interestTag), "POST-E004", 400);
    assertUpdateError(author, post, "{\"tagIds\":%s}".formatted(activities), "POST-E005", 400);
  }

  @DisplayName("제목과 이미지 URL의 공백 및 길이와 이미지 개수 제한을 검증한다")
  @Test
  void updatePost_invalidTitleAndImages_returnBadRequest() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);
    String elevenImages = java.util.stream.IntStream.range(0, 11)
        .mapToObj(index -> "\"https://example.com/" + index + ".png\"")
        .collect(java.util.stream.Collectors.joining(",", "[", "]"));

    assertUpdateError(author, post, "{\"title\":\"   \"}", "GLB-E001", 400);
    assertUpdateError(author, post, "{\"title\":\"%s\"}".formatted("가".repeat(121)), "GLB-E001", 400);
    assertUpdateError(author, post, "{\"imageUrls\":[\"   \"]}", "GLB-E001", 400);
    assertUpdateError(author, post, "{\"imageUrls\":[\"%s\"]}".formatted("a".repeat(513)), "GLB-E001", 400);
    assertUpdateError(author, post, "{\"imageUrls\":%s}".formatted(elevenImages), "GLB-E001", 400);
  }

  @DisplayName("수정 API OpenAPI 스키마가 부분 수정과 필수 응답 계약을 표현한다")
  @Test
  void updatePost_openApiSchema_matchesContract() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/posts/{postId}'].patch.parameters[0].required").value(true))
        .andExpect(jsonPath("$.paths['/api/v1/posts/{postId}'].patch.requestBody.required").value(true))
        .andExpect(jsonPath("$.components.schemas.UpdatePostRequest.required").doesNotExist())
        .andExpect(jsonPath("$.components.schemas.UpdatePostRequest.properties.countryId.description")
            .value(org.hamcrest.Matchers.containsString("cityId도 함께 전달")))
        .andExpect(jsonPath("$.components.schemas.UpdatePostRequest.properties.cityId.description")
            .value(org.hamcrest.Matchers.containsString("기존 국가")))
        .andExpect(jsonPath("$.components.schemas.UpdatePostSuccessResponse.required").isArray())
        .andExpect(jsonPath("$.components.schemas.UpdatePostSuccessResponse.required").value(org.hamcrest.Matchers.containsInAnyOrder("success", "code", "message", "data")))
        .andExpect(jsonPath("$.components.schemas.UpdatePostResponse.required").value(org.hamcrest.Matchers.hasItem("postId")));
  }

  @DisplayName("작성자가 모집 중 게시글을 소프트 삭제하면 행과 연관 데이터가 유지된다")
  @Test
  void deletePost_authorSoftDeletesRecruitingPostAndKeepsRelations() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);
    Long tagId = insertTag("산책", TagType.ACTIVITY);
    jdbcTemplate.update("INSERT INTO post_tag (post_id, tag_id) VALUES (?, ?)", post.getId(), tagId);
    jdbcTemplate.update("INSERT INTO post_age_condition (post_id, age_condition) VALUES (?, ?)", post.getId(), "EARLY_20S");
    jdbcTemplate.update("INSERT INTO post_gender_condition (post_id, gender_condition) VALUES (?, ?)", post.getId(), "MALE");
    postImageRepository.saveAndFlush(new PostImage(post, "https://example.com/keep.png", (short) 0));
    jdbcTemplate.update("INSERT INTO post_comment (post_id, author_id, content, created_at, updated_at) VALUES (?, ?, ?, NOW(6), NOW(6))",
        post.getId(), author.getId(), "유지할 댓글");

    mockMvc.perform(delete("/api/v1/posts/{postId}", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("POST-S005"))
        .andExpect(jsonPath("$.message").value("게시글 삭제에 성공했습니다."))
        .andExpect(jsonPath("$.data.postId").value(post.getId()))
        .andExpect(jsonPath("$.data.deletedAt").doesNotExist());

    Post deletedPost = postRepository.findById(post.getId()).orElseThrow();
    assertThat(deletedPost.getDeletedAt()).isNotNull();
    assertThat(postRepository.count()).isOne();
    for (String table : List.of("post_comment", "post_image", "post_tag", "post_age_condition", "post_gender_condition")) {
      assertThat(jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM " + table + " WHERE post_id = ?", Integer.class, post.getId())).isOne();
    }
  }

  @DisplayName("작성자는 모집 완료 게시글도 삭제할 수 있다")
  @Test
  void deletePost_completedPost_succeeds() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);
    post.updateStatus(PostStatus.COMPLETED);
    postRepository.saveAndFlush(post);

    mockMvc.perform(delete("/api/v1/posts/{postId}", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId())))
        .andExpect(status().isOk());
    assertThat(postRepository.findById(post.getId()).orElseThrow().getDeletedAt()).isNotNull();
  }

  @DisplayName("삭제 트랜잭션에서 예외가 발생하면 삭제 시각 기록이 롤백된다")
  @Test
  void deletePost_transactionFailure_rollsBackDeletedAt() {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

    assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
      postService.deletePost(author.getId(), post.getId());
      throw new IllegalStateException("rollback test");
    })).isInstanceOf(IllegalStateException.class);

    entityManager.clear();
    assertThat(postRepository.findById(post.getId()).orElseThrow().getDeletedAt()).isNull();
  }

  @DisplayName("게시글 삭제는 작성자 권한, 존재 여부, postId와 인증을 검증한다")
  @Test
  void deletePost_invalidRequests_returnExpectedErrors() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User other = userRepository.save(createUser("other@test.com", "provider-other", "다른 사용자"));
    Post post = createPost(author);

    mockMvc.perform(delete("/api/v1/posts/{postId}", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(other.getId())))
        .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("GLB-E003"));
    mockMvc.perform(delete("/api/v1/posts/{postId}", 99999L)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId())))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("POST-E006"));
    mockMvc.perform(delete("/api/v1/posts/{postId}", 0L)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId())))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("GLB-E001"));
    mockMvc.perform(delete("/api/v1/posts/{postId}", post.getId()))
        .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("GLB-E002"));

    mockMvc.perform(delete("/api/v1/posts/{postId}", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId())))
        .andExpect(status().isOk());
    mockMvc.perform(delete("/api/v1/posts/{postId}", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId())))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("POST-E006"));
  }

  @DisplayName("삭제된 게시글은 상세, 목록, 수정, 상태 변경, 댓글과 프로필 게시글 API에서 제외된다")
  @Test
  void deletedPost_isExcludedFromUserFacingPostPaths() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));
    Post post = createPost(author);
    post.softDelete(java.time.LocalDateTime.now());
    postRepository.saveAndFlush(post);
    long viewCount = post.getViewCount();

    mockMvc.perform(get("/api/v1/posts/{postId}", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId())))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("POST-E006"));
    assertThat(postRepository.findById(post.getId()).orElseThrow().getViewCount()).isEqualTo(viewCount);

    mockMvc.perform(get("/api/v1/posts").header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId())))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isEmpty());
    mockMvc.perform(get("/api/v1/posts").header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .param("keyword", "동행"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isEmpty());
    mockMvc.perform(patch("/api/v1/posts/{postId}", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"변경\"}"))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("POST-E006"));
    mockMvc.perform(patch("/api/v1/posts/{postId}/status", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("POST-E006"));
    mockMvc.perform(get("/api/v1/posts/{postId}/comments", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId())))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("POST-E006"));
    mockMvc.perform(post("/api/v1/posts/{postId}/comments", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId()))
            .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"댓글\"}"))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("POST-E006"));
    mockMvc.perform(get("/api/v1/users/me/posts").header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId())))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.posts").isEmpty());
    mockMvc.perform(get("/api/v1/users/{userId}/posts", author.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId())))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.posts").isEmpty());
  }

  @DisplayName("삭제된 게시글은 추천 게시글에서 제외된다")
  @Test
  void deletedPost_isExcludedFromRecommendations() throws Exception {
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Post post = createPost(author);
    User viewer = userRepository.save(User.builder()
        .email("viewer@test.com").provider(AuthProvider.KAKAO).providerId("provider-viewer")
        .nickname("조회자").interestCountry(post.getCountry()).build());
    post.softDelete(java.time.LocalDateTime.now());
    postRepository.saveAndFlush(post);

    mockMvc.perform(get("/api/v1/recommendations/posts")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.posts").isEmpty());
  }

  @DisplayName("삭제 API OpenAPI 스키마는 본문 없이 필수 성공 응답을 표현한다")
  @Test
  void deletePost_openApiSchema_matchesContract() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/posts/{postId}'].delete.parameters[0].required").value(true))
        .andExpect(jsonPath("$.paths['/api/v1/posts/{postId}'].delete.requestBody").doesNotExist())
        .andExpect(jsonPath("$.components.schemas.DeletePostSuccessResponse.required")
            .value(org.hamcrest.Matchers.containsInAnyOrder("success", "code", "message", "data")))
        .andExpect(jsonPath("$.components.schemas.DeletePostResponse.required")
            .value(org.hamcrest.Matchers.hasItem("postId")));
  }

  @DisplayName("게시글을 반복 저장해도 북마크 한 건만 생성된다")
  @Test
  void bookmarkPost_repeatedRequests_areIdempotent() throws Exception {
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Post post = createPost(user);
    LocalDateTime firstCreatedAt = null;

    for (int requestCount = 0; requestCount < 2; requestCount++) {
      mockMvc.perform(post("/api/v1/posts/{postId}/bookmarks", post.getId())
              .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.code").value("POST-S006"))
          .andExpect(jsonPath("$.message").value("게시글 저장에 성공했습니다."))
          .andExpect(jsonPath("$.data.postId").value(post.getId()))
          .andExpect(jsonPath("$.data.isBookmarked").value(true));

      LocalDateTime createdAt = jdbcTemplate.queryForObject(
          "SELECT created_at FROM post_bookmark WHERE user_id = ? AND post_id = ?",
          LocalDateTime.class,
          user.getId(),
          post.getId()
      );
      if (firstCreatedAt == null) {
        firstCreatedAt = createdAt;
      } else {
        assertThat(createdAt).isEqualTo(firstCreatedAt);
      }
    }

    assertThat(postBookmarkRepository.count()).isOne();
  }

  @DisplayName("동시에 같은 게시글을 최초 저장해도 두 요청이 성공하고 북마크는 한 건만 생성된다")
  @Test
  void bookmarkPost_concurrentFirstRequests_areIdempotent() throws Exception {
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Post post = createPost(user);
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    CountDownLatch readyLatch = new CountDownLatch(2);
    CountDownLatch startLatch = new CountDownLatch(1);

    try {
      Future<PostBookmarkResult> first = executorService.submit(
          () -> bookmarkPostAfterSignal(user.getId(), post.getId(), readyLatch, startLatch));
      Future<PostBookmarkResult> second = executorService.submit(
          () -> bookmarkPostAfterSignal(user.getId(), post.getId(), readyLatch, startLatch));

      assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
      startLatch.countDown();

      assertThat(first.get(10, TimeUnit.SECONDS).isBookmarked()).isTrue();
      assertThat(second.get(10, TimeUnit.SECONDS).isBookmarked()).isTrue();
      assertThat(postBookmarkRepository.count()).isOne();
    } finally {
      executorService.shutdownNow();
      assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  @DisplayName("게시글 저장을 반복 취소해도 성공하고 북마크가 남지 않는다")
  @Test
  void removePostBookmark_repeatedRequests_areIdempotent() throws Exception {
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Post post = createPost(user);
    postBookmarkRepository.saveAndFlush(new PostBookmark(user, post));

    for (int requestCount = 0; requestCount < 2; requestCount++) {
      mockMvc.perform(delete("/api/v1/posts/{postId}/bookmarks", post.getId())
              .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.code").value("POST-S007"))
          .andExpect(jsonPath("$.message").value("게시글 저장 취소에 성공했습니다."))
          .andExpect(jsonPath("$.data.postId").value(post.getId()))
          .andExpect(jsonPath("$.data.isBookmarked").value(false));
    }

    assertThat(postBookmarkRepository.count()).isZero();
  }

  @DisplayName("저장되지 않은 게시글의 저장 취소도 성공한다")
  @Test
  void removePostBookmark_notBookmarked_succeeds() throws Exception {
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Post post = createPost(user);

    mockMvc.perform(delete("/api/v1/posts/{postId}/bookmarks", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("POST-S007"))
        .andExpect(jsonPath("$.data.isBookmarked").value(false));
  }

  @DisplayName("소프트 삭제된 게시글도 저장을 취소할 수 있다")
  @Test
  void removePostBookmark_softDeletedPost_succeeds() throws Exception {
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Post post = createPost(user);
    postBookmarkRepository.saveAndFlush(new PostBookmark(user, post));
    post.softDelete(java.time.LocalDateTime.now());
    postRepository.saveAndFlush(post);

    mockMvc.perform(delete("/api/v1/posts/{postId}/bookmarks", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("POST-S007"))
        .andExpect(jsonPath("$.data.isBookmarked").value(false));

    assertThat(postBookmarkRepository.count()).isZero();
  }

  @DisplayName("게시글 저장과 저장 취소는 게시글 존재 여부, postId와 인증을 검증한다")
  @Test
  void postBookmark_invalidRequests_returnExpectedErrors() throws Exception {
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Post post = createPost(user);

    for (String method : List.of("POST", "DELETE")) {
      var missingPostRequest = method.equals("POST")
          ? post("/api/v1/posts/{postId}/bookmarks", 99999L)
          : delete("/api/v1/posts/{postId}/bookmarks", 99999L);
      mockMvc.perform(missingPostRequest.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("POST-E006"));

      for (long invalidPostId : List.of(0L, -1L)) {
        var invalidIdRequest = method.equals("POST")
            ? post("/api/v1/posts/{postId}/bookmarks", invalidPostId)
            : delete("/api/v1/posts/{postId}/bookmarks", invalidPostId);
        mockMvc.perform(invalidIdRequest.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("GLB-E001"));
      }

      var unauthenticatedRequest = method.equals("POST")
          ? post("/api/v1/posts/{postId}/bookmarks", post.getId())
          : delete("/api/v1/posts/{postId}/bookmarks", post.getId());
      mockMvc.perform(unauthenticatedRequest)
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.code").value("GLB-E002"));
    }
  }

  @DisplayName("게시글 저장 API OpenAPI 스키마는 본문 없이 필수 성공 응답을 표현한다")
  @Test
  void postBookmark_openApiSchema_matchesContract() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/posts/{postId}/bookmarks'].post.parameters[0].required")
            .value(true))
        .andExpect(jsonPath("$.paths['/api/v1/posts/{postId}/bookmarks'].post.requestBody").doesNotExist())
        .andExpect(jsonPath("$.paths['/api/v1/posts/{postId}/bookmarks'].delete.parameters[0].required")
            .value(true))
        .andExpect(jsonPath("$.paths['/api/v1/posts/{postId}/bookmarks'].delete.requestBody").doesNotExist())
        .andExpect(jsonPath("$.components.schemas.PostBookmarkSuccessResponse.required")
            .value(org.hamcrest.Matchers.containsInAnyOrder("success", "code", "message", "data")))
        .andExpect(jsonPath("$.components.schemas.PostBookmarkSuccessResponse.properties.code.example")
            .value("POST-S006"))
        .andExpect(jsonPath("$.components.schemas.PostBookmarkSuccessResponse.properties.message.example")
            .value("게시글 저장에 성공했습니다."))
        .andExpect(jsonPath("$.components.schemas.DeletePostBookmarkSuccessResponse.required")
            .value(org.hamcrest.Matchers.containsInAnyOrder("success", "code", "message", "data")))
        .andExpect(jsonPath("$.components.schemas.DeletePostBookmarkSuccessResponse.properties.code.example")
            .value("POST-S007"))
        .andExpect(jsonPath("$.components.schemas.DeletePostBookmarkSuccessResponse.properties.message.example")
            .value("게시글 저장 취소에 성공했습니다."))
        .andExpect(jsonPath("$.components.schemas.PostBookmarkResponse.required")
            .value(org.hamcrest.Matchers.containsInAnyOrder("postId", "isBookmarked")));
  }

  @DisplayName("마감 임박 게시글은 오늘 모집 중인 활성 게시글만 원문과 부가 정보를 포함해 반환한다")
  @Test
  void getClosingSoonPosts_returnsEligiblePostsWithResponseFields() throws Exception {
    LocalDate today = LocalDate.now();
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("France", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Country country = countryRepository.findById(countryId).orElseThrow();
    City city = cityRepository.findById(cityId).orElseThrow();

    Post savedPost = createPost(author, country, city, "저장한 오늘 글", "저장한 글 본문", today, today.plusDays(3));
    Post unsavedPost = createPost(author, country, city, "저장하지 않은 오늘 글", "저장하지 않은 글 본문", today, today);
    createPost(author, country, city, "어제 글", "본문", today.minusDays(1), today);
    createPost(author, country, city, "내일 글", "본문", today.plusDays(1), today.plusDays(2));
    Post completedPost = createPost(author, country, city, "모집 완료 글", "본문", today, today.plusDays(1));
    completedPost.updateStatus(PostStatus.COMPLETED);
    postRepository.saveAndFlush(completedPost);
    Post deletedPost = createPost(author, country, city, "삭제 글", "본문", today, today.plusDays(1));
    deletedPost.softDelete(LocalDateTime.now());
    postRepository.saveAndFlush(deletedPost);
    postImageRepository.saveAndFlush(new PostImage(savedPost, "https://example.com/first.jpg", (short) 0));
    postImageRepository.saveAndFlush(new PostImage(savedPost, "https://example.com/second.jpg", (short) 1));
    postBookmarkRepository.saveAndFlush(new PostBookmark(viewer, savedPost));

    mockMvc.perform(get("/api/v1/posts/closing-soon")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("GLB-S001"))
        .andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
        .andExpect(jsonPath("$.data.content.length()").value(2))
        .andExpect(jsonPath("$.data.content[0].postId").value(savedPost.getId()))
        .andExpect(jsonPath("$.data.content[0].country.countryId").value(countryId))
        .andExpect(jsonPath("$.data.content[0].country.name").value("France"))
        .andExpect(jsonPath("$.data.content[0].title").value("저장한 오늘 글"))
        .andExpect(jsonPath("$.data.content[0].content").value("저장한 글 본문"))
        .andExpect(jsonPath("$.data.content[0].startDate").value(today.toString()))
        .andExpect(jsonPath("$.data.content[0].endDate").value(today.plusDays(3).toString()))
        .andExpect(jsonPath("$.data.content[0].durationDays").value(4))
        .andExpect(jsonPath("$.data.content[0].thumbnailImageUrl")
            .value("https://example.com/first.jpg"))
        .andExpect(jsonPath("$.data.content[0].isSaved").value(true))
        .andExpect(jsonPath("$.data.content[1].postId").value(unsavedPost.getId()))
        .andExpect(jsonPath("$.data.content[1].durationDays").value(1))
        .andExpect(jsonPath("$.data.content[1].thumbnailImageUrl").value((Object) null))
        .andExpect(jsonPath("$.data.content[1].isSaved").value(false));
  }

  @DisplayName("마감 임박 게시글은 생성일이 오래된 순으로 최대 4개 반환한다")
  @Test
  void getClosingSoonPosts_returnsOldestFourPosts() throws Exception {
    LocalDate today = LocalDate.now();
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("France", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Country country = countryRepository.findById(countryId).orElseThrow();
    City city = cityRepository.findById(cityId).orElseThrow();

    for (int index = 0; index < 5; index++) {
      Post post = createPost(author, country, city, "게시글 " + index, "본문", today, today);
      jdbcTemplate.update(
          "UPDATE post SET created_at = ? WHERE id = ?",
          LocalDateTime.of(2026, 1, 1, 0, 0).plusDays(index),
          post.getId()
      );
    }
    entityManager.clear();

    mockMvc.perform(get("/api/v1/posts/closing-soon")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(4))
        .andExpect(jsonPath("$.data.content[0].title").value("게시글 0"))
        .andExpect(jsonPath("$.data.content[1].title").value("게시글 1"))
        .andExpect(jsonPath("$.data.content[2].title").value("게시글 2"))
        .andExpect(jsonPath("$.data.content[3].title").value("게시글 3"));
  }

  @DisplayName("마감 임박 게시글이 없으면 빈 목록을 반환한다")
  @Test
  void getClosingSoonPosts_noResult_returnsEmptyContent() throws Exception {
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));

    mockMvc.perform(get("/api/v1/posts/closing-soon")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("GLB-S001"))
        .andExpect(jsonPath("$.data.content").isEmpty());
  }

  @DisplayName("로그인하지 않은 사용자는 마감 임박 게시글을 조회할 수 없다")
  @Test
  void getClosingSoonPosts_unauthenticatedUser_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/posts/closing-soon"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GLB-E002"))
        .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
  }

  @DisplayName("마감 임박 게시글 조회 OpenAPI에는 요청값이 없고 공통 응답이 문서화된다")
  @Test
  void getClosingSoonPosts_openApiContract_hasNoInputAndDocumentsResponses() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/posts/closing-soon'].get.parameters").doesNotExist())
        .andExpect(jsonPath("$.paths['/api/v1/posts/closing-soon'].get.requestBody").doesNotExist())
        .andExpect(jsonPath("$.paths['/api/v1/posts/closing-soon'].get.responses['200']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/posts/closing-soon'].get.responses['401']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/posts/closing-soon'].get.responses['500']").exists());
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

  private Post createPost(
      User author,
      Country country,
      City city,
      String title,
      String content,
      LocalDate startDate,
      LocalDate endDate
  ) {
    return postRepository.saveAndFlush(new Post(
        author,
        country,
        city,
        title,
        content,
        startDate,
        endDate,
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

  private Long insertTag(String name, TagType tagType) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      var statement = connection.prepareStatement(
          "INSERT INTO tag (name, tag_type) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
      statement.setString(1, name);
      statement.setString(2, tagType.name());
      return statement;
    }, keyHolder);
    return keyHolder.getKey().longValue();
  }

  private void assertUpdateError(User author, Post post, String body, String code, int statusCode)
      throws Exception {
    mockMvc.perform(patch("/api/v1/posts/{postId}", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().is(statusCode))
        .andExpect(jsonPath("$.code").value(code));
  }

  private void updatePostTags(User author, Post post, String body) throws Exception {
    mockMvc.perform(patch("/api/v1/posts/{postId}", post.getId())
            .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk());
  }

  private PostBookmarkResult bookmarkPostAfterSignal(
      Long userId,
      Long postId,
      CountDownLatch readyLatch,
      CountDownLatch startLatch
  ) throws InterruptedException {
    readyLatch.countDown();
    if (!startLatch.await(5, TimeUnit.SECONDS)) {
      throw new IllegalStateException("동시 저장 시작 신호를 기다리는 중 시간 초과");
    }
    return postService.bookmarkPost(userId, postId);
  }

  private List<Long> findPostTagIds(Long postId) {
    return jdbcTemplate.queryForList(
        "SELECT tag_id FROM post_tag WHERE post_id = ? ORDER BY tag_id",
        Long.class,
        postId
    );
  }

  private void cleanUp() {
    jdbcTemplate.update("DELETE FROM post_comment");
    jdbcTemplate.update("DELETE FROM post_bookmark");
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
