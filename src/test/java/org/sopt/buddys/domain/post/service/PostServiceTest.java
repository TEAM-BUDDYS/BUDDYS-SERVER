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
import org.sopt.buddys.domain.post.code.PostErrorCode;
import org.sopt.buddys.domain.post.entity.AgeCondition;
import org.sopt.buddys.domain.post.entity.CompanionType;
import org.sopt.buddys.domain.post.entity.GenderCondition;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.sopt.buddys.domain.post.entity.RecruitmentCountType;
import org.sopt.buddys.domain.post.repository.PostAgeConditionRepository;
import org.sopt.buddys.domain.post.repository.PostGenderConditionRepository;
import org.sopt.buddys.domain.post.repository.PostImageRepository;
import org.sopt.buddys.domain.post.repository.PostRepository;
import org.sopt.buddys.domain.post.repository.PostTagRepository;
import org.sopt.buddys.domain.post.service.command.CreatePostCommand;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.data.domain.Sort;
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
class PostServiceTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private PostService postService;

  @Autowired
  private PostRepository postRepository;

  @Autowired
  private PostAgeConditionRepository postAgeConditionRepository;

  @Autowired
  private PostGenderConditionRepository postGenderConditionRepository;

  @Autowired
  private PostTagRepository postTagRepository;

  @Autowired
  private PostImageRepository postImageRepository;

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

  @DisplayName("게시글 작성 시 게시글, 태그, 이미지가 저장된다")
  @Test
  void createPost_savesPostWithTagsAndImages() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Long countryId = insertCountry("대한민국", "KR");
    Long cityId = insertCity(countryId, "Seoul", "서울특별시", 10_000_000L);
    Long tagId = insertTag("여행", TagType.ACTIVITY);

    CreatePostCommand command = new CreatePostCommand(
        countryId,
        cityId,
        LocalDate.now().plusDays(10),
        LocalDate.now().plusDays(20),
        " 주말에 파리 근교 함께 가실 분! ",
        " 안녕하세요. 같이 여행하실 분을 구합니다. ",
        List.of(AgeCondition.EARLY_20S, AgeCondition.MID_20S),
        List.of(GenderCondition.MALE, GenderCondition.FEMALE),
        CompanionType.FULL_TRIP,
        RecruitmentCountType.TWO,
        List.of(tagId),
        List.of("https://example.com/first.png", "https://example.com/second.png")
    );

    // when
    Post post = postService.createPost(user.getId(), command);

    // then
    Post savedPost = postRepository.findById(post.getId()).orElseThrow();
    assertThat(savedPost.getTitle()).isEqualTo("주말에 파리 근교 함께 가실 분!");
    assertThat(savedPost.getContent()).isEqualTo("안녕하세요. 같이 여행하실 분을 구합니다.");
    assertThat(savedPost.getStatus()).isEqualTo(PostStatus.RECRUITING);
    assertThat(savedPost.getCompanionType()).isEqualTo(CompanionType.FULL_TRIP);
    assertThat(savedPost.getRecruitmentCountType()).isEqualTo(RecruitmentCountType.TWO);
    assertThat(postAgeConditionRepository.findAll()).hasSize(2);
    assertThat(postGenderConditionRepository.findAll()).hasSize(2);
    assertThat(postTagRepository.findAll()).hasSize(1);
    assertThat(postImageRepository.findAll(Sort.by("orderNo")))
        .extracting("imageUrl")
        .containsExactly("https://example.com/first.png", "https://example.com/second.png");
  }

  @DisplayName("종료일이 시작일보다 빠르면 예외가 발생한다")
  @Test
  void createPost_endDateBeforeStartDate_throwsInvalidRequest() {
    // given
    Long countryId = insertCountry("대한민국", "KR");
    Long cityId = insertCity(countryId, "Seoul", "서울특별시", 10_000_000L);

    CreatePostCommand command = createDefaultCommand(
        countryId,
        cityId,
        LocalDate.now().plusDays(20),
        LocalDate.now().plusDays(10)
    );

    // when, then
    assertThatThrownBy(() -> postService.createPost(1L, command))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(GlobalErrorCode.INVALID_REQUEST)
        );
  }

  @DisplayName("도시가 요청 국가에 속하지 않으면 예외가 발생한다")
  @Test
  void createPost_cityNotInCountry_throwsException() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Long koreaId = insertCountry("대한민국", "KR");
    Long franceId = insertCountry("프랑스", "FR");
    Long parisId = insertCity(franceId, "Paris", "파리", 2_000_000L);

    CreatePostCommand command = createDefaultCommand(koreaId, parisId);

    // when, then
    assertThatThrownBy(() -> postService.createPost(user.getId(), command))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(PostErrorCode.CITY_NOT_IN_COUNTRY)
        );
  }

  @DisplayName("활동 태그가 하나도 없으면 예외가 발생한다")
  @Test
  void createPost_withoutActivityTag_throwsException() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Long countryId = insertCountry("대한민국", "KR");
    Long cityId = insertCity(countryId, "Seoul", "서울특별시", 10_000_000L);
    Long interestTagId = insertTag("자연", TagType.INTEREST);

    CreatePostCommand command = new CreatePostCommand(
        countryId,
        cityId,
        LocalDate.now().plusDays(10),
        LocalDate.now().plusDays(20),
        "제목",
        "본문",
        List.of(AgeCondition.EARLY_20S),
        List.of(GenderCondition.ANY),
        CompanionType.FULL_TRIP,
        RecruitmentCountType.TWO,
        List.of(interestTagId),
        List.of()
    );

    // when, then
    assertThatThrownBy(() -> postService.createPost(user.getId(), command))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(PostErrorCode.ACTIVITY_TAG_REQUIRED)
        );
    assertThat(postRepository.findAll()).isEmpty();
  }

  private CreatePostCommand createDefaultCommand(Long countryId, Long cityId) {
    return createDefaultCommand(
        countryId,
        cityId,
        LocalDate.now().plusDays(10),
        LocalDate.now().plusDays(20)
    );
  }

  private CreatePostCommand createDefaultCommand(
      Long countryId,
      Long cityId,
      LocalDate startDate,
      LocalDate endDate
  ) {
    return new CreatePostCommand(
        countryId,
        cityId,
        startDate,
        endDate,
        "제목",
        "본문",
        List.of(AgeCondition.EARLY_20S),
        List.of(GenderCondition.ANY),
        CompanionType.FULL_TRIP,
        RecruitmentCountType.TWO,
        List.of(1L),
        List.of()
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
    jdbcTemplate.update("DELETE FROM city");
    jdbcTemplate.update("DELETE FROM country");
    jdbcTemplate.update("DELETE FROM `user`");
  }
}
