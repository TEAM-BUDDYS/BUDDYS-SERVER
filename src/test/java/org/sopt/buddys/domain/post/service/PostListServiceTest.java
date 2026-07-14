package org.sopt.buddys.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.location.repository.CountryRepository;
import org.sopt.buddys.domain.post.dto.response.PostListResponse;
import org.sopt.buddys.domain.post.entity.AgeCondition;
import org.sopt.buddys.domain.post.entity.CompanionType;
import org.sopt.buddys.domain.post.entity.GenderCondition;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.sopt.buddys.domain.post.entity.RecruitmentCountType;
import org.sopt.buddys.domain.post.repository.PostRepository;
import org.sopt.buddys.domain.post.service.command.CreatePostCommand;
import org.sopt.buddys.domain.post.service.command.PostSearchCondition;
import org.sopt.buddys.domain.post.service.result.PostListResult;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PostListServiceTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private PostService postService;

  @Autowired
  private PostRepository postRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CountryRepository countryRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private User viewer;
  private User author;
  private Long franceId;
  private Long parisId;
  private Long japanId;
  private Long tokyoId;
  private Long travelTagId;
  private Long mealTagId;

  @BeforeEach
  void setUp() {
    cleanUp();
    viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));
    author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    franceId = insertCountry("France", "FR");
    parisId = insertCity(franceId, "Paris", "파리", 2_000_000L);
    japanId = insertCountry("Japan", "JP");
    tokyoId = insertCity(japanId, "Tokyo", "도쿄", 10_000_000L);
    travelTagId = insertTag("여행", TagType.ACTIVITY);
    mealTagId = insertTag("맛집 탐방", TagType.ACTIVITY);
  }

  @AfterEach
  void tearDown() {
    cleanUp();
  }

  @DisplayName("조건 없이 조회하면 모집중 게시글만 최신순으로 반환하고 내 글은 제외한다")
  @Test
  void getPosts_withoutCondition_returnsRecruitingPostsOrderByLatestExceptMine() {
    // given
    createPost(author.getId(), "오래된 글", "본문", franceId, parisId,
        List.of(GenderCondition.MALE, GenderCondition.FEMALE));
    Post myPost = createPost(viewer.getId(), "내 글", "본문", franceId, parisId,
        List.of(GenderCondition.MALE, GenderCondition.FEMALE));
    Post completedPost = createPost(author.getId(), "모집완료 글", "본문", franceId, parisId,
        List.of(GenderCondition.MALE, GenderCondition.FEMALE));
    completedPost.updateStatus(PostStatus.COMPLETED);
    postRepository.save(completedPost);
    createPost(author.getId(), "최신 글", "본문", franceId, parisId,
        List.of(GenderCondition.MALE, GenderCondition.FEMALE));

    // when
    PostListResult result = postService.getPosts(viewer.getId(), emptyCondition(), 0, 20);

    // then
    assertThat(result.content()).extracting(post -> post.post().getTitle())
        .containsExactly("최신 글", "오래된 글");
    assertThat(result.content()).extracting(post -> post.post().getId())
        .doesNotContain(myPost.getId(), completedPost.getId());
    assertThat(result.hasNext()).isFalse();
  }

  @DisplayName("검색어는 제목과 본문에 적용되고 공백이면 검색 조건을 무시한다")
  @Test
  void getPosts_keyword_filtersTitleOrContentAndBlankKeywordIgnored() {
    // given
    createPost(author.getId(), "파리 산책", "본문", franceId, parisId,
        List.of(GenderCondition.MALE, GenderCondition.FEMALE));
    createPost(author.getId(), "제목", "파리 맛집 가요", franceId, parisId,
        List.of(GenderCondition.MALE, GenderCondition.FEMALE));
    createPost(author.getId(), "도쿄 산책", "본문", japanId, tokyoId,
        List.of(GenderCondition.MALE, GenderCondition.FEMALE));

    // when
    PostListResult keywordResult = postService.getPosts(viewer.getId(), condition("파리"), 0, 20);
    PostListResult blankKeywordResult = postService.getPosts(viewer.getId(), condition("   "), 0, 20);

    // then
    assertThat(keywordResult.content()).extracting(post -> post.post().getTitle())
        .containsExactly("제목", "파리 산책");
    assertThat(blankKeywordResult.content()).hasSize(3);
  }

  @DisplayName("국가, 날짜, 나이, 성별, 동행 유형, 태그 조건을 함께 적용한다")
  @Test
  void getPosts_filtersByAllConditions() {
    // given
    createPost(
        author.getId(),
        "조건 일치",
        "파리 여행",
        franceId,
        parisId,
        LocalDate.now().plusDays(10),
        LocalDate.now().plusDays(12),
        List.of(AgeCondition.EARLY_20S),
        List.of(GenderCondition.FEMALE),
        CompanionType.MEAL,
        List.of(mealTagId),
        List.of()
    );
    createPost(author.getId(), "국가 불일치", "파리 여행", japanId, tokyoId, List.of(GenderCondition.FEMALE));
    createPost(author.getId(), "성별 무관", "파리 여행", franceId, parisId,
        List.of(GenderCondition.MALE, GenderCondition.FEMALE));
    createPost(author.getId(), "태그 불일치", "파리 여행", franceId, parisId, List.of(GenderCondition.FEMALE));

    PostSearchCondition condition = new PostSearchCondition(
        "파리",
        franceId,
        LocalDate.now().plusDays(9),
        LocalDate.now().plusDays(13),
        List.of(AgeCondition.EARLY_20S),
        List.of(GenderCondition.FEMALE),
        List.of(CompanionType.MEAL),
        mealTagId
    );

    // when
    PostListResult result = postService.getPosts(viewer.getId(), condition, 0, 20);

    // then
    assertThat(result.content()).extracting(post -> post.post().getTitle())
        .containsExactly("조건 일치");
  }

  @DisplayName("성별 무관 게시글은 남성과 여성 필터에 모두 반환된다")
  @Test
  void getPosts_genderCondition_includesAnyCondition() {
    // given
    createPost(author.getId(), "여성 조건", "본문", franceId, parisId, List.of(GenderCondition.FEMALE));
    createPost(author.getId(), "성별 무관", "본문", franceId, parisId,
        List.of(GenderCondition.MALE, GenderCondition.FEMALE));
    createPost(author.getId(), "남성 조건", "본문", franceId, parisId, List.of(GenderCondition.MALE));

    // when
    PostListResult femaleResult = postService.getPosts(
        viewer.getId(),
        new PostSearchCondition(null, null, null, null, null, List.of(GenderCondition.FEMALE), null, null),
        0,
        20
    );
    PostListResult maleResult = postService.getPosts(
        viewer.getId(),
        new PostSearchCondition(null, null, null, null, null, List.of(GenderCondition.MALE), null, null),
        0,
        20
    );

    // then
    assertThat(femaleResult.content()).extracting(post -> post.post().getTitle())
        .containsExactly("성별 무관", "여성 조건");
    assertThat(maleResult.content()).extracting(post -> post.post().getTitle())
        .containsExactly("남성 조건", "성별 무관");
  }

  @DisplayName("다중 조건에 매칭되어도 같은 게시글은 한 번만 반환한다")
  @Test
  void getPosts_multipleMatchedConditions_returnsDistinctPosts() {
    // given
    Post matchedPost = createPost(
        author.getId(),
        "중복 없이 조회",
        "본문",
        franceId,
        parisId,
        LocalDate.now().plusDays(10),
        LocalDate.now().plusDays(11),
        List.of(AgeCondition.EARLY_20S, AgeCondition.MID_20S),
        List.of(GenderCondition.MALE, GenderCondition.FEMALE),
        CompanionType.MEAL,
        List.of(travelTagId, mealTagId),
        List.of()
    );
    createPost(
        viewer.getId(),
        "내 글 제외",
        "본문",
        franceId,
        parisId,
        LocalDate.now().plusDays(10),
        LocalDate.now().plusDays(11),
        List.of(AgeCondition.EARLY_20S, AgeCondition.MID_20S),
        List.of(GenderCondition.MALE, GenderCondition.FEMALE),
        CompanionType.MEAL,
        List.of(travelTagId, mealTagId),
        List.of()
    );
    Post completedPost = createPost(
        author.getId(),
        "모집완료 제외",
        "본문",
        franceId,
        parisId,
        LocalDate.now().plusDays(10),
        LocalDate.now().plusDays(11),
        List.of(AgeCondition.EARLY_20S, AgeCondition.MID_20S),
        List.of(GenderCondition.MALE, GenderCondition.FEMALE),
        CompanionType.MEAL,
        List.of(travelTagId, mealTagId),
        List.of()
    );
    completedPost.updateStatus(PostStatus.COMPLETED);
    postRepository.save(completedPost);

    PostSearchCondition condition = new PostSearchCondition(
        null,
        franceId,
        null,
        null,
        List.of(AgeCondition.EARLY_20S, AgeCondition.MID_20S),
        List.of(GenderCondition.MALE, GenderCondition.FEMALE),
        List.of(CompanionType.MEAL),
        mealTagId
    );

    // when
    PostListResult result = postService.getPosts(viewer.getId(), condition, 0, 20);

    // then
    assertThat(result.content()).extracting(post -> post.post().getId())
        .containsExactly(matchedPost.getId());
  }

  @DisplayName("페이징, hasNext, 썸네일, durationDays를 반환한다")
  @Test
  void getPosts_returnsPagingThumbnailAndDurationDays() {
    // given
    createPost(
        author.getId(),
        "이미지 없는 글",
        "본문",
        franceId,
        parisId,
        LocalDate.now().plusDays(20),
        LocalDate.now().plusDays(20),
        List.of(AgeCondition.EARLY_20S),
        List.of(GenderCondition.MALE, GenderCondition.FEMALE),
        CompanionType.FULL_TRIP,
        List.of(travelTagId),
        List.of()
    );
    createPost(
        author.getId(),
        "이미지 있는 글",
        "본문",
        franceId,
        parisId,
        LocalDate.now().plusDays(10),
        LocalDate.now().plusDays(15),
        List.of(AgeCondition.EARLY_20S),
        List.of(GenderCondition.MALE, GenderCondition.FEMALE),
        CompanionType.FULL_TRIP,
        List.of(travelTagId),
        List.of("https://example.com/first.png", "https://example.com/second.png")
    );

    // when
    PostListResult firstPage = postService.getPosts(viewer.getId(), emptyCondition(), 0, 1);
    PostListResult secondPage = postService.getPosts(viewer.getId(), emptyCondition(), 1, 1);
    PostListResponse response = PostListResponse.from(firstPage);

    // then
    assertThat(firstPage.hasNext()).isTrue();
    assertThat(secondPage.hasNext()).isFalse();
    assertThat(response.content().get(0).durationDays()).isEqualTo(6);
    assertThat(response.content().get(0).thumbnailImageUrl()).isEqualTo("https://example.com/first.png");
    assertThat(PostListResponse.from(secondPage).content().get(0).durationDays()).isEqualTo(1);
    assertThat(PostListResponse.from(secondPage).content().get(0).thumbnailImageUrl()).isNull();
  }

  @DisplayName("결과가 없으면 빈 배열과 hasNext=false를 반환한다")
  @Test
  void getPosts_noResult_returnsEmptyContent() {
    // when
    PostListResult result = postService.getPosts(viewer.getId(), condition("없는검색어"), 0, 20);

    // then
    assertThat(result.content()).isEmpty();
    assertThat(result.hasNext()).isFalse();
  }

  @DisplayName("page, size, 날짜 범위가 유효하지 않으면 예외가 발생한다")
  @Test
  void getPosts_invalidRequest_throwsException() {
    // given
    PostSearchCondition invalidDateCondition = new PostSearchCondition(
        null,
        null,
        LocalDate.now().plusDays(5),
        LocalDate.now().plusDays(4),
        null,
        null,
        null,
        null
    );

    // when, then
    assertInvalidRequest(() -> postService.getPosts(viewer.getId(), emptyCondition(), -1, 20));
    assertInvalidRequest(() -> postService.getPosts(viewer.getId(), emptyCondition(), 0, 0));
    assertInvalidRequest(() -> postService.getPosts(viewer.getId(), invalidDateCondition, 0, 20));
  }

  private void assertInvalidRequest(Runnable runnable) {
    assertThatThrownBy(runnable::run)
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(GlobalErrorCode.INVALID_REQUEST)
        );
  }

  private PostSearchCondition emptyCondition() {
    return new PostSearchCondition(null, null, null, null, null, null, null, null);
  }

  private PostSearchCondition condition(String keyword) {
    return new PostSearchCondition(keyword, null, null, null, null, null, null, null);
  }

  private Post createPost(
      Long authorId,
      String title,
      String content,
      Long countryId,
      Long cityId,
      List<GenderCondition> genderConditions
  ) {
    return createPost(
        authorId,
        title,
        content,
        countryId,
        cityId,
        LocalDate.now().plusDays(10),
        LocalDate.now().plusDays(12),
        List.of(AgeCondition.EARLY_20S),
        genderConditions,
        CompanionType.MEAL,
        List.of(travelTagId),
        List.of()
    );
  }

  private Post createPost(
      Long authorId,
      String title,
      String content,
      Long countryId,
      Long cityId,
      LocalDate startDate,
      LocalDate endDate,
      List<AgeCondition> ageConditions,
      List<GenderCondition> genderConditions,
      CompanionType companionType,
      List<Long> tagIds,
      List<String> imageUrls
  ) {
    return postService.createPost(
        authorId,
        new CreatePostCommand(
            countryId,
            cityId,
            startDate,
            endDate,
            title,
            content,
            ageConditions,
            genderConditions,
            companionType,
            RecruitmentCountType.TWO,
            tagIds,
            imageUrls
        )
    );
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

  private Long insertTag(String name, TagType tagType) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
          var preparedStatement = connection.prepareStatement(
              "INSERT INTO tag (name, tag_type) VALUES (?, ?)",
              Statement.RETURN_GENERATED_KEYS
          );
          preparedStatement.setString(1, name);
          preparedStatement.setString(2, tagType.name());
          return preparedStatement;
        },
        keyHolder
    );
    return keyHolder.getKey().longValue();
  }

  private void cleanUp() {
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
