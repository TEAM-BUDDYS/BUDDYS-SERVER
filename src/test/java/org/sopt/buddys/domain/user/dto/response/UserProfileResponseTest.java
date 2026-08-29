package org.sopt.buddys.domain.user.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.entity.VerificationBadge;
import org.sopt.buddys.domain.user.service.result.UserProfileResult;
import org.sopt.buddys.domain.user.service.result.UserProfileResult.OrderedTagResult;

class UserProfileResponseTest {

  @DisplayName("파견교 인증이 되어 있으면 학교 인증 여부와 관계없이 파견교 인증 뱃지를 반환한다")
  @Test
  void from_exchangeVerified_returnsExchangeVerifiedBadge() {
    // given
    User user = createUser(true, true);
    UserProfileResult result = new UserProfileResult(user, List.of());

    // when
    UserProfileResponse response = UserProfileResponse.from(result);

    // then
    assertThat(response.verificationBadge()).isEqualTo(VerificationBadge.EXCHANGE_VERIFIED);
  }

  @DisplayName("학교 인증만 되어 있으면 학교 인증 뱃지를 반환한다")
  @Test
  void from_universityVerified_returnsUniversityVerifiedBadge() {
    // given
    User user = createUser(true, false);
    UserProfileResult result = new UserProfileResult(user, List.of());

    // when
    UserProfileResponse response = UserProfileResponse.from(result);

    // then
    assertThat(response.verificationBadge()).isEqualTo(VerificationBadge.UNIVERSITY_VERIFIED);
  }

  @DisplayName("추가 인증이 없으면 소셜 로그인 뱃지를 반환한다")
  @Test
  void from_notVerified_returnsSocialLoginBadge() {
    // given
    User user = createUser(false, false);
    UserProfileResult result = new UserProfileResult(user, List.of());

    // when
    UserProfileResponse response = UserProfileResponse.from(result);

    // then
    assertThat(response.verificationBadge()).isEqualTo(VerificationBadge.SOCIAL_LOGIN);
  }

  @DisplayName("내 프로필은 전체 태그를 저장 순서대로 반환한다")
  @Test
  void from_returnsAllOrderedTags() {
    // given
    User user = createUser(false, false);
    UserProfileResult result = new UserProfileResult(user, List.of(
        new OrderedTagResult(27L, "계획형", TagType.TRAVEL_STYLE, 0),
        new OrderedTagResult(1L, "여행", TagType.ACTIVITY, 1),
        new OrderedTagResult(13L, "자연", TagType.INTEREST, 2),
        new OrderedTagResult(28L, "즉흥형", TagType.TRAVEL_STYLE, 3)
    ));

    // when
    UserProfileResponse response = UserProfileResponse.from(result);

    // then
    assertThat(response.orderedTags())
        .extracting(OrderedTagResponse::id)
        .containsExactly(27L, 1L, 13L, 28L);
  }

  @DisplayName("타 사용자 프로필은 저장 순서 상위 3개 태그만 반환한다")
  @Test
  void publicProfile_returnsOnlyTopThreeTags() {
    // given
    User user = createUser(false, false);
    UserProfileResult result = new UserProfileResult(user, List.of(
        new OrderedTagResult(27L, "계획형", TagType.TRAVEL_STYLE, 0),
        new OrderedTagResult(1L, "여행", TagType.ACTIVITY, 1),
        new OrderedTagResult(13L, "자연", TagType.INTEREST, 2),
        new OrderedTagResult(28L, "즉흥형", TagType.TRAVEL_STYLE, 3)
    ));

    // when
    UserPublicProfileResponse response = UserPublicProfileResponse.from(result);

    // then
    assertThat(response.representativeTags())
        .extracting(OrderedTagResponse::id)
        .containsExactly(27L, 1L, 13L);
  }

  private User createUser(boolean universityVerified, boolean exchangeVerified) {
    return User.builder()
        .id(1L)
        .provider(AuthProvider.KAKAO)
        .providerId("12345")
        .email("test@kakao.com")
        .nickname("버디")
        .universityVerified(universityVerified)
        .exchangeVerified(exchangeVerified)
        .build();
  }
}
