package org.sopt.buddys.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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
import org.sopt.buddys.domain.post.repository.PostImageRepository;
import org.sopt.buddys.domain.post.repository.PostRepository;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.Gender;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.user.repository.UserTagRepository;
import org.sopt.buddys.domain.user.service.result.UserPostsResult;
import org.sopt.buddys.domain.user.service.result.UserProfileResult;
import org.sopt.buddys.domain.user.service.result.UserProfileResult.TagGroupResult;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
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

  @DisplayName("타입별 태그 리스트를 만들고 대표 태그는 각 타입 리스트의 첫 번째 요소로 반환한다")
  @Test
  void getProfile_representativeTags_areFirstTagsOfEachType() {
    // given
    Long userId = 1L;
    User user = createUser(userId, false, false);
    List<UserTagRepository.UserTagProjection> userTags = List.of(
        new TestUserTagProjection(TagType.ACTIVITY, "액티비티"),
        new TestUserTagProjection(TagType.ACTIVITY, "맛집탐방"),
        new TestUserTagProjection(TagType.INTEREST, "문화생활"),
        new TestUserTagProjection(TagType.INTEREST, "사진"),
        new TestUserTagProjection(TagType.TRAVEL_STYLE, "활발한"),
        new TestUserTagProjection(TagType.TRAVEL_STYLE, "느긋한")
    );

    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
    given(userTagRepository.findTagsByUserId(userId)).willReturn(userTags);

    // when
    UserProfileResult result = userService.getProfile(userId);

    // then
    assertThat(result.allTags()).hasSize(3);
    assertThat(result.allTags())
        .extracting(TagGroupResult::tagType)
        .containsExactly(TagType.ACTIVITY, TagType.INTEREST, TagType.TRAVEL_STYLE);

    assertThat(getTags(result, TagType.ACTIVITY)).containsExactlyInAnyOrder("액티비티", "맛집탐방");
    assertThat(getTags(result, TagType.INTEREST)).containsExactlyInAnyOrder("문화생활", "사진");
    assertThat(getTags(result, TagType.TRAVEL_STYLE)).containsExactlyInAnyOrder("활발한", "느긋한");

    assertThat(result.representativeTags()).containsExactly(
        getTags(result, TagType.ACTIVITY).get(0),
        getTags(result, TagType.INTEREST).get(0),
        getTags(result, TagType.TRAVEL_STYLE).get(0)
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
        new TestUserTagProjection(TagType.ACTIVITY, "액티비티"),
        new TestUserTagProjection(TagType.INTEREST, "문화생활"),
        new TestUserTagProjection(TagType.TRAVEL_STYLE, "활발한")
    );

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userTagRepository.findTagsByUserId(userId)).willReturn(userTags);

    // when
    UserProfileResult result = userService.getPublicProfile(userId);

    // then
    assertThat(result.user().getDeletedAt()).isNotNull();
    assertThat(result.representativeTags()).containsExactlyInAnyOrder("액티비티", "문화생활", "활발한");
    assertThat(result.allTags()).hasSize(3);
    assertThat(getTags(result, TagType.ACTIVITY)).containsExactly("액티비티");
    assertThat(getTags(result, TagType.INTEREST)).containsExactly("문화생활");
    assertThat(getTags(result, TagType.TRAVEL_STYLE)).containsExactly("활발한");
  }

  @DisplayName("타 유저 게시글 조회는 삭제된 사용자가 작성한 게시글도 조회한다")
  @Test
  void getPublicPosts_deletedUser_returnsPosts() {
    // given
    Long userId = 1L;

    given(userRepository.existsById(userId)).willReturn(true);
    given(postRepository.findByAuthorId(any(Long.class), any(Pageable.class)))
        .willReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 10), false));

    // when
    UserPostsResult result = userService.getPublicPosts(userId, 0, 10);

    // then
    assertThat(result.posts()).isEmpty();
    assertThat(result.page()).isZero();
    assertThat(result.size()).isEqualTo(10);
    assertThat(result.hasNext()).isFalse();
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
  void getNotificationSetting_returnsUserWithNotificationEnabled() {
    // given
    Long userId = 1L;
    User user = baseUserBuilder(userId)
        .notificationEnabled(false)
        .build();

    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));

    // when
    User result = userService.getNotificationSetting(userId);

    // then
    assertThat(result.isNotificationEnabled()).isFalse();
  }

  @DisplayName("알림 설정 조회 시 사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
  @Test
  void getNotificationSetting_userNotFound_throwsException() {
    // given
    Long userId = 1L;
    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.getNotificationSetting(userId))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(UserErrorCode.USER_NOT_FOUND);
  }

  @DisplayName("알림 설정 변경은 값을 갱신하고 갱신된 사용자를 반환한다")
  @Test
  void updateNotificationSetting_updatesValue() {
    // given
    Long userId = 1L;
    User user = baseUserBuilder(userId)
        .notificationEnabled(true)
        .build();

    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));

    // when
    User result = userService.updateNotificationSetting(userId, false);

    // then
    assertThat(result.isNotificationEnabled()).isFalse();
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

  private List<String> getTags(UserProfileResult response, TagType tagType) {
    return response.allTags()
        .stream()
        .filter(tagGroup -> tagGroup.tagType() == tagType)
        .findFirst()
        .orElseThrow()
        .tags();
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
      String tagName
  ) implements UserTagRepository.UserTagProjection {

    @Override
    public TagType getTagType() {
      return tagType;
    }

    @Override
    public String getTagName() {
      return tagName;
    }
  }
}
