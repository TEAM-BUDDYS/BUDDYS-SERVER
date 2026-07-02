package org.sopt.buddys.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

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
import org.sopt.buddys.domain.user.dto.response.UserProfileResponse;
import org.sopt.buddys.domain.user.dto.response.UserProfileResponse.TagGroupResponse;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.entity.VerificationBadge;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.user.repository.UserTagRepository;

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

  @DisplayName("파견교 인증이 되어 있으면 학교 인증 여부와 관계없이 파견교 인증 뱃지를 반환한다")
  @Test
  void getProfile_exchangeVerified_returnsExchangeVerifiedBadge() {
    // given
    Long userId = 1L;
    User user = createUser(userId, true, true);

    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
    given(userTagRepository.findTagsByUserId(userId)).willReturn(List.of());

    // when
    UserProfileResponse result = userService.getProfile(userId);

    // then
    assertThat(result.verificationBadge()).isEqualTo(VerificationBadge.EXCHANGE_VERIFIED);
  }

  @DisplayName("학교 인증만 되어 있으면 학교 인증 뱃지를 반환한다")
  @Test
  void getProfile_universityVerified_returnsUniversityVerifiedBadge() {
    // given
    Long userId = 1L;
    User user = createUser(userId, true, false);

    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
    given(userTagRepository.findTagsByUserId(userId)).willReturn(List.of());

    // when
    UserProfileResponse result = userService.getProfile(userId);

    // then
    assertThat(result.verificationBadge()).isEqualTo(VerificationBadge.UNIVERSITY_VERIFIED);
  }

  @DisplayName("추가 인증이 없으면 소셜 로그인 뱃지를 반환한다")
  @Test
  void getProfile_notVerified_returnsSocialLoginBadge() {
    // given
    Long userId = 1L;
    User user = createUser(userId, false, false);

    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
    given(userTagRepository.findTagsByUserId(userId)).willReturn(List.of());

    // when
    UserProfileResponse result = userService.getProfile(userId);

    // then
    assertThat(result.verificationBadge()).isEqualTo(VerificationBadge.SOCIAL_LOGIN);
  }

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
    UserProfileResponse result = userService.getProfile(userId);

    // then
    assertThat(result.allTags()).hasSize(3);
    assertThat(result.allTags())
        .extracting(TagGroupResponse::tagType)
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

  private List<String> getTags(UserProfileResponse response, TagType tagType) {
    return response.allTags()
        .stream()
        .filter(tagGroup -> tagGroup.tagType() == tagType)
        .findFirst()
        .orElseThrow()
        .tags();
  }

  private User createUser(Long id, boolean universityVerified, boolean exchangeVerified) {
    return User.builder()
        .id(id)
        .provider(AuthProvider.KAKAO)
        .providerId("12345")
        .email("test@kakao.com")
        .nickname("버디")
        .universityVerified(universityVerified)
        .exchangeVerified(exchangeVerified)
        .build();
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
