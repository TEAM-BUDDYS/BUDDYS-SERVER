package org.sopt.buddys.domain.user.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.entity.VerificationBadge;
import org.sopt.buddys.domain.user.service.result.UserProfileResult;

class UserProfileResponseTest {

  @DisplayName("파견교 인증이 되어 있으면 학교 인증 여부와 관계없이 파견교 인증 뱃지를 반환한다")
  @Test
  void from_exchangeVerified_returnsExchangeVerifiedBadge() {
    // given
    User user = createUser(true, true);
    UserProfileResult result = new UserProfileResult(user, List.of(), List.of());

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
    UserProfileResult result = new UserProfileResult(user, List.of(), List.of());

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
    UserProfileResult result = new UserProfileResult(user, List.of(), List.of());

    // when
    UserProfileResponse response = UserProfileResponse.from(result);

    // then
    assertThat(response.verificationBadge()).isEqualTo(VerificationBadge.SOCIAL_LOGIN);
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
