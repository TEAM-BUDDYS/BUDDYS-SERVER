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
import org.sopt.buddys.domain.post.code.PostErrorCode;
import org.sopt.buddys.domain.post.dto.response.PostDetailResponse;
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
import org.sopt.buddys.domain.post.service.result.PostDetailResult;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.Gender;
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
  private CountryRepository countryRepository;

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

  @DisplayName("활동 태그를 4개 이상 선택하면 예외가 발생한다")
  @Test
  void createPost_activityTagLimitExceeded_throwsException() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Long countryId = insertCountry("대한민국", "KR");
    Long cityId = insertCity(countryId, "Seoul", "서울특별시", 10_000_000L);
    List<Long> tagIds = List.of(
        insertTag("여행", TagType.ACTIVITY),
        insertTag("맛집 탐방", TagType.ACTIVITY),
        insertTag("카페 탐방", TagType.ACTIVITY),
        insertTag("쇼핑", TagType.ACTIVITY)
    );
    CreatePostCommand command = createDefaultCommand(countryId, cityId, tagIds);

    // when, then
    assertThatThrownBy(() -> postService.createPost(user.getId(), command))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(PostErrorCode.TAG_LIMIT_EXCEEDED)
        );
  }

  @DisplayName("관심사 태그를 3개 이상 선택하면 예외가 발생한다")
  @Test
  void createPost_interestTagLimitExceeded_throwsException() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Long countryId = insertCountry("대한민국", "KR");
    Long cityId = insertCity(countryId, "Seoul", "서울특별시", 10_000_000L);
    List<Long> tagIds = List.of(
        insertTag("여행", TagType.ACTIVITY),
        insertTag("자연", TagType.INTEREST),
        insertTag("도시", TagType.INTEREST),
        insertTag("예술", TagType.INTEREST)
    );
    CreatePostCommand command = createDefaultCommand(countryId, cityId, tagIds);

    // when, then
    assertThatThrownBy(() -> postService.createPost(user.getId(), command))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(PostErrorCode.TAG_LIMIT_EXCEEDED)
        );
  }

  @DisplayName("동행 스타일 태그를 3개 이상 선택하면 예외가 발생한다")
  @Test
  void createPost_travelStyleTagLimitExceeded_throwsException() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Long countryId = insertCountry("대한민국", "KR");
    Long cityId = insertCity(countryId, "Seoul", "서울특별시", 10_000_000L);
    List<Long> tagIds = List.of(
        insertTag("여행", TagType.ACTIVITY),
        insertTag("계획형", TagType.TRAVEL_STYLE),
        insertTag("즉흥형", TagType.TRAVEL_STYLE),
        insertTag("아침형", TagType.TRAVEL_STYLE)
    );
    CreatePostCommand command = createDefaultCommand(countryId, cityId, tagIds);

    // when, then
    assertThatThrownBy(() -> postService.createPost(user.getId(), command))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(PostErrorCode.TAG_LIMIT_EXCEEDED)
        );
  }

  @DisplayName("게시글 상세 조회 시 상세 정보가 반환되고 조회수가 1 증가한다")
  @Test
  void getPostDetail_returnsDetailAndIncreasesViewCount() {
    // given
    Long countryId = insertCountry("일본", "JP");
    Long cityId = insertCity(countryId, "Tokyo", "도쿄", 14_000_000L);
    Long foodTagId = insertTag("맛집", TagType.ACTIVITY);
    Long photoTagId = insertTag("사진", TagType.INTEREST);
    User user = userRepository.save(createUser(
        "user@test.com",
        "provider-user",
        "김가윤",
        countryId,
        LocalDate.of(2001, 1, 1),
        Gender.FEMALE
    ));
    Post post = postService.createPost(user.getId(), new CreatePostCommand(
        countryId,
        cityId,
        LocalDate.now().plusDays(10),
        LocalDate.now().plusDays(12),
        "도쿄 같이 여행할 동행 구해요",
        "같이 맛집이랑 관광지 다니실 분 구해요.",
        List.of(AgeCondition.EARLY_20S, AgeCondition.MID_20S),
        List.of(GenderCondition.ANY),
        CompanionType.MEAL,
        RecruitmentCountType.TWO,
        List.of(foodTagId, photoTagId),
        List.of("https://example.com/image1.jpg", "https://example.com/image2.jpg")
    ));
    jdbcTemplate.update("UPDATE post SET comment_count = 3 WHERE id = ?", post.getId());

    // when
    PostDetailResult result = postService.getPostDetail(post.getId());
    PostDetailResponse response = PostDetailResponse.from(result);

    // then
    assertThat(response.postId()).isEqualTo(post.getId());
    assertThat(response.author().name()).isEqualTo("김가윤");
    assertThat(response.author().country()).isEqualTo("일본");
    assertThat(response.author().ageRange()).isEqualTo("20대");
    assertThat(response.author().gender()).isEqualTo(Gender.FEMALE);
    assertThat(response.recruitmentStatus()).isEqualTo(PostStatus.RECRUITING);
    assertThat(response.imageUrls())
        .containsExactly("https://example.com/image1.jpg", "https://example.com/image2.jpg");
    assertThat(response.city().cityId()).isEqualTo(cityId);
    assertThat(response.city().name()).isEqualTo("Tokyo");
    assertThat(response.recruitmentCountType()).isEqualTo(RecruitmentCountType.TWO);
    assertThat(response.conditions().ageConditions())
        .containsExactly(AgeCondition.EARLY_20S, AgeCondition.MID_20S);
    assertThat(response.conditions().travelType()).isEqualTo(CompanionType.MEAL);
    assertThat(response.conditions().tags())
        .extracting(PostDetailResponse.TagResponse::name)
        .containsExactly("맛집", "사진");
    assertThat(response.viewCount()).isEqualTo(1L);
    assertThat(response.commentCount()).isEqualTo(3L);
    assertThat(postRepository.findById(post.getId()).orElseThrow().getViewCount()).isEqualTo(1L);
  }

  @DisplayName("게시글 상세 조회를 할 때마다 조회수가 1씩 증가한다")
  @Test
  void getPostDetail_increasesViewCountEveryTime() {
    // given
    User user = userRepository.save(createUser("user@test.com", "provider-user", "사용자"));
    Long countryId = insertCountry("대한민국", "KR");
    Long cityId = insertCity(countryId, "Seoul", "서울특별시", 10_000_000L);
    Long tagId = insertTag("여행", TagType.ACTIVITY);
    Post post = postService.createPost(user.getId(), createDefaultCommand(countryId, cityId, List.of(tagId)));

    // when
    postService.getPostDetail(post.getId());
    PostDetailResult secondResult = postService.getPostDetail(post.getId());

    // then
    assertThat(secondResult.post().getViewCount()).isEqualTo(2L);
    assertThat(postRepository.findById(post.getId()).orElseThrow().getViewCount()).isEqualTo(2L);
  }

  @DisplayName("존재하지 않는 게시글 상세 조회 시 예외가 발생한다")
  @Test
  void getPostDetail_postNotFound_throwsException() {
    assertThatThrownBy(() -> postService.getPostDetail(999L))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(PostErrorCode.POST_NOT_FOUND)
        );
  }

  private CreatePostCommand createDefaultCommand(Long countryId, Long cityId) {
    return createDefaultCommand(countryId, cityId, List.of(1L));
  }

  private CreatePostCommand createDefaultCommand(Long countryId, Long cityId, List<Long> tagIds) {
    return createDefaultCommand(
        countryId,
        cityId,
        LocalDate.now().plusDays(10),
        LocalDate.now().plusDays(20),
        tagIds
    );
  }

  private CreatePostCommand createDefaultCommand(
      Long countryId,
      Long cityId,
      LocalDate startDate,
      LocalDate endDate
  ) {
    return createDefaultCommand(countryId, cityId, startDate, endDate, List.of(1L));
  }

  private CreatePostCommand createDefaultCommand(
      Long countryId,
      Long cityId,
      LocalDate startDate,
      LocalDate endDate,
      List<Long> tagIds
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
        tagIds,
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

  private User createUser(
      String email,
      String providerId,
      String nickname,
      Long exchangeCountryId,
      LocalDate birthDate,
      Gender gender
  ) {
    return User.builder()
        .email(email)
        .provider(AuthProvider.KAKAO)
        .providerId(providerId)
        .nickname(nickname)
        .exchangeCountry(countryRepository.findById(exchangeCountryId).orElseThrow())
        .birthDate(birthDate)
        .gender(gender)
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
    jdbcTemplate.update("DELETE FROM city");
    jdbcTemplate.update("DELETE FROM country");
  }
}
