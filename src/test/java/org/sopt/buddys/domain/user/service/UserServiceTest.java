package org.sopt.buddys.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.repository.CourseImageRepository;
import org.sopt.buddys.domain.course.repository.CourseRepository;
import org.sopt.buddys.domain.post.repository.PostImageRepository;
import org.sopt.buddys.domain.post.repository.PostRepository;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.Gender;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.user.repository.UserTagRepository;
import org.sopt.buddys.domain.user.service.result.UserCoursesResult;
import org.sopt.buddys.domain.user.service.result.UserPostsResult;
import org.sopt.buddys.domain.user.service.result.UserProfileResult;
import org.sopt.buddys.global.exception.BaseException;
import org.sopt.buddys.domain.user.service.result.UserProfileResult.OrderedTagResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @InjectMocks
  private UserService userService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserTagRepository userTagRepository;

  @Mock
  private PostRepository postRepository;

  @Mock
  private PostImageRepository postImageRepository;

  @Mock
  private CourseRepository courseRepository;

  @Mock
  private CourseImageRepository courseImageRepository;

  @DisplayName("프로필 태그는 카테고리와 무관하게 displayOrder 순서대로 반환한다")
  @Test
  void getProfile_tagsAreSortedByDisplayOrder() {
    // given
    Long userId = 1L;
    User user = createUser(userId, false, false);
    List<UserTagRepository.UserTagProjection> userTags = List.of(
        new TestUserTagProjection(TagType.TRAVEL_STYLE, "활발한", 0),
        new TestUserTagProjection(TagType.ACTIVITY, "액티비티", 1),
        new TestUserTagProjection(TagType.TRAVEL_STYLE, "느긋한", 2),
        new TestUserTagProjection(TagType.INTEREST, "문화생활", 3),
        new TestUserTagProjection(TagType.ACTIVITY, "맛집탐방", 4),
        new TestUserTagProjection(TagType.INTEREST, "사진", 5)
    );

    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
    given(userTagRepository.findTagsByUserId(userId)).willReturn(userTags);

    // when
    UserProfileResult result = userService.getProfile(userId);

    // then
    assertThat(result.orderedTags())
        .extracting(OrderedTagResult::name, OrderedTagResult::displayOrder)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("활발한", 0),
            org.assertj.core.groups.Tuple.tuple("액티비티", 1),
            org.assertj.core.groups.Tuple.tuple("느긋한", 2),
            org.assertj.core.groups.Tuple.tuple("문화생활", 3),
            org.assertj.core.groups.Tuple.tuple("맛집탐방", 4),
            org.assertj.core.groups.Tuple.tuple("사진", 5)
        );
  }

  @DisplayName("타 유저 프로필 조회는 삭제된 사용자도 조회하고 삭제 여부를 결과에 포함한다")
  @Test
  void getPublicProfile_deletedUser_returnsDeletedUser() {
    // given
    Long userId = 1L;
    User user = createUser(userId, false, false);
    ReflectionTestUtils.setField(user, "deletedAt", LocalDateTime.of(2026, 7, 10, 12, 0));
    List<UserTagRepository.UserTagProjection> userTags = List.of(
        new TestUserTagProjection(TagType.ACTIVITY, "액티비티", 0),
        new TestUserTagProjection(TagType.INTEREST, "문화생활", 1),
        new TestUserTagProjection(TagType.TRAVEL_STYLE, "활발한", 2)
    );

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userTagRepository.findTagsByUserId(userId)).willReturn(userTags);

    // when
    UserProfileResult result = userService.getPublicProfile(userId);

    // then
    assertThat(result.user().getDeletedAt()).isNotNull();
    assertThat(result.orderedTags())
        .extracting(OrderedTagResult::name)
        .containsExactly("액티비티", "문화생활", "활발한");
  }

  @DisplayName("타 유저 게시글 조회는 삭제된 사용자가 작성한 게시글도 조회한다")
  @Test
  void getPublicPosts_deletedUser_returnsPosts() {
    // given
    Long userId = 1L;

    given(userRepository.existsById(userId)).willReturn(true);
    given(postRepository.findByAuthorIdAndDeletedAtIsNull(any(Long.class), any(Pageable.class)))
        .willReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 10), false));

    // when
    UserPostsResult result = userService.getPublicPosts(userId, 0, 10);

    // then
    assertThat(result.posts()).isEmpty();
    assertThat(result.page()).isZero();
    assertThat(result.size()).isEqualTo(10);
    assertThat(result.hasNext()).isFalse();
  }

  @DisplayName("내가 작성한 코스 목록을 Slice로 조회한다")
  @Test
  void getCourses_returnsCourses() {
    // given
    Long userId = 1L;
    Course course = createCourse(userId, 10L, "https://example.com/thumbnail.jpg");
    PageRequest pageable = PageRequest.of(0, 12);

    given(userRepository.existsByIdAndDeletedAtIsNull(userId)).willReturn(true);
    given(courseRepository.findByAuthorIdAndDeletedAtIsNull(any(Long.class), any(Pageable.class)))
        .willReturn(new SliceImpl<>(List.of(course), pageable, true));
    given(courseImageRepository.findThumbnailImageUrlsByCourseIds(List.of(course.getId())))
        .willReturn(List.of(
            new TestCourseImageUrlProjection(course.getId(), "https://example.com/day1-first.jpg")
        ));

    // when
    UserCoursesResult result = userService.getCourses(userId, 0, 12);

    // then
    assertThat(result.courses()).hasSize(1);
    assertThat(result.courses().get(0).courseId()).isEqualTo(course.getId());
    assertThat(result.courses().get(0).thumbnailImageUrl())
        .isEqualTo("https://example.com/day1-first.jpg");
    assertThat(result.page()).isZero();
    assertThat(result.size()).isEqualTo(12);
    assertThat(result.hasNext()).isTrue();
    then(courseRepository).should().findByAuthorIdAndDeletedAtIsNull(
        userId,
        PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
    );
  }

  @DisplayName("타 유저 코스 조회는 삭제된 사용자가 작성한 코스도 조회한다")
  @Test
  void getPublicCourses_deletedUser_returnsCourses() {
    // given
    Long userId = 1L;
    Course course = createCourse(userId, 10L, null);
    PageRequest pageable = PageRequest.of(0, 12);

    given(userRepository.existsById(userId)).willReturn(true);
    given(courseRepository.findByAuthorIdAndDeletedAtIsNull(any(Long.class), any(Pageable.class)))
        .willReturn(new SliceImpl<>(List.of(course), pageable, false));
    given(courseImageRepository.findThumbnailImageUrlsByCourseIds(List.of(course.getId())))
        .willReturn(List.of(
            new TestCourseImageUrlProjection(course.getId(), "https://example.com/day1-first.jpg")
        ));

    // when
    UserCoursesResult result = userService.getPublicCourses(userId, 0, 12);

    // then
    assertThat(result.courses()).hasSize(1);
    assertThat(result.courses().get(0).courseId()).isEqualTo(course.getId());
    assertThat(result.courses().get(0).thumbnailImageUrl())
        .isEqualTo("https://example.com/day1-first.jpg");
    assertThat(result.hasNext()).isFalse();
    then(courseRepository).should().findByAuthorIdAndDeletedAtIsNull(
        userId,
        PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
    );
  }

  @DisplayName("성별이 없으면 온보딩이 완료되지 않은 것으로 판단한다")
  @Test
  void isOnboardingCompleted_genderMissing_returnsFalse() {
    // given
    User user = baseUserBuilder(1L)
        .birthDate(LocalDate.of(2000, 1, 1))
        .build();

    // when
    boolean result = userService.isOnboardingCompleted(user);

    // then
    assertThat(result).isFalse();
  }

  @DisplayName("생년월일이 없으면 온보딩이 완료되지 않은 것으로 판단한다")
  @Test
  void isOnboardingCompleted_birthDateMissing_returnsFalse() {
    // given
    User user = baseUserBuilder(1L)
        .gender(Gender.FEMALE)
        .build();

    // when
    boolean result = userService.isOnboardingCompleted(user);

    // then
    assertThat(result).isFalse();
  }

  @DisplayName("성별과 생년월일은 있지만 태그가 3개 미만이면 온보딩이 완료되지 않은 것으로 판단한다")
  @Test
  void isOnboardingCompleted_tagCountBelowMinimum_returnsFalse() {
    // given
    Long userId = 1L;
    User user = createOnboardedProfileUser(userId);

    given(userTagRepository.countByUserId(userId)).willReturn(2L);

    // when
    boolean result = userService.isOnboardingCompleted(user);

    // then
    assertThat(result).isFalse();
  }

  @DisplayName("성별, 생년월일이 있고 태그가 3개 이상이면 온보딩이 완료된 것으로 판단한다")
  @Test
  void isOnboardingCompleted_allRequirementsMet_returnsTrue() {
    // given
    Long userId = 1L;
    User user = createOnboardedProfileUser(userId);

    given(userTagRepository.countByUserId(userId)).willReturn(3L);

    // when
    boolean result = userService.isOnboardingCompleted(user);

    // then
    assertThat(result).isTrue();
  }

  @DisplayName("알림 설정 조회는 로그인한 사용자의 알림 설정 여부를 반환한다")
  @Test
  void getNotificationSetting_returnsNotificationEnabled() {
    // given
    Long userId = 1L;
    given(userRepository.findNotificationEnabledById(userId)).willReturn(Optional.of(false));

    // when
    boolean result = userService.getNotificationSetting(userId);

    // then
    assertThat(result).isFalse();
  }

  @DisplayName("알림 설정 조회 시 사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
  @Test
  void getNotificationSetting_userNotFound_throwsException() {
    // given
    Long userId = 1L;
    given(userRepository.findNotificationEnabledById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.getNotificationSetting(userId))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(UserErrorCode.USER_NOT_FOUND);
  }

  @DisplayName("알림 설정 변경은 값을 갱신하고 갱신된 값을 반환한다")
  @Test
  void updateNotificationSetting_updatesValue() {
    // given
    Long userId = 1L;
    User user = baseUserBuilder(userId)
        .notificationEnabled(true)
        .build();

    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));

    // when
    boolean result = userService.updateNotificationSetting(userId, false);

    // then
    assertThat(result).isFalse();
    assertThat(user.isNotificationEnabled()).isFalse();
  }

  @DisplayName("알림 설정 변경 시 사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
  @Test
  void updateNotificationSetting_userNotFound_throwsException() {
    // given
    Long userId = 1L;
    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.updateNotificationSetting(userId, false))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(UserErrorCode.USER_NOT_FOUND);
  }

  private User createOnboardedProfileUser(Long userId) {
    return baseUserBuilder(userId)
        .gender(Gender.FEMALE)
        .birthDate(LocalDate.of(2000, 1, 1))
        .build();
  }

  private Course createCourse(Long authorId, Long courseId, String thumbnailImageUrl) {
    Course course = new Course(
        baseUserBuilder(authorId).build(),
        "파리 미술관 코스",
        null,
        thumbnailImageUrl,
        LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 5)
    );
    ReflectionTestUtils.setField(course, "id", courseId);
    return course;
  }

  private User createUser(Long id, boolean universityVerified, boolean exchangeVerified) {
    return baseUserBuilder(id)
        .universityVerified(universityVerified)
        .exchangeVerified(exchangeVerified)
        .build();
  }

  private User.UserBuilder baseUserBuilder(Long id) {
    return User.builder()
        .id(id)
        .provider(AuthProvider.KAKAO)
        .providerId("12345")
        .email("test@kakao.com")
        .nickname("버디");
  }

  private record TestUserTagProjection(
      TagType tagType,
      String tagName,
      int displayOrder
  ) implements UserTagRepository.UserTagProjection {

    @Override
    public TagType getTagType() {
      return tagType;
    }

    @Override
    public String getTagName() {
      return tagName;
    }

    @Override
    public Long getTagId() {
      return (long) displayOrder + 1;
    }

    @Override
    public int getDisplayOrder() {
      return displayOrder;
    }
  }

  private record TestCourseImageUrlProjection(
      Long courseId,
      String imageUrl
  ) implements CourseImageRepository.CourseImageUrlProjection {

    @Override
    public Long getCourseId() {
      return courseId;
    }

    @Override
    public String getImageUrl() {
      return imageUrl;
    }
  }
}
